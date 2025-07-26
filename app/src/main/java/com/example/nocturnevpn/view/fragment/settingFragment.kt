package com.example.nocturnevpn.view.fragment

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
import com.example.nocturnevpn.R
import com.example.nocturnevpn.SharedPreference
import com.example.nocturnevpn.databinding.FragmentSettingBinding
import java.util.concurrent.TimeUnit

class settingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private var sharedPreference: SharedPreference? = null

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
            val intent = Intent(requireContext(), com.example.nocturnevpn.view.activitys.ReportIssueActivity::class.java)
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

    private fun updateAccountName() {
        val userName = sharedPreference?.getUserName()
        if (userName != null && userName.isNotEmpty()) {
            binding.accountName.text = userName
        } else {
            // If no user name is set, show a default or placeholder
            binding.accountName.text = "Guest User"
        }
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
            // Clear user data using the new method
            sharedPreference?.clearUserData()
            
            // Show success message
            Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
            
            // Navigate back to home/profile to show guest profile
            findNavController().navigate(R.id.action_settingFragment3_to_profileFragment)
            
        } catch (e: Exception) {
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

    override fun onResume() {
        super.onResume()
        com.example.nocturnevpn.utils.RatingDialogManager.maybeShowRatingDialog(requireActivity())
        // Refresh account name when returning to settings
        updateAccountName()
    }

    companion object {

    }
}