package com.callblocker

import android.content.Intent
import android.provider.Settings
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import com.facebook.react.bridge.*

class WhatsappBridgeModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "WhatsappBridge"

    private fun sendEvent(name: String, params: WritableMap?) {
        try {
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(name, params)
        } catch (e: Exception) {
            Log.w("WhatsappBridge", "sendEvent failed: ${e.message}")
        }
    }

    @ReactMethod
    fun requestDecline(caller: String?) {
        val intent = Intent("com.callblocker.ACTION_BLOCK_WHATSAPP_CALL")
        intent.putExtra("caller", caller)
        reactContext.sendBroadcast(intent)
    }

    @ReactMethod
    fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        reactContext.startActivity(intent)
    }

    @ReactMethod
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        reactContext.startActivity(intent)
    }

    @ReactMethod
    fun isNotificationListenerEnabled(promise: Promise) {
        try {
            val pkg = reactContext.packageName
            val flat = Settings.Secure.getString(reactContext.contentResolver, "enabled_notification_listeners") ?: ""
            promise.resolve(flat.contains(pkg))
        } catch (e: Exception) {
            promise.reject("ERR", e.message)
        }
    }

    @ReactMethod
    fun isAccessibilityServiceEnabled(promise: Promise) {
        try {
            val enabled = Settings.Secure.getString(reactContext.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            // tentativa simples: procurar pelo package name
            val pkg = reactContext.packageName
            promise.resolve(enabled.contains(pkg))
        } catch (e: Exception) {
            promise.reject("ERR", e.message)
        }
    }
}
