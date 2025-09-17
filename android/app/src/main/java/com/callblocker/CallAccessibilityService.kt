package com.callblocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class CallAccessibilityService : AccessibilityService() {
    private val TAG = "CallAccessibilitySvc"

    private val blockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.callblocker.ACTION_BLOCK_WHATSAPP_CALL") {
                val caller = intent.getStringExtra("caller")
                Log.i(TAG, "Recebeu pedido para bloquear chamada de $caller")
                declineWhatsappCall()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter("com.callblocker.ACTION_BLOCK_WHATSAPP_CALL")
        registerReceiver(blockReceiver, filter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // opcional: podemos reagir a eventos se quisermos
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(blockReceiver)
        } catch (_: Exception) {}
    }

    private fun declineWhatsappCall() {
        val root = rootInActiveWindow ?: run {
            Log.w(TAG, "rootInActiveWindow null")
            return
        }

        val candidates = listOf("Recusar", "Decline", "Reject", "Rejeitar")
        for (txt in candidates) {
            val nodes = root.findAccessibilityNodeInfosByText(txt)
            if (!nodes.isNullOrEmpty()) {
                for (node in nodes) {
                    if (node.isClickable) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.i(TAG, "Clicou em botão '$txt'")
                        return
                    } else {
                        var parent = node.parent
                        while (parent != null) {
                            if (parent.isClickable) {
                                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                Log.i(TAG, "Clicou no parent de '$txt'")
                                return
                            }
                            parent = parent.parent
                        }
                    }
                }
            }
        }

        // fallback: tentar encontrar por viewId (pode mudar com versões do WhatsApp)
        try {
            val idCandidates = listOf(
                "com.whatsapp:id/decline",
                "com.whatsapp:id/call_decline_button"
            )
            for (resId in idCandidates) {
                val nodesById = root.findAccessibilityNodeInfosByViewId(resId)
                if (!nodesById.isNullOrEmpty()) {
                    nodesById[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.i(TAG, "Clicou por viewId $resId")
                    return
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao procurar por viewId: ${e.message}")
        }

        // último recurso: voltar
        performGlobalAction(GLOBAL_ACTION_BACK)
        Log.i(TAG, "Fallback: GLOBAL_ACTION_BACK")
    }
}
