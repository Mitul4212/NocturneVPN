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


class profileFragment : Fragment() {

    private var _binding: FragmentProfileBinding?= null
    private val binding get() = _binding!!
    
    private lateinit var simpleHistoryAdapter: SimpleHistoryAdapter
    private lateinit var historyHelper: HistoryHelper
    private var sharedPreference: SharedPreference? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        sharedPreference = SharedPreference(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSimpleHistoryRecyclerView()
        setupClickListeners()
        loadRecentHistory()
        setupFragmentResultListener()
        setupGuestProfile()
        
        // Temporary: Add sample data generation for testing
        addSampleDataButton()
        updateSelectedServerIp()
        updateDeviceUniqueId()
        loadUserName()
        
        // Add touch listener to the main fragment view to handle guest profile button clicks
        setupMainFragmentTouchListener()
    }

    private fun loadUserName() {
        val userName = sharedPreference?.getUserName()
        if (userName != null && userName.isNotEmpty()) {
            binding.userName.text = userName
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
                
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditUserInfoScreen() {
        // Get current user data from UI
        val currentUserName = binding.userName.text.toString()
        val currentUserEmail = "test@gmail.com" // This should come from your actual user data source

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
        // Refresh guest profile visibility
        setupGuestProfile()
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

    private fun setupGuestProfile() {
        // Check if user is signed in or not
        val isUserSignedIn = sharedPreference?.isUserSignedIn() ?: false
        
        if (isUserSignedIn) {
            binding.guestProfile.root.visibility = View.GONE
            binding.profilePageAfterSignin.visibility = View.VISIBLE
        } else {
            binding.guestProfile.root.visibility = View.VISIBLE
            binding.profilePageAfterSignin.visibility = View.GONE
            
            // Make sure the guest profile is on top and can receive touch events
            binding.guestProfile.root.bringToFront()
            binding.guestProfile.root.elevation = 1000f
            
            // Setup click listeners immediately and also after a delay to ensure they work
            setupGuestProfileClickListeners()
            binding.guestProfile.root.post {
                setupGuestProfileClickListeners()
            }
        }
    }

    private fun setupGuestProfileClickListeners() {
        // Get the included layout views
        val guestProfileView = binding.guestProfile.root
        android.util.Log.d("GUEST_PROFILE", "Setting up click listeners for guest profile")
        
        // Setup individual button listeners (this is the working approach)
        setupIndividualButtonListeners(guestProfileView)
        
        // Also add touch listener to the root view to handle button clicks
        guestProfileView.setOnTouchListener { _, event ->
            android.util.Log.d("GUEST_PROFILE", "Guest profile touch detected at: ${event.x}, ${event.y}")
            
            // Check if touch is on sign in button
            val signInButton = guestProfileView.findViewById<android.widget.Button>(R.id.sign_in_button)
            if (signInButton != null && isTouchInView(event, signInButton)) {
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    android.util.Log.d("GUEST_PROFILE", "Sign in button clicked via touch listener")
                    val intent = Intent(requireContext(), AppAuthActivity::class.java)
                    startActivity(intent)
                    return@setOnTouchListener true
                }
            }
            
            // Check if touch is on upgrade button
            val upgradeButton = guestProfileView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.upgrade_button)
            if (upgradeButton != null && isTouchInView(event, upgradeButton)) {
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    android.util.Log.d("GUEST_PROFILE", "Upgrade button clicked via touch listener")
                    this.findNavController().navigate(R.id.action_profileFragment_to_premiumFragment)
                    return@setOnTouchListener true
                }
            }
            
            // Check if touch is on copy button
            val copyButton = guestProfileView.findViewById<android.widget.ImageView>(R.id.copy_button)
            if (copyButton != null && isTouchInView(event, copyButton)) {
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    android.util.Log.d("GUEST_PROFILE", "Copy button clicked via touch listener")
                    copyUserIdToClipboard()
                    return@setOnTouchListener true
                }
            }
            
            false
        }
        
