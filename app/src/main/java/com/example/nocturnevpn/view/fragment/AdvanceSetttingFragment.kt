package com.example.nocturnevpn.view.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.nocturnevpn.databinding.FragmentAdvanceSetttingBinding
import com.example.nocturnevpn.utils.ConsentManager

class AdvanceSetttingFragment : Fragment() {

    // View binding to interact with UI elements
    private var _binding: FragmentAdvanceSetttingBinding? = null
    private val binding get() = _binding!!

    // SharedPreferences to store Dark Mode preference
    private lateinit var sharedPreferences: SharedPreferences
    
    // Consent Manager for handling consent settings
    private lateinit var consentManager: ConsentManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using View Binding
        _binding = FragmentAdvanceSetttingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Consent Manager
        consentManager = ConsentManager.getInstance(requireContext())
        
        // Initialize consent settings
        initializeConsentSettings()

        // Handle back button click to navigate back
        binding.backArrow.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }







    // Function to enable or disable Dark Mode
//    private fun toggleDarkMode(enable: Boolean) {
//        if (enable) {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//        } else {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
//        }
//        // Save the user's preference in SharedPreferences
//        sharedPreferences.edit().putBoolean("DarkMode", enable).apply()
//    }



    /**
     * Initialize consent settings and display current status
     */
    private fun initializeConsentSettings() {
        Log.d("AdvanceSettings", "🔧 === INITIALIZING CONSENT SETTINGS ===")
        
        // Get current consent status
        val currentStatus = consentManager.getSavedConsentStatus()
        Log.d("AdvanceSettings", "📊 Current consent status: $currentStatus")
        
        // Update UI with current consent status
        updateConsentStatusUI(currentStatus)
        
        // Set up click listener for consent button
        binding.consentButton.setOnClickListener {
            Log.d("AdvanceSettings", "👆 Consent button clicked")
            showConsentManagementDialog()
        }
    }
    
    /**
     * Update UI to show current consent status
     */
    private fun updateConsentStatusUI(consentStatus: ConsentManager.ConsentStatus) {
        val statusText = when (consentStatus) {
            ConsentManager.ConsentStatus.PERSONALIZED -> "Personalized"
            ConsentManager.ConsentStatus.NON_PERSONALIZED -> "Non-Personalized"
            ConsentManager.ConsentStatus.NOT_REQUIRED -> "Not Required"
            ConsentManager.ConsentStatus.UNKNOWN -> "Not Set"
        }
        
        Log.d("AdvanceSettings", "🎨 Updating UI with status: $statusText")
        binding.currentConsentStatus.text = statusText
        
        // Update text color based on status
        val statusColor = when (consentStatus) {
            ConsentManager.ConsentStatus.PERSONALIZED -> requireContext().getColor(com.example.nocturnevpn.R.color.strong_violet)
            ConsentManager.ConsentStatus.NON_PERSONALIZED -> requireContext().getColor(android.R.color.holo_blue_dark)
            ConsentManager.ConsentStatus.NOT_REQUIRED -> requireContext().getColor(android.R.color.holo_green_dark)
            ConsentManager.ConsentStatus.UNKNOWN -> requireContext().getColor(android.R.color.darker_gray)
        }
        binding.currentConsentStatus.setTextColor(statusColor)
    }
    
    /**
     * Show consent management dialog
     */
    private fun showConsentManagementDialog() {
        Log.d("AdvanceSettings", "📋 === SHOWING CONSENT MANAGEMENT DIALOG ===")
        
        try {
            consentManager.showPrivacyOptionsForm(requireActivity()) { newConsentStatus ->
                Log.d("AdvanceSettings", "✅ User updated consent to: $newConsentStatus")
                
                // Update UI with new consent status
                updateConsentStatusUI(newConsentStatus)
                
                // Show success message
                showConsentUpdateMessage(newConsentStatus)
            }
        } catch (e: Exception) {
            Log.e("AdvanceSettings", "❌ Error showing consent dialog: ${e.message}")
            // Show error message to user
            showErrorMessage("Failed to open consent settings")
        }
    }
    
    /**
     * Show success message after consent update
     */
    private fun showConsentUpdateMessage(consentStatus: ConsentManager.ConsentStatus) {
        val message = when (consentStatus) {
            ConsentManager.ConsentStatus.PERSONALIZED -> "Consent updated to Personalized Ads"
            ConsentManager.ConsentStatus.NON_PERSONALIZED -> "Consent updated to Non-Personalized Ads"
            ConsentManager.ConsentStatus.NOT_REQUIRED -> "Consent not required for your region"
            ConsentManager.ConsentStatus.UNKNOWN -> "Consent status updated"
        }
        
        // You can implement a toast or snackbar here
        Log.d("AdvanceSettings", "✅ $message")
        
        // For now, just log the message
        // In a real app, you might want to show a Toast or Snackbar
        // Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Show error message
     */
    private fun showErrorMessage(message: String) {
        Log.e("AdvanceSettings", "❌ Error: $message")
        // You can implement a toast or snackbar here
        // Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks by setting binding to null
    }
}
