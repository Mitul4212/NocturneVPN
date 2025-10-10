package com.nocturnevpn.view.fragment

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nocturnevpn.R
import com.nocturnevpn.SharedPreference
import com.nocturnevpn.databinding.FragmentSettingBinding
import com.nocturnevpn.utils.AuthManager
import com.nocturnevpn.utils.UserDataLoader
import com.nocturnevpn.view.managers.BannerAdManager
import android.util.Log
import java.util.concurrent.TimeUnit

class settingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private var sharedPreference: SharedPreference? = null
    private lateinit var authManager: AuthManager
    private lateinit var bannerAdManager: BannerAdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)// Non-null assertion operator to safely access binding

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout using View Binding
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        sharedPreference = SharedPreference(requireContext())
        authManager = AuthManager.getInstance(requireContext())
        bannerAdManager = BannerAdManager.getInstance(requireContext())
        return binding.root // Use binding.root, not binding.roots
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//         Access views using binding
//         Example: binding.textView.text = "Hello, View Binding!"
       binding.advanceSetting.setOnClickListener{
           this.findNavController().navigate(R.id.action_settingFragment3_to_advanceSetttingFragment)
       }

        binding.appIcon.setOnClickListener{
           this.findNavController().navigate(R.id.action_settingFragment3_to_appIconFragment)
       }

        binding.helpCenter.setOnClickListener{
            this.findNavController().navigate(R.id.action_settingFragment3_to_helpCenterFragment)
        }

        binding.notificton.setOnClickListener {
            openNotificationSettings(requireContext())
        }

        binding.backArrow.setOnClickListener{
            findNavController().navigateUp()
        }

        binding.reportIssue.setOnClickListener {
            val intent = Intent(requireContext(), com.nocturnevpn.view.activitys.ReportIssueActivity::class.java)
            startActivity(intent)
        }

        // Add sign out functionality
        binding.signOut.setOnClickListener {
            showSignOutConfirmationDialog()
        }

        // Update account name display
        updateAccountName()

        // Rating dialog logic
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasRated = prefs.getBoolean("has_rated", false)

        binding.rateApp.setOnClickListener {
            showRatingDialog(prefs)
        }

        // Initialize banner ad
        initializeBannerAd()

        // Show rating dialog after first VPN connect/disconnect and then once per day if not rated
        val vpnPrompted = prefs.getBoolean("vpn_prompted", false)
        val lastPromptTime = prefs.getLong("last_rating_prompt", 0L)
        val now = System.currentTimeMillis()
        val oneDayMillis = TimeUnit.DAYS.toMillis(1)

        // Simulate VPN connect/disconnect event (replace with real event in your app)
        val vpnConnected = prefs.getBoolean("vpn_connected_once", false)

        if (!hasRated && vpnConnected) {
            if (!vpnPrompted || now - lastPromptTime > oneDayMillis) {
                showRatingDialog(prefs)
                prefs.edit().putBoolean("vpn_prompted", true).putLong("last_rating_prompt", now).apply()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh account name when fragment becomes visible
        updateAccountName()
        // Show rating dialog if needed
        com.nocturnevpn.utils.RatingDialogManager.maybeShowRatingDialog(requireActivity())
        // Resume banner ad
        try {
            bannerAdManager.resumeBannerAd(binding.bannerAdView)
        } catch (e: Exception) {
            Log.e("SettingsFragment", "❌ Error resuming banner ad: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause banner ad
        try {
            bannerAdManager.pauseBannerAd(binding.bannerAdView)
        } catch (e: Exception) {
            Log.e("SettingsFragment", "❌ Error pausing banner ad: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Destroy banner ad
        try {
            bannerAdManager.destroyBannerAd(binding.bannerAdView)
        } catch (e: Exception) {
            Log.e("SettingsFragment", "❌ Error destroying banner ad: ${e.message}")
        }
        _binding = null
    }

    private fun updateAccountName() {
        UserDataLoader.loadUserName(requireContext(), binding.accountName, authManager, sharedPreference)
    }

    private fun showSignOutConfirmationDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.sign_out_dialog)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Get dialog views
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnSignOut = dialog.findViewById<Button>(R.id.btnSignOut)
        val ivClose = dialog.findViewById<LinearLayout>(R.id.ivClose)

        btnSignOut.setOnClickListener {
            performSignOut()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        ivClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun performSignOut() {
        try {
            Log.d("SettingFragment", "Starting sign out process...")
            
            // 1. Sign out from Firebase Auth (MOST IMPORTANT STEP)
            val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
            firebaseAuth.signOut()
            Log.d("SettingFragment", "Firebase Auth sign out completed")
            
            // 2. Clear user data from SharedPreference
            sharedPreference?.clearUserData()
            Log.d("SettingFragment", "SharedPreference cleared")
            
            // 3. Clear authentication state from AuthManager
            authManager.clearAuthState()
            Log.d("SettingFragment", "AuthManager state cleared")
            
            // 4. Reset AuthFlowManager to show login page on next app launch
            val authFlowManager = com.nocturnevpn.utils.AuthFlowManager.getInstance(requireContext())
            authFlowManager.resetFirstTimeLogin()
            Log.d("SettingFragment", "AuthFlowManager reset completed")
            
            // 5. Sign out from Google (if user signed in with Google)
            try {
                val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
                    requireActivity(),
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                )
                googleSignInClient.signOut()
                Log.d("SettingFragment", "Google sign out completed")
            } catch (e: Exception) {
                Log.d("SettingFragment", "Google sign out failed (user might not have signed in with Google): ${e.message}")
            }
            
            // 6. Show success message
            Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
            Log.d("SettingFragment", "Sign out process completed successfully")
            
            // 7. Navigate back to home/profile to show guest profile
            findNavController().navigate(R.id.action_settingFragment3_to_profileFragment)
            
        } catch (e: Exception) {
            Log.e("SettingFragment", "Error during sign out: ${e.message}", e)
            Toast.makeText(context, "Error signing out: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openNotificationSettings(context: android.content.Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }

    private fun showRatingDialog(prefs: SharedPreferences) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.rating_dialog)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ratingBar = dialog.findViewById<RatingBar>(R.id.ratingBar)
        val btnSubmit = dialog.findViewById<Button>(R.id.btnSubmit)
        val tvNoThanks = dialog.findViewById<Button>(R.id.tvNoThanks)
        val ivClose = dialog.findViewById<LinearLayout>(R.id.ivClose)

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating
            if (rating >= 4) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = android.net.Uri.parse("market://details?id=" + requireContext().packageName)
                startActivity(intent)
            }
            prefs.edit().putBoolean("has_rated", true).apply()
            dialog.dismiss()
        }

        tvNoThanks.setOnClickListener {
            dialog.dismiss()
        }

        ivClose?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Initialize banner ad
     */
    private fun initializeBannerAd() {
        try {
            Log.d("SettingsFragment", "🚀 Initializing banner ad...")
            bannerAdManager.initializeBannerAd(
                binding.bannerAdView,
                onAdLoaded = {
                    Log.d("SettingsFragment", "✅ Banner ad loaded successfully")
                },
                onAdFailed = { error ->
                    Log.e("SettingsFragment", "❌ Banner ad failed to load: $error")
                }
            )
        } catch (e: Exception) {
            Log.e("SettingsFragment", "❌ Error initializing banner ad: ${e.message}")
        }
    }


    companion object {

    }
}