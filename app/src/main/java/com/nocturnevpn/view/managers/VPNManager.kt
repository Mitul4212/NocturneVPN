package com.nocturnevpn.view.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.nocturnevpn.CheckInternetConnection
import com.nocturnevpn.SharedPreference
import com.nocturnevpn.utils.HistoryManager
import com.nocturnevpn.utils.toast
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VPNLaunchHelper
import de.blinkt.openvpn.core.VpnStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader

class VPNManager(
    private val context: Context,
    private val sharedPreference: SharedPreference,
    private val billingClient: BillingClient // Add BillingClient to constructor
) {
    private var vpnStart = false
    private var connection: CheckInternetConnection? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var vpnResultLauncher: ActivityResultLauncher<Intent>? = null
    private var connectionStatusManager: ConnectionStatusManager? = null
    private var notificationManager: NotificationManager? = null
    private var notificationUpdateHandler: Handler? = null
    private var notificationUpdateRunnable: Runnable? = null
    private var historyManager: HistoryManager? = null
    private val billingClientState = MutableStateFlow(false) // State flow for billing client readiness

    init {
        connection = CheckInternetConnection()
        notificationUpdateHandler = Handler(Looper.getMainLooper())
        historyManager = HistoryManager.getInstance(context)

        // Initialize billing client connection
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    billingClientState.value = true
                    Log.d("VPNManager", "Billing client setup finished, ready.")
                } else {
                    billingClientState.value = false
                    Log.w("VPNManager", "Billing client setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                billingClientState.value = false
                Log.w("VPNManager", "Billing service disconnected.")
            }
        })
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
        // Status is managed via broadcast receiver and VpnStatus listeners
        // No need to query directly - status updates come through broadcast receiver
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

            // Get .ovpn config string (already decoded from Base64)
            val ovpnConfigString = selectedServer.getOvpnConfigData()

            if (ovpnConfigString.isNullOrEmpty()) {
                context.toast("VPN configuration data is missing.")
                return
            }

            Log.d("VPN_START", "Starting VPN with server:")
            Log.d("VPN_START", "Country: ${selectedServer.getCountryLong()} (${selectedServer.getCountryShort()})")
            Log.d("VPN_START", "IP Address: ${selectedServer.getIpAddress()}")

            // Track connection start in history
            historyManager?.onConnectionStarted(selectedServer)

            // Parse .ovpn string to VpnProfile using ConfigParser
            val configParser = ConfigParser()
            val inputStreamReader = InputStreamReader(ByteArrayInputStream(ovpnConfigString.toByteArray()))
            configParser.parseConfig(inputStreamReader)
            val vpnProfile = configParser.convertProfile()

            // Configure profile
            vpnProfile.mName = selectedServer.getCountryShort()

            // Validate profile before setting username/password (like old OpenVpnApi did)
            val profileError = vpnProfile.checkProfile(context)
            if (profileError != de.blinkt.openvpn.R.string.no_error_found) {
                val errorMessage = context.getString(profileError)
                Log.e("VPN_START", "Profile validation failed: $errorMessage")
                context.toast("VPN configuration error: $errorMessage")
                historyManager?.onConnectionFailed(selectedServer, "Profile Error: $errorMessage")
                return
            }

            Log.d("VPN_START", "Profile validated successfully")
            Log.d("VPN_START", "Auth type: ${vpnProfile.mAuthenticationType}, Requires userpass: ${vpnProfile.isUserPWAuth()}")

            // Set profile creator
            vpnProfile.mProfileCreator = context.packageName
            
            // Set username/password (matching old OpenVpnApi.startVpn behavior)
            // Old API always set username/password regardless of auth type
            // VPN Gate servers might use these even if config doesn't explicitly require them
            vpnProfile.mUsername = "vpn"
            vpnProfile.mPassword = "vpn"
            Log.d("VPN_START", "Set username/password: vpn/vpn (matching old API behavior)")
            
            // Fix cipher compatibility: VPN Gate servers often use AES-128-CBC
            // which is not in OpenVPN's default cipher list. Add it to data-ciphers.
            if (vpnProfile.mDataCiphers.isNullOrEmpty()) {
                // If no data-ciphers specified, use default plus AES-128-CBC for compatibility
                vpnProfile.mDataCiphers = "AES-256-GCM:AES-128-GCM:CHACHA20-POLY1305:AES-256-CBC:AES-128-CBC"
                Log.d("VPN_START", "Set data-ciphers to include AES-128-CBC for VPN Gate compatibility")
            } else if (!vpnProfile.mDataCiphers.contains("AES-128-CBC", ignoreCase = true)) {
                // If data-ciphers exists but doesn't include AES-128-CBC, add it
                vpnProfile.mDataCiphers = "${vpnProfile.mDataCiphers}:AES-128-CBC"
                Log.d("VPN_START", "Added AES-128-CBC to existing data-ciphers: ${vpnProfile.mDataCiphers}")
            } else {
                Log.d("VPN_START", "Data-ciphers already includes AES-128-CBC: ${vpnProfile.mDataCiphers}")
            }

            // Set as temporary profile (not saved to disk)
            ProfileManager.setTemporaryProfile(context, vpnProfile)

            // Log profile details for debugging
            Log.d("VPN_START", "Profile details:")
            Log.d("VPN_START", "  UUID: ${vpnProfile.getUUIDString()}")
            Log.d("VPN_START", "  Name: ${vpnProfile.mName}")
            Log.d("VPN_START", "  Use Pull: ${vpnProfile.mUsePull}")
            // Removed mConnections.size to avoid R8 missing class error for Connection

            // Start VPN using VPNLaunchHelper
            // Parameters: profile, context, startReason (null = manual), replace_running_vpn (true)
            VPNLaunchHelper.startOpenVpn(vpnProfile, context, null, true)
            vpnStart = true
            
            Log.d("VPN_START", "VPNLaunchHelper.startOpenVpn called successfully")
        } catch (exception: IOException) {
            exception.printStackTrace()
            // Track failed connection
            val selectedServer = sharedPreference.getServer()
            if (selectedServer != null) {
                historyManager?.onConnectionFailed(selectedServer, "Configuration Error")
            }
        } catch (exception: ConfigParser.ConfigParseError) {
            exception.printStackTrace()
            // Track failed connection
            val selectedServer = sharedPreference.getServer()
            if (selectedServer != null) {
                historyManager?.onConnectionFailed(selectedServer, "Configuration Parse Error")
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            // Track failed connection
            val selectedServer = sharedPreference.getServer()
            if (selectedServer != null) {
                historyManager?.onConnectionFailed(selectedServer, "Service Error")
            }
        }
    }

    fun stopVPN(): Boolean {
        return try {
            Log.d("VPNManager", "Stopping VPN connection")
            
            // Track connection stop in history
            historyManager?.onConnectionDisconnected()
            
            // Clear connected VPN profile state
            ProfileManager.setConntectedVpnProfileDisconnected(context)
            
            // Stop VPN by sending DISCONNECT intent to OpenVPNService
            val disconnectIntent = Intent(context, OpenVPNService::class.java)
            disconnectIntent.action = OpenVPNService.DISCONNECT_VPN
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(disconnectIntent)
            } else {
                context.startService(disconnectIntent)
            }
            
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
                // Notify history manager that connection is established
                historyManager?.onConnectionEstablished()
            }
            "DISCONNECTED", "NOPROCESS" -> {
                vpnStart = false
                stopPeriodicNotificationUpdate()
                // Track connection stop in history when VPN state changes to disconnected
                historyManager?.onConnectionDisconnected()
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
        Log.d("VPN_START", "setStatus() called with state: $connectionState")
        if (connectionState != null) when (connectionState) {
            "DISCONNECTED" -> {
                vpnStart = false
                stopPeriodicNotificationUpdate()
                // Status is already updated via VpnStatus, no need to set default
                // Track connection stop in history
                historyManager?.onConnectionDisconnected()
            }
            "CONNECTED" -> {
                vpnStart = true
                startPeriodicNotificationUpdate()
                // Notify history manager that connection is established
                historyManager?.onConnectionEstablished()
            }
            "CONNECTING" -> {
                // Connection is in progress
                // History manager already knows about this from onConnectionStarted
            }
            "AUTH_FAILED", "CONNECTION_FAILED", "TUNNEL_FAILED", "RECONNECTING" -> {
                // Connection failed or reconnecting - log OpenVPN messages for debugging
                Log.e("VPN_START", "=== ENTERED RECONNECTING/FAILURE HANDLER ===")
                val selectedServer = sharedPreference.getServer()
                if (selectedServer != null) {
                    Log.e("VPN_START", "Selected server found: ${selectedServer.getCountryShort()}")
                } else {
                    Log.e("VPN_START", "WARNING: Selected server is null!")
                }
                
                if (selectedServer != null) {
                    try {
                        // Get last log message (avoiding LogItem references for R8 compatibility)
                        val lastMessage = VpnStatus.getLastCleanLogMessage(context)
                        if (lastMessage.isNotEmpty()) {
                            Log.e("VPN_START", "OpenVPN status during $connectionState: $lastMessage")
                        }
                        // Removed log buffer iteration to avoid R8 missing class error for LogItem
                    } catch (e: Exception) {
                        Log.e("VPN_START", "Error reading OpenVPN logs", e)
                        e.printStackTrace()
                    }
                    
                    if (connectionState in listOf("AUTH_FAILED", "CONNECTION_FAILED", "TUNNEL_FAILED")) {
                        historyManager?.onConnectionFailed(selectedServer, connectionState)
                    }
                }
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

    // Add a method to query active subscriptions from Google Play
    suspend fun queryActiveSubscriptionPurchases(): Result<List<Purchase>> = withContext(Dispatchers.IO) {
        if (!billingClientState.first { it }) {
            Log.w("VPNManager", "BillingClient not ready for querying purchases.")
            return@withContext Result.failure(IllegalStateException("BillingClient not ready"))
        }
        
        val purchasesResult = CompletableDeferred<Result<List<Purchase>>>()

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchasesResult.complete(Result.success(purchases))
            } else {
                Log.e("VPNManager", "Query purchases failed: ${billingResult.debugMessage}")
                purchasesResult.complete(Result.failure(Exception(billingResult.debugMessage)))
            }
        }
        purchasesResult.await()
    }

} 