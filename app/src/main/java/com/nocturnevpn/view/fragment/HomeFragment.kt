package com.nocturnevpn.view.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.billingclient.api.BillingClient
import com.nocturnevpn.CheckInternetConnection
import com.nocturnevpn.R
import com.nocturnevpn.SharedPreference
import com.nocturnevpn.data.repository.SubscriptionRepository
import com.nocturnevpn.databinding.FragmentHomeBinding
import com.nocturnevpn.model.Server
import com.nocturnevpn.utils.AnimatedBorderManager
import com.nocturnevpn.utils.ConsentManager
import com.nocturnevpn.utils.SubscriptionSyncManager
import com.nocturnevpn.utils.Utils
import com.nocturnevpn.utils.toast
import com.nocturnevpn.view.activitys.ChangeServerActivity
import com.nocturnevpn.view.managers.BannerAdManager
import com.nocturnevpn.view.managers.ConnectionStatusManager
import com.nocturnevpn.view.managers.GlobeManager
import com.nocturnevpn.view.managers.NotificationManager
import com.nocturnevpn.view.managers.ServerManager
import com.nocturnevpn.view.managers.VPNManager
import de.blinkt.openvpn.core.VpnStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeFragment : Fragment(), VpnStatus.StateListener, VpnStatus.ByteCountListener {

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
    private lateinit var bannerAdManager: BannerAdManager
    
    private var connection: CheckInternetConnection? = null
    private var sharedPreference: SharedPreference? = null
    var wasConnectedOnce = false
    private var isUserPremium: Boolean = false
    
    
    private val PREFS = "reward_prefs"
    // Reward timer keys (separate from subscription keys used in SubscriptionSyncManager)
    private val KEY_REWARD_TIMER_END = "reward_timer_end"
    private val KEY_REWARD_TIMER_TYPE = "reward_timer_type"
    // Local subscription cache keys (mirror those in SubscriptionSyncManager)
    private val KEY_PRO_TIMER_END = "pro_timer_end"
    private val KEY_PRO_TIMER_TYPE = "pro_timer_type"
    private var proTimerHandler: Handler? = null
    private var proTimerRunnable: Runnable? = null
    private var rewardTimerReceiver: android.content.BroadcastReceiver? = null
    private var hasRewardAccess: Boolean = false
    
    // Animated border manager
    private lateinit var animatedBorderManager: AnimatedBorderManager
    
    // Consent manager
    private lateinit var consentManager: ConsentManager

    // Subscription repository
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var subscriptionSyncManager: SubscriptionSyncManager // Add this line


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
        val billingClient = BillingClient.newBuilder(requireContext().applicationContext)
            .enablePendingPurchases()
            .setListener { billingResult, purchases -> /* Handled by VPNManager or AfterPremiumFragment */ }
            .build()
        vpnManager = VPNManager(requireContext(), sharedPreference!!, billingClient)
        initializeManagers()
        subscriptionRepository = SubscriptionRepository(SubscriptionRepository.subscriptionService)
        subscriptionSyncManager = SubscriptionSyncManager.getInstance(requireContext()) // Initialize SubscriptionSyncManager
    }

    private fun initializeManagers() {
        notificationManager = NotificationManager(requireContext())
        animatedBorderManager = AnimatedBorderManager.getInstance(requireContext())
        consentManager = ConsentManager.getInstance(requireContext())
        bannerAdManager = BannerAdManager.getInstance(requireContext())
        
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
        // Removed: This is now handled by updateGoProButtonState for conditional navigation and animated border.
        // goProButton.setOnClickListener {
        //     android.util.Log.d("AnimatedBorderTest", "Button clicked, navigating to PremiumFragment")
        //     animatedBorderManager.setShouldShowAfterNavigation(true)
        //     findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)
        // }
        
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
        // setupProTimer() // This will now be handled by updateGoProButtonState
        updateGoProButtonState()
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
            // The navigation logic will be handled by updateGoProButtonState
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

    private val notifPermissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
        if (granted) {
            proceedConnectFlow()
        } else {
            // Open app notification settings if user denied; Samsung may suppress notifications
            openAppNotificationSettings()
            mContext.toast("Please enable notifications to start VPN")
        }
    }

    private fun notificationsAllowed(): Boolean {
        return NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
    }

    private fun openAppNotificationSettings() {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) { }
    }

    private fun proceedConnectFlow() {
        if (isUserPremium) {
            vpnManager.prepareVPN()
        } else {
            val adManager = com.nocturnevpn.view.managers.AdManager.getInstance(requireContext())
            adManager.showInterstitialAd(requireActivity()) { vpnManager.prepareVPN() }
        }
    }

    private fun handleConnectButtonClick() {
        // Add button click animation for immediate feedback
        connectionStatusManager.animateButtonClick()
        
        if (vpnManager.isVPNStarted()) {
            // For premium users, do not show interstitial on disconnect
            if (isUserPremium) {
                confirmDisconnect()
            } else {
                val adManager = com.nocturnevpn.view.managers.AdManager.getInstance(requireContext())
                adManager.showInterstitialAd(requireActivity()) {
                    // Continue with disconnect confirm after ad closed or if not ready
                    confirmDisconnect()
                }
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

        // Reward-based access is a separate flag; do not touch subscription flag
        hasRewardAccess = isRewardProTimerActive()
        Log.d("NOCTURNE_VPN_REWARD", "handleConnect: rewardAccess=$hasRewardAccess, end=${getRewardTimerEnd()}, type=${getRewardTimerType()}")
        if (hasRewardAccess) {
            // For reward timer we allow premium servers and skip subscription checks
            proceedConnectionFlowAfterPremiumCheck(true)
            return
        }

        // Otherwise, check backend-verified subscription status from Firebase
        CoroutineScope(Dispatchers.IO).launch {
            subscriptionSyncManager.restoreSubscriptionFromFirebase(
                onSuccess = { subscriptionStatus ->
                    val isUserPremiumFromFirebase = subscriptionStatus?.status == "active" &&
                                                  (subscriptionStatus.expiryTimeMillis > System.currentTimeMillis())
                    isUserPremium = isUserPremiumFromFirebase
                    Log.d("NOCTURNE_VPN_PREMIUM_CHECK", "isPremiumServer=$isPremiumServer, isUserPremiumFromFirebase=$isUserPremiumFromFirebase, BackendStatus=$subscriptionStatus")

                    if (isPremiumServer && !isUserPremiumFromFirebase) {
                        // Show custom dialog: Not allowed to connect premium server
                        Log.d("NOCTURNE_VPN_PREMIUM_CHECK", "Blocked: User tried to connect to premium server without premium access based on Firebase!")
                        requireActivity().runOnUiThread {
                            showPremiumBlockDialog()
                        }
                    } else {
                        // Proceed with VPN connection flow
                        requireActivity().runOnUiThread {
                            proceedConnectionFlowAfterPremiumCheck(isUserPremiumFromFirebase)
                        }
                    }
                },
                onFailure = { error ->
                    Log.e("NOCTURNE_VPN_PREMIUM_CHECK", "Failed to restore subscription from Firebase: $error")
                    // If Firebase check fails, default to blocking premium server access for safety
                    requireActivity().runOnUiThread {
                        if (isPremiumServer) {
                            showPremiumBlockDialog()
                        } else {
                            proceedConnectionFlowAfterPremiumCheck(false)
                        }
                    }
                }
            )
        }
    }

    private fun isRewardProTimerActive(): Boolean {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val endTime = prefs.getLong(KEY_REWARD_TIMER_END, 0L)
        val timerType = prefs.getString(KEY_REWARD_TIMER_TYPE, "") ?: ""
        // Treat missing type as reward-based for backward compatibility
        return endTime > System.currentTimeMillis()
    }

    private fun getRewardTimerEnd(): Long {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_REWARD_TIMER_END, 0L)
    }

    private fun getRewardTimerType(): String {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_REWARD_TIMER_TYPE, "") ?: ""
    }

    private fun showPremiumBlockDialog() {
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
    }

    private fun proceedConnectionFlowAfterPremiumCheck(isPremium: Boolean) {
        // On Android 13+, ensure POST_NOTIFICATIONS is granted; Samsung may suppress FGS otherwise
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted || !notificationsAllowed()) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        } else if (!notificationsAllowed()) {
            openAppNotificationSettings()
            mContext.toast("Please enable notifications to start VPN")
            return
        }

        // If we get here, either the server is not premium or the user is premium
        if (isPremium) {
            vpnManager.prepareVPN()
        } else {
            val adManager = com.nocturnevpn.view.managers.AdManager.getInstance(requireContext())
            adManager.showInterstitialAd(requireActivity()) { vpnManager.prepareVPN() }
        }
    }

    private fun setupProTimer() {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val endTime = prefs.getLong(KEY_REWARD_TIMER_END, 0L)
        val timerType = prefs.getString(KEY_REWARD_TIMER_TYPE, "") ?: ""
        hasRewardAccess = endTime > System.currentTimeMillis()
        Log.d("NOCTURNE_VPN_REWARD", "setupProTimer: rewardAccess=$hasRewardAccess, end=$endTime, type=$timerType")

        // This logic now specifically for reward-based pro timer
        if (hasRewardAccess) {
            binding?.proTimer?.visibility = View.VISIBLE
            binding?.goProButton?.visibility = View.GONE
            startProTimerCountdown(endTime, timerType)
        } else {
            binding?.proTimer?.visibility = View.GONE
            // The visibility of goProButton will be managed by updateGoProButtonState for subscription status
            // Only stop animated border if it's not running for navigation purposes
            val goProButton = view?.findViewById<com.nocturnevpn.widget.AnimatedGradientBorderView>(R.id.go_pro_button)
            if (goProButton != null && !animatedBorderManager.isAnimationRunningForNavigation()) {
                animatedBorderManager.stopAnimatedBorder()
            }
        }
        applyProTimerGradient()
    }

    private fun updateGoProButtonState() {
        CoroutineScope(Dispatchers.Main).launch {
            val goProButton = binding?.goProButton
            val proTimer = binding?.proTimer

            if (goProButton == null || proTimer == null) return@launch

            // First, check for reward-based pro timer (independent of subscription)
            val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val rewardProTimerEnd = prefs.getLong(KEY_REWARD_TIMER_END, 0L)
            val rewardTimerType = prefs.getString(KEY_REWARD_TIMER_TYPE, "") ?: ""
            hasRewardAccess = rewardProTimerEnd > System.currentTimeMillis()
            Log.d("NOCTURNE_VPN_REWARD", "updateGoProButtonState: rewardAccess=$hasRewardAccess, end=$rewardProTimerEnd, type=$rewardTimerType")

            if (hasRewardAccess) {
                proTimer.visibility = View.VISIBLE
                goProButton.visibility = View.GONE
                startProTimerCountdown(rewardProTimerEnd, rewardTimerType)
                animatedBorderManager.stopAnimatedBorder() // Ensure no animated border for reward timer
                goProButton.setOnClickListener { /* No action, timer is active */ }
                return@launch
            }

            // If no reward timer, then check for active subscription
            val purchasesResult = vpnManager.queryActiveSubscriptionPurchases()

            if (purchasesResult.isSuccess) {
                val purchases = purchasesResult.getOrNull() ?: emptyList()
                val packageName = requireContext().packageName
                var hasActiveSubscriptionFromBackend = false

                for (purchase in purchases) {
                    if (purchase.products.isNotEmpty()) {
                        val productId = purchase.products.first()
                        val purchaseToken = purchase.purchaseToken

                        // Get current user account information for subscription binding
                        val authManager = com.nocturnevpn.utils.AuthManager.getInstance(requireContext())
                        val userEmail = authManager.getCurrentUserEmail()
                        val userId = authManager.getCurrentUserId()
                        
                        Log.d("HomeFragment", "🔍 Checking subscription for user: email=$userEmail, userId=$userId")
                        val backendResult = subscriptionRepository.checkSubscription(packageName, productId, purchaseToken, userEmail, userId)
                        backendResult.onSuccess { subscriptionStatus ->
                            // Persist backend verdict snapshot so future restores use fresh data
                            val sync = com.nocturnevpn.utils.SubscriptionSyncManager.getInstance(requireContext())
                            sync.saveBackendVerifiedSubscription(subscriptionStatus, productId, purchaseToken, verifySource = "home-verify")

                            if (subscriptionStatus.status == "active" && subscriptionStatus.expiryTimeMillis > System.currentTimeMillis()) {
                                hasActiveSubscriptionFromBackend = true
                                Log.d("HomeFragment", "Active subscription verified by backend: $subscriptionStatus")
                            }
                        }.onFailure { e ->
                            Log.e("HomeFragment", "Backend verification failed for $productId: ${e.message}")
                        }
                    }
                }

                if (hasActiveSubscriptionFromBackend) {
                    goProButton.visibility = View.VISIBLE
                    proTimer.visibility = View.GONE
                    // Always-on animated border with guard against re-starting
                    if (!animatedBorderManager.isCurrentlyAnimating()) {
                        animatedBorderManager.startAnimatedBorder(goProButton, AnimatedBorderManager.ANIMATION_INFINITE)
                    }
                    goProButton.setOnClickListener {
                        findNavController().navigate(R.id.action_homeFragment_to_afterPremiumFragment2)
//                        mContext.toast("Premium feature coming soon")
                    }
                } else {
                    // Free user or expired subscription
                    goProButton.visibility = View.VISIBLE
                    proTimer.visibility = View.GONE
                    animatedBorderManager.stopAnimatedBorder()
                    goProButton.setOnClickListener {
                        findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)
//                        mContext.toast("Premium feature coming soon")
                    }
                }
            } else {
                // Error querying Google Play purchases (e.g., billing not ready)
                Log.e("HomeFragment", "Error querying Google Play purchases: ${purchasesResult.exceptionOrNull()?.message}")
                // Default to free user experience (but check local cache for quick UX)
                val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val endTime = prefs.getLong(KEY_PRO_TIMER_END, 0L)
                val type = prefs.getString(KEY_PRO_TIMER_TYPE, "") ?: ""
                val isLocalPremium = endTime > System.currentTimeMillis() && type == "subscription"
                goProButton.visibility = View.VISIBLE
                proTimer.visibility = View.GONE
                if (isLocalPremium) {
                    if (!animatedBorderManager.isCurrentlyAnimating()) {
                        animatedBorderManager.startAnimatedBorder(goProButton, AnimatedBorderManager.ANIMATION_INFINITE)
                    }
                    goProButton.setOnClickListener {
                        findNavController().navigate(R.id.action_homeFragment_to_afterPremiumFragment2)
//                        mContext.toast("Premium feature coming soon")
                    }
                } else {
                    animatedBorderManager.stopAnimatedBorder()
                    goProButton.setOnClickListener {
                        findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)
//                        mContext.toast("Premium feature coming soon")
                    }
                }
            }
        }
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
        VpnStatus.addByteCountListener(this)
        vpnManager.registerBroadcastReceiver()
        serverManager.loadSavedServer()
        com.nocturnevpn.utils.RatingDialogManager.maybeShowRatingDialog(requireActivity())
        setupProTimer()

        // Re-evaluate Go Pro button state on resume
        updateGoProButtonState()
        Log.d("NOCTURNE_VPN_REWARD", "onResume: end=${getRewardTimerEnd()}, type=${getRewardTimerType()}")

        // Ensure subscription state restored here (source of truth for Home)
        if (com.nocturnevpn.utils.AuthManager.getInstance(requireContext()).isUserSignedIn()) {
            subscriptionSyncManager.restoreSubscriptionFromFirebase(
                onSuccess = { status ->
                    Log.d("HomeFragment", "Subscription restored onResume: $status")
                },
                onFailure = { e ->
                    Log.w("HomeFragment", "Failed to restore subscription onResume: $e")
                }
            )
        }

        // Resume banner ad
        try {
            binding?.bannerAdView?.let { bannerAdManager.resumeBannerAd(it) }
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error resuming banner ad: ${e.message}")
        }

        // Restore animated border state when fragment resumes
        val goProButton = view?.findViewById<com.nocturnevpn.widget.AnimatedGradientBorderView>(R.id.go_pro_button)
        if (goProButton != null) {
            // Add a small delay to prevent multiple rapid calls
            goProButton.post {
                animatedBorderManager.onFragmentResume(goProButton)
            }
        }

        // Listen for reward timer start events from RewardFragment
        if (rewardTimerReceiver == null) {
            rewardTimerReceiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == "reward_timer_started") {
                        Log.d("NOCTURNE_VPN_REWARD", "reward_timer_started broadcast received")
                        Log.d("NOCTURNE_VPN_REWARD", "broadcast current prefs end=${getRewardTimerEnd()}, type=${getRewardTimerType()}")
                        setupProTimer()
                        updateGoProButtonState()
                    }
                }
            }
        }
        rewardTimerReceiver?.let {
            LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(it, android.content.IntentFilter("reward_timer_started"))
        }
    }

    override fun onPause() {
        super.onPause()
        VpnStatus.removeStateListener(this)
        VpnStatus.removeByteCountListener(this)
        proTimerRunnable?.let { proTimerHandler?.removeCallbacks(it) }
        
        // Pause banner ad
        try {
            binding?.bannerAdView?.let { bannerAdManager.pauseBannerAd(it) }
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error pausing banner ad: ${e.message}")
        }
        
        // Preserve animated border state when fragment is paused
        animatedBorderManager.onFragmentPause()

        // Stop listening for reward timer events
        rewardTimerReceiver?.let {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(it)
        }
    }

    override fun onStop() {
        serverManager.saveCurrentServer()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.removeVPNNotification()
        
        // Destroy banner ad
        try {
            binding?.bannerAdView?.let { bannerAdManager.destroyBannerAd(it) }
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error destroying banner ad: ${e.message}")
        }
        
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
            // Log state changes for debugging
            Log.d("VPN_START", "HomeFragment.updateState() called with state: $state, logmessage: $logmessage")
            
            connectionStatusManager.updateVPNStatusText(state ?: "", wasConnectedOnce)
            vpnManager.updateVPNState(state, wasConnectedOnce)
            // Remember last state for timer reset logic
            _lastState = state
            
            // Handle RECONNECTING and failure states - capture OpenVPN errors (avoid direct LogItem references)
            if (state == "RECONNECTING" || state == "AUTH_FAILED" || state == "CONNECTION_FAILED" || state == "TUNNEL_FAILED") {
                try {
                    val context = requireContext()
                    val lastMessage = de.blinkt.openvpn.core.VpnStatus.getLastCleanLogMessage(context)
                    if (lastMessage.isNotEmpty()) {
                        Log.e("VPN_START", "OpenVPN error during $state: $lastMessage")
                    }
                } catch (e: Exception) {
                    Log.e("VPN_START", "Error reading OpenVPN logs in HomeFragment", e)
                    e.printStackTrace()
                }
            }
            
            // Handle VPN status changes for globe updates
            when (state) {
                "CONNECTED" -> {
                    // Reset and start timer for new session
                    connectionStartMs = System.currentTimeMillis()
                    wasConnectedOnce = true
                    Log.d("HomeFragment", "VPN Connected - updating globe to show VPN server location")
                    globeManager?.onVPNStatusChanged(true)
                }
                "DISCONNECTED" -> {
                    // Ensure persistent notification is removed on disconnect from any source
                    try { notificationManager.removeVPNNotification() } catch (_: Exception) {}
                    // Reset timer on disconnect
                    connectionStartMs = 0L
                    Log.d("HomeFragment", "VPN Disconnected - updating globe to show real user location")
                    globeManager?.onVPNStatusChanged(false)
                    
                    // Force a fresh location update after VPN disconnection
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d("HomeFragment", "Forcing fresh location update after VPN disconnection")
                        globeManager?.forceRefreshLocation()
                    }, 2000)
                }
                "NOPROCESS" -> {
                    // Ensure notification is removed when no VPN process remains
                    try { notificationManager.removeVPNNotification() } catch (_: Exception) {}
                    // Reset timer when no process
                    connectionStartMs = 0L
                    Log.d("HomeFragment", "VPN No Process - updating globe to show real user location")
                    globeManager?.onVPNStatusChanged(false)
                }
                "VPN_GENERATE_CONFIG", "TCP_CONNECT", "WAIT", "AUTH", "GET_CONFIG" -> {
                    // During a fresh connection attempt, ensure timer starts from zero until CONNECTED
                    if (lastStatusWasTerminal()) {
                        connectionStartMs = 0L
                    }
                }
            }
        }
    }

    // Track whether previous status was terminal to decide timer reset during a new attempt
    private var _lastState: String? = null

    private fun lastStatusWasTerminal(): Boolean {
        val terminal = _lastState == null || _lastState == "DISCONNECTED" || _lastState == "NOPROCESS"
        return terminal
    }

    override fun setConnectedVPN(uuid: String?) {
        vpnManager.setConnectedVPN(uuid)
    }

    // Track connection start time for timer
    private var connectionStartMs: Long = 0L

    

    // Format mm:ss or hh:mm:ss
    private fun formatDuration(elapsedMs: Long): String {
        val totalSec = (elapsedMs / 1000).toInt()
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    private fun formatBpsShort(bps: Long): String {
        return when {
            bps >= 1024L * 1024L * 1024L -> String.format("%.1f GB/s", bps / (1024.0 * 1024.0 * 1024.0))
            bps >= 1024L * 1024L -> String.format("%.1f MB/s", bps / (1024.0 * 1024.0))
            bps >= 1024L -> String.format("%.1f KB/s", bps / 1024.0)
            else -> String.format("%d B/s", bps)
        }
    }

    override fun updateByteCount(`in`: Long, out: Long, diffIn: Long, diffOut: Long) {
        requireActivity().runOnUiThread {
            try {
                // Initialize start time when first bytes flow
                if (connectionStartMs == 0L) connectionStartMs = System.currentTimeMillis()
                val elapsed = System.currentTimeMillis() - connectionStartMs
                val durationStr = formatDuration(elapsed)

                // OpenVPN reports diffs for the last interval; interval is 2 seconds by default
                val intervalSec = de.blinkt.openvpn.core.OpenVPNManagement.mBytecountInterval
                val downBps = if (intervalSec > 0) diffIn / intervalSec else diffIn
                val upBps = if (intervalSec > 0) diffOut / intervalSec else diffOut

                // Update UI via manager using new direct method
                connectionStatusManager.updateConnectionSpeeds(durationStr, downBps, upBps)

                // Update persistent notification with current speeds
                try {
                    val serverCountry = sharedPreference?.getServer()?.countryLong ?: "Unknown"
                    val downText = formatBpsShort(downBps)
                    val upText = formatBpsShort(upBps)
                    notificationManager.updateVPNNotification(serverCountry, downText, upText)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error updating VPN notification", e)
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error updating byte count UI", e)
            }
        }
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
                Log.d("HomeFragment", "🚀 Initializing banner ad...")
                bannerAdManager.initializeBannerAd(
                    adView,
                    onAdLoaded = {
                        Log.d("HomeFragment", "✅ Banner ad loaded successfully")
                    },
                    onAdFailed = { error ->
                        Log.e("HomeFragment", "❌ Banner ad failed to load: $error")
                    }
                )
            } else {
                Log.w("HomeFragment", "⚠️ Banner ad view not found in layout")
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error initializing banner ad: ${e.message}")
        }
    }

}