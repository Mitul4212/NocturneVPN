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


class profileFragment : Fragment() {

    private var _binding: FragmentProfileBinding?= null
    private val binding get() = _binding!!
    
    private lateinit var simpleHistoryAdapter: SimpleHistoryAdapter
    private lateinit var historyHelper: HistoryHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSimpleHistoryRecyclerView()
        setupClickListeners()
        loadRecentHistory()
        setupFragmentResultListener()
        
        // Temporary: Add sample data generation for testing
        addSampleDataButton()
    }

    private fun setupSimpleHistoryRecyclerView() {
        simpleHistoryAdapter = SimpleHistoryAdapter()
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = simpleHistoryAdapter
        }
    }

    private fun setupClickListeners() {
        binding.upgradeButton.setOnClickListener {
            this.findNavController().navigate(R.id.action_profileFragment_to_premiumFragment)
        }

        binding.signInButton.setOnClickListener {
            val intent = Intent(requireContext(), AppAuthActivity::class.java)
            startActivity(intent)
        }

        binding.moreButton.setOnClickListener {
            // Navigate to HistoryFragment
            this.findNavController().navigate(R.id.action_profileFragment_to_historyFragment)
        }

        binding.goToPremiumButton.setOnClickListener {
            this.findNavController().navigate(R.id.action_profileFragment_to_premiumFragment)
        }

        // Add click listener for info update button
        binding.infoUpadetButton.setOnClickListener {
            showEditUserInfoScreen()
        }
    }

    private fun setupFragmentResultListener() {
        setFragmentResultListener("edit_user_info_result") { _, result ->
            val updatedUserName = result.getString("updated_user_name", "")
            val updatedUserEmail = result.getString("updated_user_email", "")
            
            if (updatedUserName.isNotEmpty()) {
                // Update the UI with new user name
                binding.userName.text = updatedUserName
                
                // TODO: Save the updated user info to your database or SharedPreferences
                // Example: saveUserInfoToDatabase(updatedUserName, updatedUserEmail)
                
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditUserInfoScreen() {
        // Get current user data from UI
        val currentUserName = binding.userName.text.toString()
        val currentUserEmail = "test@gmail.com" // This should come from your actual user data source
        
        // Create and show the edit user info fragment as overlay
        val editUserInfoFragment = EditUserInfoFragment.newInstance(currentUserName, currentUserEmail)
        
        // Add as child fragment to keep the profile fragment visible in background
        childFragmentManager.beginTransaction()
            .add(R.id.profile_overlay_container, editUserInfoFragment, "EditUserInfoFragment")
            .addToBackStack(null)
            .commit()
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
    }

}