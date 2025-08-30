package com.nocturnevpn.view.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
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
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nocturnevpn.CheckInternetConnection
import com.nocturnevpn.R
import com.nocturnevpn.SharedPreference
import com.nocturnevpn.databinding.FragmentHomeBinding
import com.nocturnevpn.model.Server
import com.nocturnevpn.utils.Utils
import com.nocturnevpn.utils.toast
import com.nocturnevpn.utils.AnimatedBorderManager
import com.nocturnevpn.utils.ConsentManager
import com.nocturnevpn.view.activitys.ChangeServerActivity
import com.nocturnevpn.view.managers.ConnectionStatusManager
import com.nocturnevpn.view.managers.GlobeManager
import com.nocturnevpn.view.managers.NotificationManager
import com.nocturnevpn.view.managers.ServerManager
import com.nocturnevpn.view.managers.VPNManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.AdListener
import de.blinkt.openvpn.core.VpnStatus
import android.webkit.WebView

class HomeFragment : Fragment(), VpnStatus.StateListener {

    companion object {
        // Removed shouldShowAnimatedBorder as it's now handled by AnimatedBorderManager
    }

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


    private val PREFS = "reward_prefs"
    private val KEY_PRO_TIMER_END = "pro_timer_end"
    private val KEY_PRO_TIMER_TYPE = "pro_timer_type"
    private var proTimerHandler: Handler? = null
    private var proTimerRunnable: Runnable? = null
    
    // Animated border manager
    private lateinit var animatedBorderManager: AnimatedBorderManager
    
    // Consent manager
    private lateinit var consentManager: ConsentManager


    @SuppressLint("SuspiciousIndentation")
    private val getServerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedServer = result.data!!.getParcelableExtra<Server>("serverextra")
            if (selectedServer != null) {
                serverManager.updateServer(selectedServer)
                val adManager = com.nocturnevpn.view.managers.AdManager.getInstance(requireContext())
                adManager.showInterstitialAd(requireActivity())
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
        animatedBorderManager = AnimatedBorderManager.getInstance(requireContext())
        consentManager = ConsentManager.getInstance(requireContext())
        
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

        // Get reference to go pro button for later use
        val goProButton = view.findViewById<com.nocturnevpn.widget.AnimatedGradientBorderView>(R.id.go_pro_button)

        // On click: navigate to PremiumFragment, then after returning, show animated border for 1 minute
        goProButton.setOnClickListener {
            android.util.Log.d("AnimatedBorderTest", "Button clicked, navigating to PremiumFragment")
            animatedBorderManager.setShouldShowAfterNavigation(true)
            findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)
        }
        
