package com.nocturnevpn.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nocturnevpn.R
import com.nocturnevpn.adapter.SimpleHistoryAdapter
import com.nocturnevpn.databinding.FragmentProfileBinding
import com.nocturnevpn.db.HistoryHelper
import com.nocturnevpn.view.activitys.AppAuthActivity
import com.nocturnevpn.SharedPreference
import com.nocturnevpn.model.Server
import android.provider.Settings
import com.nocturnevpn.utils.getUserFriendlyDeviceId
import com.nocturnevpn.utils.AuthManager
import com.nocturnevpn.utils.UserDataLoader
import com.nocturnevpn.view.managers.BannerAdManager
import android.util.Log


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding?= null
    private val binding get() = _binding!!
    
    private lateinit var simpleHistoryAdapter: SimpleHistoryAdapter
    private lateinit var historyHelper: HistoryHelper
    private var sharedPreference: SharedPreference? = null
    private lateinit var authManager: AuthManager
    private lateinit var bannerAdManager: BannerAdManager
    

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        sharedPreference = SharedPreference(requireContext())
        authManager = AuthManager.getInstance(requireContext())
        bannerAdManager = BannerAdManager.getInstance(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSimpleHistoryRecyclerView()
        setupClickListeners()
        loadRecentHistory()
        setupFragmentResultListener()
        setupProfileVisibility()
        initializeBannerAd()

        updateSelectedServerIp()
        updateDeviceUniqueId()
        loadUserName()
        loadUserEmail()
        setupGuestProfileClickListeners()

        // Update premium status initially (Profile no longer triggers restore; Home/Premium own it)
        updatePremiumStatus()
    }
    
    /**
     * Initialize banner ad
     */
    private fun initializeBannerAd() {
        try {
            Log.d("ProfileFragment", "🚀 Initializing banner ad...")
            bannerAdManager.initializeBannerAd(
                binding.bannerAdView,
                onAdLoaded = {
                    Log.d("ProfileFragment", "✅ Banner ad loaded successfully")
                },
                onAdFailed = { error ->
                    Log.e("ProfileFragment", "❌ Banner ad failed to load: $error")
                }
            )
        } catch (e: Exception) {
            Log.e("ProfileFragment", "❌ Error initializing banner ad: ${e.message}")
        }
    }

    private fun loadUserName() {
        UserDataLoader.loadUserName(requireContext(), binding.userName, authManager, sharedPreference)
    }
    
    private fun loadUserEmail() {
        UserDataLoader.loadUserEmail(requireContext(), binding.userEmail, authManager)
    }

    private fun setupSimpleHistoryRecyclerView() {
        simpleHistoryAdapter = SimpleHistoryAdapter()
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = simpleHistoryAdapter
        }
    }

    private fun setupClickListeners() {
        binding.moreButton.setOnClickListener {
            // Navigate to HistoryFragment
            this.findNavController().navigate(R.id.action_profileFragment_to_historyFragment)
        }

        binding.goToPremiumButton.setOnClickListener {
            // Quick local check for immediate UX
            val prefs = requireContext().getSharedPreferences("reward_prefs", android.content.Context.MODE_PRIVATE)
            val endTime = prefs.getLong("pro_timer_end", 0L)
            val type = prefs.getString("pro_timer_type", "") ?: ""
            val isLocalPremium = endTime > System.currentTimeMillis() && (type == "subscription" || type.isNotEmpty())
            if (isLocalPremium) {
                this.findNavController().navigate(R.id.afterPremiumFragment)
                return@setOnClickListener
            }

            // Navigate directly to premium page
            Log.d("ProfileFragment", "🎯 Premium button clicked - navigating to premium page")
            this.findNavController().navigate(R.id.premiumFragment)
        }

        // Add click listener for info update button
        binding.infoUpadetButton.setOnClickListener {
            android.util.Log.d("PROFILE_DEBUG", "infoUpadetButton clicked")
            showEditUserInfoScreen()
        }

        // Add click listener for Reward Hub button
        binding.rewardHub.setOnClickListener {
            this.findNavController().navigate(R.id.action_profileFragment_to_rewardFragment)
        }

        // Add click listeners for copy buttons
        binding.deviceIdCopyBtn.setOnClickListener {
            copyDeviceIdToClipboard()
        }

        binding.ipAddressCopyBtn.setOnClickListener {
            copyIpAddressToClipboard()
        }
    }

    private fun setupFragmentResultListener() {
        setFragmentResultListener("edit_user_info_result") { _, result ->
            val updatedUserName = result.getString("updated_user_name", "")
            val updatedUserEmail = result.getString("updated_user_email", "")
            
            if (updatedUserName.isNotEmpty()) {
                // Update the UI with new user name
                binding.userName.text = updatedUserName
                
                // Save the updated user name to SharedPreference
                sharedPreference?.saveUserName(updatedUserName)
                
                // Update AuthManager's cached user name
                authManager.updateUserName(updatedUserName)
                
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditUserInfoScreen() {
        // Get current user data from UI
        val currentUserName = binding.userName.text.toString()
        val currentUserEmail = binding.userEmail.text.toString()

        // Create and show the edit user info fragment as a bottom sheet dialog
        val editUserInfoFragment = EditUserInfoFragment.newInstance(currentUserName, currentUserEmail)
        editUserInfoFragment.show(childFragmentManager, "EditUserInfoFragment")
    }

    private fun loadRecentHistory() {
        historyHelper = HistoryHelper.getInstance(requireContext())
        val recentHistory = historyHelper.getRecentHistory(3)
        
        if (recentHistory.isEmpty()) {
            binding.noHistoryText.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
            binding.moreButton.visibility = View.GONE
        } else {
            binding.noHistoryText.visibility = View.GONE
            binding.historyRecyclerView.visibility = View.VISIBLE
            binding.moreButton.visibility = View.VISIBLE
            simpleHistoryAdapter.setHistoryList(recentHistory)
        }
    }



    override fun onResume() {
        super.onResume()
        com.nocturnevpn.utils.RatingDialogManager.maybeShowRatingDialog(requireActivity())
        // Refresh history when returning from HistoryFragment
        loadRecentHistory()
        updateSelectedServerIp()
        updateDeviceUniqueId()
        // Refresh profile visibility and user data
        setupProfileVisibility()
        loadUserName()
        loadUserEmail()
        updatePremiumStatus()
        
        // Resume banner ad
        try {
            bannerAdManager.resumeBannerAd(binding.bannerAdView)
        } catch (e: Exception) {
            Log.e("ProfileFragment", "❌ Error resuming banner ad: ${e.message}")
        }
        
        // Update premium status from local cache only
        if (authManager.isUserSignedIn()) {
            updatePremiumStatus()
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause banner ad
        try {
            bannerAdManager.pauseBannerAd(binding.bannerAdView)
        } catch (e: Exception) {
            Log.e("ProfileFragment", "❌ Error pausing banner ad: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Destroy banner ad
        try {
            bannerAdManager.destroyBannerAd(binding.bannerAdView)
        } catch (e: Exception) {
            Log.e("ProfileFragment", "❌ Error destroying banner ad: ${e.message}")
        }
        _binding = null
    }

    private fun updateSelectedServerIp() {
        val server: Server? = sharedPreference?.getServer()
        val ip = server?.getIpAddress() ?: "0.0.0.0"
        binding.ipAddressProfilePage.text = ip
    }

    private fun updateDeviceUniqueId() {
        val userFriendlyId = requireContext().getUserFriendlyDeviceId()
        binding.myId.text = userFriendlyId
    }

    private fun setupProfileVisibility() {
        // Check if user is signed in using AuthManager
        val isUserSignedIn = authManager.isUserSignedIn()
        
        Log.d("PROFILE_FRAGMENT", "setupProfileVisibility() called - isUserSignedIn: $isUserSignedIn")
        
        if (isUserSignedIn) {
            // Show signed-in profile page
            Log.d("PROFILE_FRAGMENT", "User is signed in - showing profile page")
            binding.guestProfilePage.visibility = View.GONE
            binding.profilePage.visibility = View.VISIBLE
        } else {
            // Show guest profile page
            Log.d("PROFILE_FRAGMENT", "User is NOT signed in - showing guest profile")
            binding.guestProfilePage.visibility = View.VISIBLE
            binding.profilePage.visibility = View.GONE
            updateGuestDeviceUniqueId()
        }
    }

    private fun updatePremiumStatus() {
        if (!authManager.isUserSignedIn()) {
            // If user is not signed in, they are not premium
            binding.premiumStatus.text = "Free"
            binding.premiumStatus.setTextColor(requireContext().getColor(R.color.strong_violet))
            return
        }

        // Check premium status from account-aware Firebase data first, then fallback to local cache
        val subscriptionSyncManager = com.nocturnevpn.utils.SubscriptionSyncManager.getInstance(requireContext())
        subscriptionSyncManager.restoreSubscriptionFromFirebase(
            onSuccess = { subscriptionStatus ->
                val isPremiumFromFirebase = subscriptionStatus?.status == "active" && 
                                          subscriptionStatus.expiryTimeMillis > System.currentTimeMillis()
                
                val statusText = if (isPremiumFromFirebase) "Premium" else "Free"
                val color = if (isPremiumFromFirebase) R.color.success_green_premium else R.color.strong_violet
                
                try {
                    if (isAdded && context != null) {
                        binding.premiumStatus.text = statusText
                        binding.premiumStatus.setTextColor(requireContext().getColor(color))
                    }
                } catch (_: Exception) {}
                
                Log.d("ProfileFragment", "Premium status updated from Firebase: isPremium=$isPremiumFromFirebase, status=${subscriptionStatus?.status}")
            },
            onFailure = { 
                // Fallback to local cache if Firebase fails
                val prefs = requireContext().getSharedPreferences("reward_prefs", android.content.Context.MODE_PRIVATE)
                val proTimerEnd = prefs.getLong("pro_timer_end", 0L)
                val proTimerType = prefs.getString("pro_timer_type", "") ?: ""
                
                val isPremium = proTimerType == "subscription" && proTimerEnd > System.currentTimeMillis()
                val statusText = if (isPremium) "Premium" else "Free"
                val color = if (isPremium) R.color.success_green_premium else R.color.strong_violet
                
                try {
                    if (isAdded && context != null) {
                        binding.premiumStatus.text = statusText
                        binding.premiumStatus.setTextColor(requireContext().getColor(color))
                    }
                } catch (_: Exception) {}
                
                Log.d("ProfileFragment", "Premium status updated from local cache (Firebase failed): isPremium=$isPremium")
            }
        )
    }

    private fun setupGuestProfileClickListeners() {
        // Setup click listeners for guest profile buttons directly in the main layout
        
        // Setup sign in button
        binding.signInButton.setOnClickListener {
            val intent = Intent(requireContext(), AppAuthActivity::class.java)
            startActivity(intent)
        }

        // Setup upgrade button
        binding.upgradeButton.setOnClickListener {
            this.findNavController().navigate(R.id.action_profileFragment_to_premiumFragment)
        }

        // Setup copy button
        binding.copyButton.setOnClickListener {
            copyUserIdToClipboard()
        }
    }

    private fun updateGuestDeviceUniqueId() {
        val userFriendlyId = requireContext().getUserFriendlyDeviceId()
        binding.userId.text = userFriendlyId
    }

    private fun copyUserIdToClipboard() {
        val userId = binding.userId.text.toString()
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("User ID", userId)
        clipboard.setPrimaryClip(clip)
        
        // Show toast message
        Toast.makeText(context, "User ID copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    private fun copyDeviceIdToClipboard() {
        val deviceId = binding.myId.text.toString()
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Device ID", deviceId)
        clipboard.setPrimaryClip(clip)
        
        // Show toast message
        Toast.makeText(context, "Device ID copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    private fun copyIpAddressToClipboard() {
        val ipAddress = binding.ipAddressProfilePage.text.toString()
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("IP Address", ipAddress)
        clipboard.setPrimaryClip(clip)
        
        // Show toast message
        Toast.makeText(context, "IP Address copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    
    
    

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 