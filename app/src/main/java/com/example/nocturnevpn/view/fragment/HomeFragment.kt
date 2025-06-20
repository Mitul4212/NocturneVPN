package com.example.nocturnevpn.view.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nocturnevpn.CheckInternetConnection
import com.example.nocturnevpn.R
import com.example.nocturnevpn.SharedPreference
import com.example.nocturnevpn.databinding.FragmentHomeBinding
import com.example.nocturnevpn.model.Server
import com.example.nocturnevpn.utils.Utils
import com.example.nocturnevpn.utils.toast
import com.example.nocturnevpn.view.activitys.ChangeServerActivity
import com.example.nocturnevpn.view.managers.ConnectionStatusManager
import com.example.nocturnevpn.view.managers.GlobeManager
import com.example.nocturnevpn.view.managers.NotificationManager
import com.example.nocturnevpn.view.managers.ServerManager
import com.example.nocturnevpn.view.managers.VPNManager
import de.blinkt.openvpn.core.VpnStatus

class HomeFragment : Fragment(), VpnStatus.StateListener {

    private lateinit var mContext: Context
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding
    
    // Managers
    private lateinit var vpnManager: VPNManager
    private lateinit var serverManager: ServerManager
    private var globeManager: GlobeManager? = null
    private lateinit var notificationManager: NotificationManager
    private lateinit var connectionStatusManager: ConnectionStatusManager
    
    private var connection: CheckInternetConnection? = null
    private var sharedPreference: SharedPreference? = null
    var wasConnectedOnce = false
    private var lastClickTime = 0L