        // Update device unique ID for guest profile
        updateGuestDeviceUniqueId()
    }

    private fun isTouchInView(event: android.view.MotionEvent, view: android.view.View): Boolean {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        
        val x = event.rawX
        val y = event.rawY
        
        return x >= location[0] && 
               x <= location[0] + view.width && 
               y >= location[1] && 
               y <= location[1] + view.height
    }

    private fun setupMainFragmentTouchListener() {
        // Add touch listener to the main fragment view to handle guest profile button clicks
        binding.root.setOnTouchListener { _, event ->
            // Only handle touch events if guest profile is visible
            if (binding.guestProfile.root.visibility == View.VISIBLE) {
                android.util.Log.d("GUEST_PROFILE", "Main fragment touch detected at: ${event.x}, ${event.y}")
                
                val guestProfileView = binding.guestProfile.root
                
                // Check if touch is on sign in button
                val signInButton = guestProfileView.findViewById<android.widget.Button>(R.id.sign_in_button)
                if (signInButton != null && isTouchInView(event, signInButton)) {
                    if (event.action == android.view.MotionEvent.ACTION_UP) {
                        android.util.Log.d("GUEST_PROFILE", "Sign in button clicked via main fragment touch listener")
                        val intent = Intent(requireContext(), AppAuthActivity::class.java)
                        startActivity(intent)
                        return@setOnTouchListener true
                    }
                }
                
                // Check if touch is on upgrade button
                val upgradeButton = guestProfileView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.upgrade_button)
                if (upgradeButton != null && isTouchInView(event, upgradeButton)) {
                    if (event.action == android.view.MotionEvent.ACTION_UP) {
                        android.util.Log.d("GUEST_PROFILE", "Upgrade button clicked via main fragment touch listener")
                        this.findNavController().navigate(R.id.action_profileFragment_to_premiumFragment)
                        return@setOnTouchListener true
                    }
                }
                
                // Check if touch is on copy button
                val copyButton = guestProfileView.findViewById<android.widget.ImageView>(R.id.copy_button)
                if (copyButton != null && isTouchInView(event, copyButton)) {
                    if (event.action == android.view.MotionEvent.ACTION_UP) {
                        android.util.Log.d("GUEST_PROFILE", "Copy button clicked via main fragment touch listener")
                        copyUserIdToClipboard()
                        return@setOnTouchListener true
                    }
                }
            }
            
            false
        }
    }

    private fun setupIndividualButtonListeners(guestProfileView: android.view.View) {
        // Setup sign in button
        val signInButton = guestProfileView.findViewById<android.widget.Button>(R.id.sign_in_button)
        android.util.Log.d("GUEST_PROFILE", "Sign in button found: ${signInButton != null}")
        if (signInButton != null) {
            signInButton.setOnClickListener {
                android.util.Log.d("GUEST_PROFILE", "Sign in button clicked!")
                val intent = Intent(requireContext(), AppAuthActivity::class.java)
                startActivity(intent)
            }
        }

        // Setup upgrade button
        val upgradeButton = guestProfileView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.upgrade_button)
        android.util.Log.d("GUEST_PROFILE", "Upgrade button found: ${upgradeButton != null}")
        if (upgradeButton != null) {
            upgradeButton.setOnClickListener {
                android.util.Log.d("GUEST_PROFILE", "Upgrade button clicked!")
                this.findNavController().navigate(R.id.action_profileFragment_to_premiumFragment)
            }
        }

        // Setup copy button
        val copyButton = guestProfileView.findViewById<android.widget.ImageView>(R.id.copy_button)
        android.util.Log.d("GUEST_PROFILE", "Copy button found: ${copyButton != null}")
        if (copyButton != null) {
            copyButton.setOnClickListener {
                android.util.Log.d("GUEST_PROFILE", "Copy button clicked!")
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
}