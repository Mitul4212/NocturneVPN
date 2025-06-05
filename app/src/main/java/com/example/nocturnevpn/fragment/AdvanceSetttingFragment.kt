package com.example.nocturnevpn.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.nocturnevpn.databinding.FragmentAdvanceSetttingBinding

class AdvanceSetttingFragment : Fragment() {

    // View binding to interact with UI elements
    private var _binding: FragmentAdvanceSetttingBinding? = null
    private val binding get() = _binding!!

    // SharedPreferences to store Dark Mode preference
    private lateinit var sharedPreferences: SharedPreferences

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

        // Initialize SharedPreferences to retrieve Dark Mode settings
        sharedPreferences = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Load the saved Dark Mode state and apply it to the switch
        val isDarkMode = sharedPreferences.getBoolean("DarkMode", false)
        binding.darkmode.isChecked = isDarkMode

        // Set up the listener for the Dark Mode toggle switch
        binding.darkmode.setOnCheckedChangeListener { _, isChecked ->
            toggleDarkMode(isChecked)
        }

        // Handle back button click to navigate back
        binding.backArrow.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    // Function to enable or disable Dark Mode
    private fun toggleDarkMode(enable: Boolean) {
        if (enable) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        // Save the user's preference in SharedPreferences
        sharedPreferences.edit().putBoolean("DarkMode", enable).apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks by setting binding to null
    }
}
