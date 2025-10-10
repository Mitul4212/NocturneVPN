package com.nocturnevpn.view.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nocturnevpn.databinding.FragmentAdvanceSetttingBinding
import com.nocturnevpn.utils.ConsentManager
import com.nocturnevpn.utils.ThemeManager
import com.nocturnevpn.SharedPreference
import com.nocturnevpn.view.managers.GlobeManager
import androidx.core.text.HtmlCompat
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.graphics.Color
import android.widget.Toast

class AdvanceSetttingFragment : Fragment() {

    // View binding to interact with UI elements
    private var _binding: FragmentAdvanceSetttingBinding? = null
    private val binding get() = _binding!!

    // SharedPreferences to store Dark Mode preference
    private lateinit var sharedPreferences: SharedPreferences
    
    // Consent Manager for handling consent settings
    private lateinit var consentManager: ConsentManager
    
    // Globe Manager for handling globe theme switching
    private var globeManager: GlobeManager? = null

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
        // Initialize SharedPreference for protocol setting
        val sharedPref = SharedPreference(requireContext())
        
        // Initialize consent settings
        initializeConsentSettings()

        // Initialize protocol selection UI
        initializeProtocolSelector(sharedPref)

        // Initialize dark mode toggle
        initializeDarkModeToggle(sharedPref)

        // Apply multi-line styled texts for protocol options
        applyProtocolRichText()

        // Connect to HomeFragment's GlobeManager for theme switching
        connectToHomeFragmentGlobeManager()

        // Handle back button click to navigate back
        binding.backArrow.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    /**
     * Set the GlobeManager instance for theme switching
     */
    fun setGlobeManager(globeManager: GlobeManager) {
        this.globeManager = globeManager
        // Apply current theme to globe when manager is set
        globeManager.applyCurrentTheme()
    }

    /**
     * Connect to HomeFragment's GlobeManager
     */
    fun connectToHomeFragmentGlobeManager() {
        try {
            // Try to get the HomeFragment from the parent activity
            val activity = requireActivity()
            val navHostFragment = activity.supportFragmentManager
                .findFragmentById(com.nocturnevpn.R.id.fragmentContainerView)
            
            if (navHostFragment != null) {
                val homeFragment = navHostFragment.childFragmentManager
                    .fragments.find { it is HomeFragment } as? HomeFragment
                
                homeFragment?.let { home ->
                    val homeGlobeManager = home.getGlobeManager()
                    homeGlobeManager?.let { manager ->
                        setGlobeManager(manager)
                        Log.d("AdvanceSettings", "Successfully connected to HomeFragment's GlobeManager")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AdvanceSettings", "Error connecting to HomeFragment's GlobeManager: ${e.message}")
        }
    }







    /**
     * Initialize dark mode toggle switch
     */
    private fun initializeDarkModeToggle(sharedPref: SharedPreference) {
        // Set initial state based on saved preference
        val isDarkModeEnabled = sharedPref.isDarkModeEnabled()
        binding.darkmode.isChecked = isDarkModeEnabled
        
        // Set up listener for dark mode toggle
        binding.darkmode.setOnCheckedChangeListener { _, isChecked ->
            Log.d("AdvanceSettings", "🌙 Dark mode toggle changed to: $isChecked")
            
            // Save the preference
            sharedPref.setDarkModeEnabled(isChecked)
            
            // Apply the theme
            ThemeManager.setDarkMode(requireContext(), isChecked)
            
            // Switch globe theme
            globeManager?.switchGlobeTheme(isChecked)
            
            // Show feedback to user
            val message = if (isChecked) "Dark mode enabled" else "Light mode enabled"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            
            // Recreate activity to apply theme changes
            requireActivity().recreate()
        }
    }



    /**
     * Initialize consent settings and display current status
     */
    private fun initializeConsentSettings() {
        Log.d("AdvanceSettings", "🔧 Initializing consent settings")
        
        // Get current consent status
        val currentStatus = consentManager.getSavedConsentStatus()
        
        // Check if consent is required for this region
        val consentRequired = consentManager.isConsentRequired()
        
        if (consentRequired) {
            Log.d("AdvanceSettings", "📋 Showing consent settings for consent-required region")
            // Show consent UI for regions that require consent
            binding.consentHeading.visibility = View.VISIBLE
            binding.consentButton.visibility = View.VISIBLE
            
            // Update UI with current consent status
            updateConsentStatusUI(currentStatus)
            
            // Set up click listener for consent button
            binding.consentButton.setOnClickListener {
                Log.d("AdvanceSettings", "👆 Consent button clicked")
                showConsentManagementDialog()
            }
        } else {
            Log.d("AdvanceSettings", "📋 Hiding consent settings for non-consent region")
            // Hide consent UI for regions that don't require consent
            binding.consentHeading.visibility = View.GONE
            binding.consentButton.visibility = View.GONE
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
            ConsentManager.ConsentStatus.PERSONALIZED -> requireContext().getColor(com.nocturnevpn.R.color.strong_violet)
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
        
        // Double-check if consent is required for this region
        if (!consentManager.isConsentRequired()) {
            Log.d("AdvanceSettings", "📋 Consent not required for this region")
            val message = consentManager.getConsentRequirementMessage()
            showErrorMessage(message)
            return
        }
        
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

    private fun initializeProtocolSelector(sharedPref: SharedPreference) {
        val current = sharedPref.protocolFilter
        Log.d("AdvanceSettings", "🔧 Initializing protocol selector with: $current")
        when (current) {
            "TCP" -> binding.radioProtocolTcp.isChecked = true
            "UDP" -> binding.radioProtocolUdp.isChecked = true
            else -> binding.radioProtocolAll.isChecked = true
        }

        val onChecked: (String) -> Unit = { value ->
            sharedPref.protocolFilter = value
            Log.d("AdvanceSettings", "✅ Protocol filter changed -> $value")
        }

        binding.radioProtocolAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onChecked("ALL")
        }
        binding.radioProtocolTcp.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onChecked("TCP")
        }
        binding.radioProtocolUdp.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onChecked("UDP")
        }
    }

    private fun applyProtocolRichText() {
        val grayHex = "#B5AEBE"
        val grayColor = Color.parseColor(grayHex)
        val descScale = 0.85f

        fun setRichText(title: String, desc: String, target: android.widget.TextView) {
            val sb = SpannableStringBuilder()
            sb.append(title)
            sb.append("\n")
            val start = sb.length
            sb.append(desc)
            sb.setSpan(ForegroundColorSpan(grayColor), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(RelativeSizeSpan(descScale), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            target.text = sb
        }

        setRichText(
            title = "All",
            desc = "All Protocols: Shows TCP and UDP servers. Best compatibility.",
            target = binding.radioProtocolAll
        )
        setRichText(
            title = "TCP",
            desc = "More reliable and stable, may be slightly slower.",
            target = binding.radioProtocolTcp
        )
        setRichText(
            title = "UDP",
            desc = "Often faster and lower latency, but can be blocked by some networks.",
            target = binding.radioProtocolUdp
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks by setting binding to null
    }
}
