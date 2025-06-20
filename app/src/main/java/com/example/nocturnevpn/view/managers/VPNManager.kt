package com.example.nocturnevpn.view.managers

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.nocturnevpn.CheckInternetConnection
import com.example.nocturnevpn.SharedPreference
import com.example.nocturnevpn.utils.toast
import de.blinkt.openvpn.OpenVpnApi
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.OpenVPNThread
import de.blinkt.openvpn.core.VpnStatus
import java.io.IOException

class VPNManager(
    private val context: Context,
    private val sharedPreference: SharedPreference
) {
    private var vpnStart = false
    private lateinit var vpnThread: OpenVPNThread
    private lateinit var vpnService: OpenVPNService
    private var connection: CheckInternetConnection? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var vpnResultLauncher: ActivityResultLauncher<Intent>? = null
    private var connectionStatusManager: ConnectionStatusManager? = null
    private var notificationManager: NotificationManager? = null
    private var notificationUpdateHandler: Handler? = null
    private var notificationUpdateRunnable: Runnable? = null

    init {
        vpnThread = OpenVPNThread()
        vpnService = OpenVPNService()
        connection = CheckInternetConnection()
        notificationUpdateHandler = Handler(Looper.getMainLooper())
    }

    fun setVPNResultLauncher(launcher: ActivityResultLauncher<Intent>) {
        vpnResultLauncher = launcher
    }

    fun setConnectionStatusManager(manager: ConnectionStatusManager) {
        connectionStatusManager = manager
    }

    fun setNotificationManager(manager: NotificationManager) {
        notificationManager = manager
    }

    fun isVPNStarted(): Boolean = vpnStart

    fun checkServiceStatus() {
        setStatus(OpenVPNService.getStatus())
    }

    fun prepareVPN() {
        if (!vpnStart) {
            if (getInternetStatus()) {
                Log.d("OpenVPN", "Preparing VPN...")
                val intent = VpnService.prepare(context)
                if (intent != null) {
                    vpnResultLauncher?.launch(intent)
                } else {
                    Log.d("OpenVPN", "VPN already prepared, starting VPN")
                    startVpn()
                }
            } else {
                context.toast("No Internet Connection")
            }
        }
    }

    fun startVpn() {
        try {
            context.toast("Starting VPN...")
            val selectedServer = sharedPreference.getServer()

            if (selectedServer == null) {
                context.toast("No server selected.")
                return
            }

            val conf = selectedServer.getOvpnConfigData()

            if (conf.isNullOrEmpty()) {
                context.toast("VPN configuration data is missing.")
                return
            }

            Log.d("VPN_START", "Starting VPN with server:")
            Log.d("VPN_START", "Country: ${selectedServer.getCountryLong()} (${selectedServer.getCountryShort()})")
            Log.d("VPN_START", "IP Address: ${selectedServer.getIpAddress()}")

            OpenVpnApi.startVpn(context, conf, selectedServer.getCountryShort(), "vpn", "vpn")
            vpnStart = true
        } catch (exception: IOException) {
            exception.printStackTrace()
        } catch (exception: RemoteException) {
            exception.printStackTrace()
        }
    }

    fun stopVPN(): Boolean {
        return try {
            Log.d("VPNManager", "Stopping VPN connection")
            OpenVPNThread.stop()
            vpnStart = false
            stopPeriodicNotificationUpdate() // Stop periodic updates
            
            // Notify managers about VPN disconnection
            connectionStatusManager?.let { manager ->
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d("VPNManager", "Notifying connection status manager about VPN disconnection")
                    // The connection status manager will handle UI updates
                }, 500)
            }
            
            notificationManager?.let { manager ->
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d("VPNManager", "Removing VPN notification after disconnection")
                    manager.removeVPNNotification()
                }, 1000)
            }
            
            true
        } catch (e: Exception) {
            Log.e("VPNManager", "Error stopping VPN", e)
            e.printStackTrace()
            false
        }
    }

    fun registerBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("VPN_Debug", "=== Broadcast Received ===")
                Log.d("VPN_Debug", "Action: ${intent.action}")
                
                try {
                    val state = intent.getStringExtra("state")
                    Log.d("VPN_Debug", "VPN State: $state")
                    setStatus(state)

                    val duration = intent.getStringExtra("duration") ?: "00:00:00"
                    val lastPacketReceive = intent.getStringExtra("lastPacketReceive") ?: "0"
                    val byteIn = intent.getStringExtra("byteIn") ?: "0"
                    val byteOut = intent.getStringExtra("byteOut") ?: "0"

                    Log.d("VPN_Debug", "Raw values received:")
                    Log.d("VPN_Debug", "Duration: $duration")
                    Log.d("VPN_Debug", "Last Packet: $lastPacketReceive")
                    Log.d("VPN_Debug", "Byte In: $byteIn")
                    Log.d("VPN_Debug", "Byte Out: $byteOut")

                    // Update notification based on state and speed data
                    when (state) {
                        "CONNECTED" -> {
                            val serverCountry = sharedPreference.getServer()?.getCountryLong() ?: "Unknown"
                            notificationManager?.updateVPNNotification(serverCountry, byteIn, byteOut)
                        }
                        "DISCONNECTED", "NOPROCESS" -> {
                            notificationManager?.removeVPNNotification()
                        }
                    }

                    // Update notification with speed data if VPN is connected and we have speed data
                    if (vpnStart && byteIn != "0" && byteOut != "0") {
                        val serverCountry = sharedPreference.getServer()?.getCountryLong() ?: "Unknown"
                        Log.d("VPN_Debug", "Updating notification with speed data - Server: $serverCountry")
                        notificationManager?.updateVPNNotification(serverCountry, byteIn, byteOut)
                    }

                    // Update connection status
                    connectionStatusManager?.updateConnectionStatus(duration, lastPacketReceive, byteIn, byteOut)
                } catch (e: Exception) {
                    Log.e("VPN_Debug", "Error in broadcast receiver", e)
                }
            }
        }

        val intentFilter = IntentFilter("connectionState")
        LocalBroadcastManager.getInstance(context).registerReceiver(
            broadcastReceiver!!, intentFilter
        )
        Log.d("VPN_Debug", "Broadcast receiver registered")
    }

    fun unregisterBroadcastReceiver() {
        try {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(
                broadcastReceiver!!
            )
            Log.d("VPN_Debug", "Broadcast receiver unregistered")
        } catch (e: Exception) {
            Log.e("VPN_Debug", "Error unregistering receiver", e)
        }
    }

    fun updateVPNState(state: String?, wasConnectedOnce: Boolean) {
        when (state) {
            "CONNECTED" -> {
                vpnStart = true
                startPeriodicNotificationUpdate()
            }
            "DISCONNECTED", "NOPROCESS" -> {
                vpnStart = false
                stopPeriodicNotificationUpdate()
            }
        }
    }

    fun setConnectedVPN(uuid: String?) {
        if (uuid != null) {
            val sharedPreferences = context.getSharedPreferences("VPNPreferences", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("connectedVPN", uuid)
            editor.apply()
        }
    }

    private fun getInternetStatus(): Boolean {
        return connection?.netCheck(context) ?: false
    }

    private fun setStatus(connectionState: String?) {
        if (connectionState != null) when (connectionState) {
            "DISCONNECTED" -> {
                vpnStart = false
                stopPeriodicNotificationUpdate()
                OpenVPNService.setDefaultStatus()
            }
            "CONNECTED" -> {
                vpnStart = true
                startPeriodicNotificationUpdate()
            }
        }
    }

    // Add method to force update notification with current speed data
    fun updateNotificationWithCurrentData() {
        if (vpnStart) {
            val serverCountry = sharedPreference.getServer()?.getCountryLong() ?: "Unknown"
            // Get current speed data from connection status manager if available
            connectionStatusManager?.let { manager ->
                // This will trigger a notification update with the latest speed data
                Log.d("VPN_Debug", "Forcing notification update with current speed data")
                // The notification will be updated on the next broadcast
            }
        }
    }

    private fun startPeriodicNotificationUpdate() {
        stopPeriodicNotificationUpdate() // Stop any existing updates
        
        notificationUpdateRunnable = object : Runnable {
            override fun run() {
                if (vpnStart) {
                    Log.d("VPN_Debug", "Periodic notification update")
                    updateNotificationWithCurrentData()
                    // Schedule next update in 2 seconds
                    notificationUpdateHandler?.postDelayed(this, 2000)
                }
            }
        }
        
        notificationUpdateHandler?.post(notificationUpdateRunnable!!)
        Log.d("VPN_Debug", "Started periodic notification updates")
    }

    private fun stopPeriodicNotificationUpdate() {
        notificationUpdateRunnable?.let { runnable ->
            notificationUpdateHandler?.removeCallbacks(runnable)
            notificationUpdateRunnable = null
            Log.d("VPN_Debug", "Stopped periodic notification updates")
        }
    }
} 