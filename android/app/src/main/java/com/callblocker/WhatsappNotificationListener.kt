package com.callblocker

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.Manifest
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.PhoneLookup
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class WhatsappNotificationListener : NotificationListenerService() {
    private val TAG = "WhatsappNotifListener"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") return

        val notif = sbn.notification ?: return
        val extras = notif.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()) ?: ""
        val category = notif.category

        val isCall = (category == Notification.CATEGORY_CALL)
                || title.contains("chamada", true)
                || text.contains("chamada", true)
                || title.contains("call", true)
                || text.contains("calling", true)

        if (!isCall) return

        val caller = if (title.isNotBlank()) title else text

        Log.i(TAG, "WhatsApp call detected from: $caller")

        val known = isContactKnown(caller)

        if (!known) {
            // Notifica JS (se estiver inicializado)
            try {
                val reactContext = (applicationContext as android.app.Application)
                // Use broadcast to AccessibilityService for action
                val intent = Intent("com.callblocker.ACTION_BLOCK_WHATSAPP_CALL")
                intent.putExtra("caller", caller)
                sendBroadcast(intent)

                // também enviar evento ao RN via bridge (se disponível)
                // NOTE: só funciona se ReactContext já estiver criado; ignoramos falhas
                val reactInstance = com.facebook.react.ReactApplication::class.java
                // simplificado: enviamos broadcast e o RN pode reagir via listener nativo
            } catch (e: Exception) {
                Log.w(TAG, "Erro ao notificar RN: ${e.message}")
            }
        }
    }

    private fun isContactKnown(display: String): Boolean {
        // tenta extrair dígitos (telefone)
        val phone = display.filter { it.isDigit() || it == '+' }
        // checa permissão
        val hasContactsPerm = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED)

        if (!hasContactsPerm) {
            // se não tem permissão, retornamos false (tratado como desconhecido)
            Log.w(TAG, "READ_CONTACTS não concedida")
            return false
        }

        if (phone.isNotBlank()) {
            return queryContactByPhone(phone)
        } else {
            return queryContactByName(display)
        }
    }

    private fun queryContactByPhone(phone: String): Boolean {
        try {
            val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone))
            val projection = arrayOf(PhoneLookup._ID)
            val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                return it.count > 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "queryContactByPhone erro: ${e.message}")
        }
        return false
    }

    private fun queryContactByName(name: String): Boolean {
        try {
            val uri = ContactsContract.Contacts.CONTENT_URI
            val projection = arrayOf(ContactsContract.Contacts._ID)
            val selection = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?"
            val selArgs = arrayOf("%$name%")
            val cursor: Cursor? = contentResolver.query(uri, projection, selection, selArgs, null)
            cursor?.use {
                return it.count > 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "queryContactByName erro: ${e.message}")
        }
        return false
    }
}