        // Initialize consent popup after everything else is set up
        initializeConsentPopup()
        

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
        setupProTimer()
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
            // Set flag to show animation after navigation
            animatedBorderManager.setShouldShowAfterNavigation(true)
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


    }

    private fun handleConnectButtonClick() {
        // Add button click animation for immediate feedback
        connectionStatusManager.animateButtonClick()
        
        if (vpnManager.isVPNStarted()) {
            // Show interstitial before disconnect flow
            val adManager = com.nocturnevpn.view.managers.AdManager.getInstance(requireContext())
            adManager.showInterstitialAd(requireActivity()) {
                // Continue with disconnect confirm after ad closed or if not ready
                confirmDisconnect()
            }
            return
        }

        // Check if server is selected
        if (!serverManager.isServerSelected()) {
            getServerResult.launch(Intent(mContext, ChangeServerActivity::class.java))
            return
        }

        // --- DEBUG LOGS START ---
        val selectedServer = sharedPreference?.getServer()
        if (selectedServer == null) {
            mContext.toast("No server selected")
            Log.d("NOCTURNE_VPN_PREMIUM_CHECK", "No server selected in SharedPreference!")
            return
        }

        // Get the latest server list and update the selected server's premium status
        val latestServerList = sharedPreference?.loadServerList()
        val matchedServer = latestServerList?.find { it.ipAddress == selectedServer.ipAddress }
        val isPremiumServer = matchedServer?.isPremium ?: selectedServer.isPremium

        Log.d("NOCTURNE_VPN_PREMIUM_CHECK", "Selected server: IP=${selectedServer.ipAddress}, Country=${selectedServer.countryLong}, isPremium=$isPremiumServer (matched in list: ${matchedServer != null})")
        // --- DEBUG LOGS END ---

        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val proTimerEnd = prefs.getLong(KEY_PRO_TIMER_END, 0L)
        val isUserPremium = proTimerEnd > System.currentTimeMillis()
        Log.d("NOCTURNE_VPN_PREMIUM_CHECK", "isPremiumServer=$isPremiumServer, isUserPremium=$isUserPremium, proTimerEnd=$proTimerEnd, now=${System.currentTimeMillis()}")

        if (isPremiumServer && !isUserPremium) {
            // Show custom dialog: Not allowed to connect premium server
            Log.d("NOCTURNE_VPN_PREMIUM_CHECK", "Blocked: User tried to connect to premium server without premium access!")
            val dialogView = LayoutInflater.from(mContext).inflate(R.layout.premium_block_dialog, null)
            val titleView = dialogView.findViewById<TextView>(R.id.premium_block_title)
            val messageView = dialogView.findViewById<TextView>(R.id.premium_block_message)
            val lottie = dialogView.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.premium_block_lottie)
            val changeServerBtn = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.premium_block_change_server_btn)
            val getPremiumBtn = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.premium_block_get_premium_btn)
            // Set dialog content (optional, already set in XML)
            titleView.text = "Premium Server"
            messageView.text = "This server is for premium users only.\n\nPlease change to a normal server or get a premium subscription."
            lottie.setAnimation(R.raw.info_animation)
            val dialog = android.app.Dialog(mContext)
            dialog.setContentView(dialogView)
            dialog.setCancelable(true)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            changeServerBtn.setOnClickListener {
                dialog.dismiss()
                getServerResult.launch(Intent(mContext, ChangeServerActivity::class.java))
            }
            getPremiumBtn.setOnClickListener {
                dialog.dismiss()
                findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)
            }
            dialog.show()
            return
        }
        // --- PREMIUM CHECK LOGIC END ---

        // If we get here, either the server is not premium or the user is premium
        // Show interstitial before starting VPN for every 3rd connect attempt only (basic frequency cap)
        val adManager = com.nocturnevpn.view.managers.AdManager.getInstance(requireContext())
        adManager.showInterstitialAd(requireActivity()) {
            vpnManager.prepareVPN()
        }
    }

    private fun setupProTimer() {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val endTime = prefs.getLong(KEY_PRO_TIMER_END, 0L)
        val timerType = prefs.getString(KEY_PRO_TIMER_TYPE, "") ?: ""
        if (endTime > System.currentTimeMillis()) {
            binding?.proTimer?.visibility = View.VISIBLE
            binding?.goProButton?.visibility = View.GONE
            startProTimerCountdown(endTime, timerType)
            
            // Show animated border for premium users
            val goProButton = view?.findViewById<com.nocturnevpn.widget.AnimatedGradientBorderView>(R.id.go_pro_button)
            if (goProButton != null && !animatedBorderManager.isCurrentlyAnimating()) {
                animatedBorderManager.startAnimatedBorder(goProButton, 60000) // 1 minute duration
            }
        } else {
            binding?.proTimer?.visibility = View.GONE
            binding?.goProButton?.visibility = View.VISIBLE
            
            // Only stop animated border if it's not running for navigation purposes
            val goProButton = view?.findViewById<com.nocturnevpn.widget.AnimatedGradientBorderView>(R.id.go_pro_button)
            if (goProButton != null && !animatedBorderManager.isAnimationRunningForNavigation()) {
                animatedBorderManager.stopAnimatedBorder()
            }
        }
        applyProTimerGradient()
    }

    private fun startProTimerCountdown(endTime: Long, timerType: String) {
        proTimerRunnable?.let { proTimerHandler?.removeCallbacks(it) }
        proTimerHandler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val remaining = endTime - System.currentTimeMillis()
                if (remaining > 0) {
                    val hours = (remaining / (1000 * 60 * 60)).toInt()
                    val minutes = ((remaining / (1000 * 60)) % 60).toInt()
                    val seconds = ((remaining / 1000) % 60).toInt()
                    val timeStr = when {
                        timerType == "1d" -> String.format("%02d:%02d", hours + minutes / 60, minutes % 60)
                        hours > 0 -> String.format("%02d:%02d", hours, minutes)
                        else -> String.format("%02d:%02d", minutes, seconds)
                    }
                    binding?.proTimerText?.text = timeStr
                    binding?.proTimer?.visibility = View.VISIBLE
                    binding?.goProButton?.visibility = View.GONE
                    proTimerHandler?.postDelayed(this, 1000)
                } else {
                    binding?.proTimer?.visibility = View.GONE
                    binding?.goProButton?.visibility = View.VISIBLE
                    binding?.proTimerText?.text = "00:00"
                    
                    // Only stop animated border when pro timer expires if it's not for navigation
                    if (!animatedBorderManager.isAnimationRunningForNavigation()) {
                        animatedBorderManager.stopAnimatedBorder()
                    }
                }
            }
        }
        proTimerRunnable = runnable
        proTimerHandler?.post(runnable)
    }

    private fun applyProTimerGradient() {
        val proTimerText = binding?.proTimerText
        proTimerText?.let { textView ->
            val paint = textView.paint
            val width = paint.measureText(textView.text.toString())
            textView.paint.shader = LinearGradient(
                0f, 0f, width, textView.textSize,
                intArrayOf(Color.parseColor("#6622CC"), Color.parseColor("#22CCC2")),
                null, Shader.TileMode.REPEAT
            )
            textView.invalidate()
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

    /**
     * Get the GlobeManager instance for theme switching
     */
    fun getGlobeManager(): GlobeManager? {
        return globeManager
    }

    private fun checkVPNStatus() {
        vpnManager.checkServiceStatus()
    }



    private fun confirmDisconnect() {
        val dialog = Dialog(mContext)
        dialog.setContentView(R.layout.vpn_disconnect_confirm_dialog)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Update message with string resource
        val messageTextView = dialog.findViewById<android.widget.TextView>(R.id.vpn_disconnect_confirm_dialog_message)
        messageTextView.text = mContext.getString(R.string.connection_close_confirm)

        // Yes button
        val btnYes = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_yes)
        btnYes.setOnClickListener {
            vpnManager.stopVPN()
            dialog.dismiss()
        }

        // No button
        val btnNo = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_no)
        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        Log.d("VPN_Debug", "=== Fragment Resumed ===")
        VpnStatus.addStateListener(this)
        vpnManager.registerBroadcastReceiver()
        serverManager.loadSavedServer()
        com.nocturnevpn.utils.RatingDialogManager.maybeShowRatingDialog(requireActivity())
        setupProTimer()

        // Resume banner ad
        binding?.bannerAdView?.resume()

        // Restore animated border state when fragment resumes
        val goProButton = view?.findViewById<com.nocturnevpn.widget.AnimatedGradientBorderView>(R.id.go_pro_button)
        if (goProButton != null) {
            // Add a small delay to prevent multiple rapid calls
            goProButton.post {
                animatedBorderManager.onFragmentResume(goProButton)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        VpnStatus.removeStateListener(this)
        proTimerRunnable?.let { proTimerHandler?.removeCallbacks(it) }
        
        // Pause banner ad
        binding?.bannerAdView?.pause()
        
        // Preserve animated border state when fragment is paused
        animatedBorderManager.onFragmentPause()
    }

    override fun onStop() {
        serverManager.saveCurrentServer()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.removeVPNNotification()
        
        // Destroy banner ad
        binding?.bannerAdView?.destroy()
        
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
    
    /**
     * Initialize and show consent popup if required
     * This is called after HomeFragment is fully loaded
     */
    private fun initializeConsentPopup() {
        Log.d("HomeFragment", "🏠 === INITIALIZING CONSENT POPUP IN HOME FRAGMENT ===")
        try {
            if (!isAdded || activity == null || activity?.isFinishing == true) {
                Log.w("HomeFragment", "⚠️ Activity not valid, skipping consent popup")
                return
            }
            Log.d("HomeFragment", "📱 Activity valid, checking consent requirements...")
            consentManager.initializeConsent(requireActivity()) { consentStatus ->
                Log.d("HomeFragment", "✅ Consent initialization completed with status: $consentStatus")
                when (consentStatus) {
                    ConsentManager.ConsentStatus.PERSONALIZED -> {
                        Log.d("HomeFragment", "🎯 User chose personalized ads")
                        initializeBannerAd()
                    }
                    ConsentManager.ConsentStatus.NON_PERSONALIZED -> {
                        Log.d("HomeFragment", "🔒 User chose non-personalized ads")
                        initializeBannerAd()
                    }
                    ConsentManager.ConsentStatus.NOT_REQUIRED -> {
                        Log.d("HomeFragment", "🌍 Consent not required for this region")
                        initializeBannerAd()
                    }
                    ConsentManager.ConsentStatus.UNKNOWN -> {
                        Log.d("HomeFragment", "❓ Consent status unknown")
                        initializeBannerAd()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error initializing consent popup: ${e.message}")
        }
    }

    private fun initializeBannerAd() {
        try {
            val adView = binding?.bannerAdView
            if (adView != null) {
                Log.d("HomeFragment", "Initializing banner ad...")
                // Proceed with ad initialization immediately
                initializeBannerAdOnce()
            } else {
                Log.w("HomeFragment", "⚠️ Banner ad view not found in layout")
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error initializing banner ad: ${e.message}")
        }
    }

    private fun initializeBannerAdOnce() {
        try {
            val adView = binding?.bannerAdView
            if (adView != null) {
                Log.d("HomeFragment", "Initializing banner ad (final attempt)...")
                try {
                    WebView.setWebContentsDebuggingEnabled(true)
                } catch (e: Exception) {
                    Log.w("HomeFragment", "WebView debugging already enabled or failed: ${e.message}")
                }
                try {
                    val adRequest = AdRequest.Builder().build()
                    adView.loadAd(adRequest)
                    Log.d("HomeFragment", "Banner ad request sent successfully")
                } catch (e: Exception) {
                    Log.e("HomeFragment", "❌ Error loading banner ad: ${e.message}")
                    adView.visibility = View.GONE
                }
                adView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("HomeFragment", "✅ Banner ad loaded successfully")
                        adView.visibility = View.VISIBLE
                    }
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e("HomeFragment", "❌ Banner ad failed to load: ${loadAdError.message}")
                        adView.visibility = View.GONE
                        if (loadAdError.message?.contains("JavascriptEngine") == true) {
                            Log.w("HomeFragment", "WebView conflict detected, will retry later...")
                            Handler(Looper.getMainLooper()).postDelayed({ reloadBannerAd() }, 15000) // 15s short retry
                        } else {
                            Handler(Looper.getMainLooper()).postDelayed({ reloadBannerAd() }, 8000) // 8s retry for other errors
                        }
                    }
                    override fun onAdOpened() { Log.d("HomeFragment", "🔓 Banner ad opened") }
                    override fun onAdClosed() { Log.d("HomeFragment", "🔒 Banner ad closed") }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error in initializeBannerAdOnce: ${e.message}")
        }
    }

    private fun reloadBannerAd() {
        try {
            val adView = binding?.bannerAdView
            if (adView != null) {
                Log.d("HomeFragment", "Reloading banner ad...")
                try {
                    val adRequest = AdRequest.Builder().build()
                    adView.loadAd(adRequest)
                    Log.d("HomeFragment", "Banner ad reload request sent successfully")
                } catch (e: Exception) {
                    Log.e("HomeFragment", "❌ Error reloading banner ad: ${e.message}")
                    adView.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error reloading banner ad: ${e.message}")
        }
    }
}