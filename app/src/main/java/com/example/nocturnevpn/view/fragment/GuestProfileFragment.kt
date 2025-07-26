package com.example.nocturnevpn.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nocturnevpn.R
import com.example.nocturnevpn.databinding.FragmentGuestProfileBinding
import com.example.nocturnevpn.view.activitys.AppAuthActivity
import com.example.nocturnevpn.utils.getUserFriendlyDeviceId

class GuestProfileFragment : Fragment() {

    private var _binding: FragmentGuestProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuestProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        updateDeviceUniqueId()
    }

    private fun setupClickListeners() {
        binding.signInButton.setOnClickListener {
            val intent = Intent(requireContext(), AppAuthActivity::class.java)
            startActivity(intent)
        }

        binding.upgradeButton.setOnClickListener {
            this.findNavController().navigate(R.id.action_profileFragment_to_premiumFragment)
        }

        binding.copyButton.setOnClickListener {
            // Copy user ID to clipboard
            copyUserIdToClipboard()
        }
    }

    private fun updateDeviceUniqueId() {
        val userFriendlyId = requireContext().getUserFriendlyDeviceId()
        binding.userId.text = userFriendlyId
    }

    private fun copyUserIdToClipboard() {
        val userId = binding.userId.text.toString()
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("User ID", userId)
        clipboard.setPrimaryClip(clip)
        
        // Show toast message
        android.widget.Toast.makeText(context, "User ID copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 