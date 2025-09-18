package com.callblocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import android.provider.ContactsContract

class CallBlockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val className = event.className?.toString() ?: return
        if (className.contains("com.whatsapp.voipcalling.InCallActivity") ||
            className.contains("com.whatsapp.voipcalling.OutgoingCallActivity")) {

            val rootNode = rootInActiveWindow ?: return
            val callerNumber = findCallerNumber(rootNode)
            if (callerNumber != null && !isInContacts(callerNumber)) {
                Log.d("CallBlocker", "Número desconhecido: $callerNumber, recusando chamada")
                refuseCall(rootNode)
            }
        }
    }

    override fun onInterrupt() {}

    private fun findCallerNumber(rootNode: AccessibilityNodeInfo): String? {
        // IDs podem mudar, valide usando uiautomator ou Accessibility Scanner
        val nodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/caller_name")
        if (nodes.isNotEmpty()) return nodes[0].text?.toString()
        return null
    }

    private val declineTexts = listOf("Recusar", "Decline")

    private fun refuseCall(rootNode: AccessibilityNodeInfo) {
        for (text in declineTexts) {
            val declineNodes = rootNode.findAccessibilityNodeInfosByText(text)
            for (node in declineNodes) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }

    private fun isInContacts(number: String): Boolean {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val cursor = contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?", arrayOf(number), null)
        val exists = cursor?.count ?: 0 > 0
        cursor?.close()
        return exists
    }
}