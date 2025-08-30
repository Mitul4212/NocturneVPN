package com.nocturnevpn.utils

import android.app.Dialog
import android.content.Context
import com.nocturnevpn.R

object CustomDialogManager {
    
    fun showVpnDisconnectDialog(
        context: Context,
        onDisconnect: () -> Unit,
        onReconnect: () -> Unit,
        onCancel: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.vpn_disconnect_dialog)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Disconnect button
        val btnDisconnect = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_disconnect)
        btnDisconnect.setOnClickListener {
            onDisconnect()
            dialog.dismiss()
        }

        // Reconnect button
        val btnReconnect = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_reconnect)
        btnReconnect.setOnClickListener {
            onReconnect()
            dialog.dismiss()
        }

        // Cancel button
        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)
        btnCancel.setOnClickListener {
            onCancel()
            dialog.dismiss()
        }

        dialog.show()
    }
} 