    @SuppressLint("SuspiciousIndentation")
    private val getServerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedServer = result.data!!.getParcelableExtra<Server>("serverextra")
            if (selectedServer != null) {
                serverManager.updateServer(selectedServer)
            }
        }
    }

    private val vpnResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { vpnResult ->
            if (vpnResult.resultCode == Activity.RESULT_OK) {
                //Permission granted, start the VPN
            vpnManager.startVpn()
            } else {
                mContext.toast("For a successful VPN connection, permission must be granted.")
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        sharedPreference = SharedPreference(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connection = CheckInternetConnection()
        initializeManagers()
    }

    private fun initializeManagers() {
        vpnManager = VPNManager(requireContext(), sharedPreference!!)
        notificationManager = NotificationManager(requireContext())
        
        // Set up VPN result launcher
        vpnManager.setVPNResultLauncher(vpnResult)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize managers that need binding AFTER binding is ready
        initializeBindingDependentManagers()

        // Initialize Lottie animations
        initializeLottieAnimations()

        // Load country coordinates from JSON
        Utils.loadCountryCoordinates(requireContext())

        setupUI()
        setupClickListeners()
        setupGlobe()
        checkVPNStatus()
        
        // Initialize VPN status
        VpnStatus.initLogCache(mContext.cacheDir)
    }

    private fun initializeBindingDependentManagers() {
        // Initialize managers that need binding AFTER binding is ready
        serverManager = ServerManager(requireContext(), binding, sharedPreference!!)
        globeManager = GlobeManager(requireContext(), binding)
        connectionStatusManager = ConnectionStatusManager(requireContext(), binding, sharedPreference!!)
        
        // Connect managers
        vpnManager.setConnectionStatusManager(connectionStatusManager)
        vpnManager.setNotificationManager(notificationManager)
    }

    private fun initializeLottieAnimations() {
        // Initialize the button animation
        binding?.buttonAnimation?.let { buttonView ->
            buttonView.setAnimation(R.raw.button_base)
            buttonView.loop(true)
            buttonView.speed = 1.0f  // Normal speed for initial state
            buttonView.playAnimation()
        }
        
        // Initialize the loader animation (hidden by default)
        binding?.loaderAnimation?.let { loaderView ->
            loaderView.setAnimation(R.raw.loader)
            loaderView.loop(true)
            loaderView.speed = 2.0f  // Faster speed for loader
            loaderView.visibility = View.GONE
        }
        
        Log.d("HomeFragment", "Lottie animations initialized with optimized settings")
    }

    private fun setupUI() {
        setupProButtonGradient()
        serverManager.loadSavedServer()
    }

    private fun setupProButtonGradient() {
        val proText = view?.findViewById<TextView>(R.id.go_pro)
        proText?.let { textView ->
            val paint = textView.paint
            val width = paint.measureText(textView.text.toString())
            textView.paint.shader = LinearGradient(
                0f, 0f, width, textView.textSize,
            intArrayOf(Color.parseColor("#6622CC"), Color.parseColor("#22CCC2")),
            null, Shader.TileMode.REPEAT
        )
        }
    }

    private fun setupClickListeners() {
        binding?.goProButton?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)
        }

        binding?.chooseServer?.setOnClickListener {
            if (!vpnManager.isVPNStarted()) {
                getServerResult.launch(Intent(mContext, ChangeServerActivity::class.java))
            } else {
                mContext.toast(resources.getString(R.string.disconnect_first))
            }
        }

        binding?.connectButton?.setOnClickListener {
            handleConnectButtonClick()
        }

        // Add test button for debugging globe (you can remove this later)
        binding?.globeWebView?.setOnLongClickListener {
            Log.d("HomeFragment", "Long press on globe - testing globe functionality")
            globeManager?.testGlobe()
            mContext.toast("Testing globe with New York coordinates")
            true
        }

        // Add double tap for test globe (you can remove this later)
        binding?.globeWebView?.setOnClickListener {
            // Simple click counter for double tap detection
            if (System.currentTimeMillis() - lastClickTime < 300) {
                // Double tap detected
                Log.d("HomeFragment", "Double tap on globe - loading test globe")
                globeManager?.loadTestGlobe()
                mContext.toast("Loading test globe")
            }
            lastClickTime = System.currentTimeMillis()
        }
    }

    private fun handleConnectButtonClick() {
        // Add button click animation for immediate feedback
        connectionStatusManager.animateButtonClick()
        
        if (vpnManager.isVPNStarted()) {
            confirmDisconnect()
        } else {
            if (!vpnManager.isVPNStarted() && serverManager.isServerSelected()) {
                vpnManager.prepareVPN()
            } else if (!serverManager.isServerSelected() && !vpnManager.isVPNStarted()) {
                getServerResult.launch(Intent(mContext, ChangeServerActivity::class.java))
            } else if (vpnManager.isVPNStarted() && !serverManager.isServerSelected()) {
                mContext.toast(resources.getString(R.string.disconnect_first))
            } else {
                mContext.toast("Unable to connect the VPN")
            }
        }
    }

    private fun setupGlobe() {
        globeManager?.setupGlobe()
        
        // Force immediate location display for faster loading
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("HomeFragment", "Forcing initial location display for faster loading")
            globeManager?.forceInitialLocationDisplay()
        }, 1000) // Reduced to 1 second for faster response
        
        // Backup ensure user location is loaded after a longer delay
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("HomeFragment", "Ensuring user location is loaded after app startup")
            globeManager?.ensureUserLocationLoaded()
        }, 4000) // 4 seconds as backup
    }

    private fun checkVPNStatus() {
        vpnManager.checkServiceStatus()
    }



    private fun confirmDisconnect() {
        AlertDialog.Builder(mContext)
            .setMessage(mContext.getString(R.string.connection_close_confirm))
            .setPositiveButton(mContext.getString(R.string.yes)) { _, _ -> 
                vpnManager.stopVPN()
            }
            .setNegativeButton(mContext.getString(R.string.no)) { _, _ -> }
            .create()
            .show()
    }

    override fun onResume() {
        super.onResume()
        Log.d("VPN_Debug", "=== Fragment Resumed ===")
        VpnStatus.addStateListener(this)
        vpnManager.registerBroadcastReceiver()
        serverManager.loadSavedServer()
    }

    override fun onPause() {
        Log.d("VPN_Debug", "=== Fragment Paused ===")
        vpnManager.unregisterBroadcastReceiver()
        super.onPause()
        VpnStatus.removeStateListener(this)
    }

    override fun onStop() {
        serverManager.saveCurrentServer()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.removeVPNNotification()
        _binding = null  // 💡 Add this to avoid memory leaks
    }

    // VpnStatus.StateListener implementation
    override fun updateState(
        state: String?,
        logmessage: String?,
        localizedResId: Int,
        level: de.blinkt.openvpn.core.ConnectionStatus?,
        Intent: Intent?
    ) {
        requireActivity().runOnUiThread {
            connectionStatusManager.updateVPNStatusText(state ?: "", wasConnectedOnce)
            vpnManager.updateVPNState(state, wasConnectedOnce)
            
            // Handle VPN status changes for globe updates
            when (state) {
                "CONNECTED" -> {
                    wasConnectedOnce = true
                    Log.d("HomeFragment", "VPN Connected - updating globe to show VPN server location")
                    globeManager?.onVPNStatusChanged(true)
                }
                "DISCONNECTED" -> {
                    Log.d("HomeFragment", "VPN Disconnected - updating globe to show real user location")
                    globeManager?.onVPNStatusChanged(false)
                    
                    // Force a fresh location update after VPN disconnection
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d("HomeFragment", "Forcing fresh location update after VPN disconnection")
                        globeManager?.forceRefreshLocation()
                    }, 2000)
                }
                "NOPROCESS" -> {
                    Log.d("HomeFragment", "VPN No Process - updating globe to show real user location")
                    globeManager?.onVPNStatusChanged(false)
                }
            }
        }
    }

    override fun setConnectedVPN(uuid: String?) {
        vpnManager.setConnectedVPN(uuid)
    }
}