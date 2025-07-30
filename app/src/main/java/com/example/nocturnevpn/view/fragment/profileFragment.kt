package com.example.nocturnevpn.view.fragment

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
import com.example.nocturnevpn.R
import com.example.nocturnevpn.adapter.SimpleHistoryAdapter
import com.example.nocturnevpn.databinding.FragmentProfileBinding
import com.example.nocturnevpn.db.HistoryHelper
import com.example.nocturnevpn.utils.SampleDataGenerator
import com.example.nocturnevpn.view.activitys.AppAuthActivity
import com.example.nocturnevpn.SharedPreference
import com.example.nocturnevpn.model.Server
import android.provider.Settings
import com.example.nocturnevpn.utils.getUserFriendlyDeviceId
import com.example.nocturnevpn.utils.AuthManager
import android.util.Log


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding?= null
    private val binding get() = _binding!!
    
    private lateinit var simpleHistoryAdapter: SimpleHistoryAdapter
    private lateinit var historyHelper: HistoryHelper
    private var sharedPreference: SharedPreference? = null
    private lateinit var authManager: AuthManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        sharedPreference = SharedPreference(requireContext())
        authManager = AuthManager.getInstance(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSimpleHistoryRecyclerView()
        setupClickListeners()
        loadRecentHistory()
        setupFragmentResultListener()
        setupProfileVisibility()
        
        // Temporary: Add sample data generation for testing
        addSampleDataButton()
        updateSelectedServerIp()
        updateDeviceUniqueId()
        loadUserName()
        loadUserEmail()
        setupGuestProfileClickListeners()
    }

    private fun loadUserName() {
        // Check if user is signed in using AuthManager
        val isUserSignedIn = authManager.isUserSignedIn()
        
        if (isUserSignedIn) {
            // Try to get user name from AuthManager first, then fallback to SharedPreference
            val authUserName = authManager.getCurrentUserName()
            val sharedPrefUserName = sharedPreference?.getUserName()
            
            val userName = authUserName ?: sharedPrefUserName
            if (userName != null && userName.isNotEmpty()) {
                binding.userName.text = userName
            } else {
                // If no cached name, fetch from Firestore
                binding.userName.text = "Loading..." // Show loading state
                authManager.fetchUserNameFromFirestore { firestoreUserName ->
                    activity?.runOnUiThread {
                        if (firestoreUserName != null && firestoreUserName.isNotEmpty()) {
                            binding.userName.text = firestoreUserName
                        } else {
                            binding.userName.text = "Signed In User"
                        }
                    }
                }
            }
        } else {
            // User is not signed in
            binding.userName.text = "Guest User"
        }
    }
    
    private fun loadUserEmail() {
        // Check if user is signed in using AuthManager
        val isUserSignedIn = authManager.isUserSignedIn()
        
        if (isUserSignedIn) {
            // Try to get user email from AuthManager first
            val authUserEmail = authManager.getCurrentUserEmail()
            
            if (authUserEmail != null && authUserEmail.isNotEmpty()) {
                binding.userEmail.text = authUserEmail
            } else {
                // If no cached email, fetch from Firestore
                binding.userEmail.text = "Loading..." // Show loading state
                authManager.fetchUserEmailFromFirestore { firestoreUserEmail ->
                    activity?.runOnUiThread {
                        if (firestoreUserEmail != null && firestoreUserEmail.isNotEmpty()) {
                            binding.userEmail.text = firestoreUserEmail
                        } else {
                            binding.userEmail.text = "No email available"
                        }
                    }
                }
            }
        } else {
            // User is not signed in
            binding.userEmail.text = "Guest User"
        }
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
            this.findNavController().navigate(R.id.action_profileFragment_to_premiumFragment)
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
        
        // Add click listener for Device ID copy button
        binding.deviceIdCopyBtn.setOnClickListener {
            copyDeviceIdToClipboard()
        }
        
        // Add click listener for IP Address copy button
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

    // Temporary method for testing - remove this in production
    private fun addSampleDataButton() {
        binding.moreButton.setOnLongClickListener {
            SampleDataGenerator.generateSampleHistory(requireContext())
            loadRecentHistory()
            Toast.makeText(context, "Sample history data generated!", Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.nocturnevpn.utils.RatingDialogManager.maybeShowRatingDialog(requireActivity())
        // Refresh history when returning from HistoryFragment
        loadRecentHistory()
        updateSelectedServerIp()
        updateDeviceUniqueId()
        // Refresh profile visibility and user data
        setupProfileVisibility()
        loadUserName()
        loadUserEmail()
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
            binding.guestProfile.root.visibility = View.GONE
            binding.profilePageAfterSignin.visibility = View.VISIBLE
        } else {
            // Show guest profile page
            Log.d("PROFILE_FRAGMENT", "User is NOT signed in - showing guest profile")
            binding.guestProfile.root.visibility = View.VISIBLE
            binding.profilePageAfterSignin.visibility = View.GONE
            updateGuestDeviceUniqueId()
        }
    }

    private fun setupGuestProfileClickListeners() {
        // Setup click listeners for guest profile buttons
        val guestProfileView = binding.guestProfile.root
        
        // Setup sign in button
        val signInButton = guestProfileView.findViewById<android.widget.Button>(R.id.sign_in_button)
        if (signInButton != null) {
            signInButton.setOnClickListener {
                val intent = Intent(requireContext(), AppAuthActivity::class.java)
                startActivity(intent)
            }
        }

        // Setup upgrade button
        val upgradeButton = guestProfileView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.upgrade_button)
        if (upgradeButton != null) {
            upgradeButton.setOnClickListener {
                this.findNavController().navigate(R.id.action_profileFragment_to_premiumFragment)
            }
        }

        // Setup copy button
        val copyButton = guestProfileView.findViewById<android.widget.ImageView>(R.id.copy_button)
        if (copyButton != null) {
            copyButton.setOnClickListener {
                copyUserIdToClipboard()
            }
        }
    }

    private fun updateGuestDeviceUniqueId() {
        val userFriendlyId = requireContext().getUserFriendlyDeviceId()
        val guestProfileView = binding.guestProfile.root
        guestProfileView.findViewById<android.widget.TextView>(R.id.user_id).text = userFriendlyId
    }

    private fun copyUserIdToClipboard() {
        val guestProfileView = binding.guestProfile.root
        val userId = guestProfileView.findViewById<android.widget.TextView>(R.id.user_id).text.toString()
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
} 