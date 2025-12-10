package com.nocturnevpn.view.managers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat

class NotificationManager(private val context: Context) {

    private val CHANNEL_ID = "VPN_STATUS_CHANNEL"
    private val NOTIFICATION_ID = 1
    private var notificationManager: NotificationManagerCompat? = null

    init {
        createNotificationChannel()
        notificationManager = NotificationManagerCompat.from(context)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "VPN Status"
            val descriptionText = "Shows VPN connection status and speeds"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * App-level VPN notification disabled so only the OpenVPNService
     * foreground notification is visible (single notification on status bar).
     */
    fun showVPNNotification(
        isConnected: Boolean,
        serverCountry: String,
        downloadSpeed: String,
        uploadSpeed: String
    ) {
        Log.d(
            "VPN_Debug",
            "showVPNNotification() called but app-level notification is disabled; OpenVPNService notification is the single source of truth."
        )
        // Intentionally no NotificationCompat.Builder / notify() here.
    }

    /**
     * Kept for API compatibility – no-op apart from logging.
     */
    fun updateVPNNotification(serverCountry: String, byteIn: String, byteOut: String) {
        try {
            Log.d(
                "VPN_Debug",
                "updateVPNNotification() called (In=$byteIn Out=$byteOut) but app-level notification is disabled."
            )
        } catch (e: Exception) {
            Log.e("VPN_Debug", "Error in updateVPNNotification (no-op)", e)
        }
    }

    /**
     * Still cancel by ID in case a legacy notification from older builds exists.
     */
    fun removeVPNNotification() {
        try {
            notificationManager?.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e("VPN_Debug", "Error cancelling VPN notification", e)
        }
    }
}