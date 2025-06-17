package com.example.nocturnevpn.view.managers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nocturnevpn.R
import de.blinkt.openvpn.core.OpenVPNService

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
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showVPNNotification(isConnected: Boolean, serverCountry: String, downloadSpeed: String, uploadSpeed: String) {
        val disconnectIntent = Intent(context, OpenVPNService::class.java).apply {
            action = OpenVPNService.DISCONNECT_VPN
        }
        val disconnectPendingIntent = PendingIntent.getService(
            context,
            0,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create a more readable notification content
        val contentText = if (isConnected) {
            "↓ $downloadSpeed | ↑ $uploadSpeed"
        } else {
            "Disconnected"
        }
        
        val bigText = if (isConnected) {
            "Download: $downloadSpeed Upload: $uploadSpeed"
        } else {
            "VPN connection has been disconnected"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vpn_key)
            .setContentTitle(if (isConnected) "Connected to $serverCountry" else "Disconnected")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isConnected)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_disconnect, "Disconnect", disconnectPendingIntent)
            .build()

        try {
            notificationManager?.notify(NOTIFICATION_ID, notification)
            Log.d("VPN_Debug", "Notification shown - Connected: $isConnected, Server: $serverCountry, Download: $downloadSpeed, Upload: $uploadSpeed")
        } catch (e: SecurityException) {
            Log.e("VPN_Debug", "Failed to show notification", e)
        }
    }

    fun updateVPNNotification(serverCountry: String, byteIn: String, byteOut: String) {
        try {
            Log.d("VPN_Debug", "Updating notification with - Server: $serverCountry, In: $byteIn, Out: $byteOut")
            
            // Parse the formatted byte strings
            val downInfo = SpeedCalculator.parseFormattedByteString(byteIn)
            val upInfo = SpeedCalculator.parseFormattedByteString(byteOut)
            
            Log.d("VPN_Debug", "Parsed speeds - Down: ${downInfo.speed.value} ${downInfo.speed.unit}/s, Up: ${upInfo.speed.value} ${upInfo.speed.unit}/s")
            
            // Check if parsing was successful
            val downloadText = if (downInfo.speed.value > 0) {
                "${downInfo.speed.value} ${downInfo.speed.unit}/s"
            } else {
                // Fallback: try to extract speed from original string
                extractSpeedFromString(byteIn)
            }
            
            val uploadText = if (upInfo.speed.value > 0) {
                "${upInfo.speed.value} ${upInfo.speed.unit}/s"
            } else {
                // Fallback: try to extract speed from original string
                extractSpeedFromString(byteOut)
            }
            
            Log.d("VPN_Debug", "Final speeds - Download: $downloadText, Upload: $uploadText")
            
            showVPNNotification(true, serverCountry, downloadText, uploadText)
        } catch (e: Exception) {
            Log.e("VPN_Debug", "Error updating notification", e)
            // Fallback to basic notification if parsing fails
            showVPNNotification(true, serverCountry, "0 KB/s", "0 KB/s")
        }
    }

    private fun extractSpeedFromString(input: String): String {
        return try {
            // Simple regex to find speed pattern like "1.7 kB/s" or "2.5 MB/s"
            val speedRegex = "([0-9.]+)\\s*([KMG]?B)/s".toRegex(RegexOption.IGNORE_CASE)
            val match = speedRegex.find(input)
            if (match != null) {
                val (value, unit) = match.destructured
                "${value} ${unit.uppercase()}/s"
            } else {
                "0 KB/s"
            }
        } catch (e: Exception) {
            Log.e("VPN_Debug", "Error extracting speed from string: $input", e)
            "0 KB/s"
        }
    }

    fun removeVPNNotification() {
        notificationManager?.cancel(NOTIFICATION_ID)
    }
} 