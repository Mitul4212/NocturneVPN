package com.example.nocturnevpn.view.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nocturnevpn.R
import com.example.nocturnevpn.databinding.FragmentAppIconBinding

class appIconFragment : Fragment() {

    private var _binding: FragmentAppIconBinding? = null
    private val binding get() = _binding!!

    private var currentSelectedRadioButton: RadioButton? = null
    private var currentSelectedLayout: View? = null

    // Map radio button IDs to launcher alias component names
    private val iconComponentMap = mapOf(
        R.id.radio_default to "com.example.nocturnevpn.DefaultAlias",
        R.id.radio_dark to "com.example.nocturnevpn.Icon2Alias",
        R.id.radio_dark2 to "com.example.nocturnevpn.Icon3Alias",
        R.id.radio_3d_1 to "com.example.nocturnevpn.Icon4Alias",
        R.id.radio_3d_2 to "com.example.nocturnevpn.Icon5Alias",
        R.id.radio_retro to "com.example.nocturnevpn.Icon6Alias",
        R.id.radio_wether to "com.example.nocturnevpn.WeatherAlias",
        R.id.radio_notes to "com.example.nocturnevpn.NotesAlias",
        R.id.radio_calculeter to "com.example.nocturnevpn.CalculatorAlias"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppIconBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBackButton()
        setupIconSelection()
        autoSelectCurrentIcon()
    }

    private fun setupBackButton() {
        binding.backArrow.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupIconSelection() {
        // Setup click listeners for all icon layouts
        val iconLayouts = mapOf(
            binding.defultIconLayout to R.id.radio_default,
            binding.darkIconLayout to R.id.radio_dark,
            binding.dark2IconLayout to R.id.radio_dark2,
            binding.icon3d1Layout to R.id.radio_3d_1,
            binding.icon3d2Layout to R.id.radio_3d_2,
            binding.retroIconLayout to R.id.radio_retro,
            binding.wetherIconLayout to R.id.radio_wether,
            binding.notesIconLayout to R.id.radio_notes,
            binding.calculeterIconLayout to R.id.radio_calculeter
        )

        iconLayouts.forEach { (layout, radioId) ->
            // Disable radio button clicks to prevent multiple selections
            val radioButton = binding.root.findViewById<RadioButton>(radioId)
            radioButton?.isClickable = false
            radioButton?.isFocusable = false

            layout.setOnClickListener {
                handleIconSelection(radioId)
            }
        }
    }

    private fun handleIconSelection(selectedRadioId: Int) {
        val alias = iconComponentMap[selectedRadioId]
            if (alias != null) {
            showConfirmationDialog(selectedRadioId, alias)
        }
    }

    private fun showConfirmationDialog(selectedRadioId: Int, alias: String) {
        val isDiscreetIcon = selectedRadioId in listOf(
            R.id.radio_wether, 
            R.id.radio_notes, 
            R.id.radio_calculeter
        )
        
        val title = if (isDiscreetIcon) "Change to Discreet Icon" else "Change App Icon"
        val message = if (isDiscreetIcon) {
            "This will disguise your VPN app with a different icon. The app will still function as NocturneVPN but will appear as a different app on your home screen. Continue?"
        } else {
            "Are you sure you want to change the app icon?"
        }

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                changeAppIcon(selectedRadioId, alias)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                // Restore previous selection if user cancels
                restorePreviousSelection()
                dialog.dismiss()
            }
            .show()
    }

    private fun changeAppIcon(selectedRadioId: Int, enabledAlias: String) {
        try {
            val pm = requireActivity().packageManager
            val packageName = requireActivity().packageName

            Log.d("IconChange", "Changing icon to: $enabledAlias")

            // Disable ALL activity aliases first
            iconComponentMap.values.forEach { alias ->
                val componentName = ComponentName(packageName, alias)
                val currentState = pm.getComponentEnabledSetting(componentName)
                Log.d("IconChange", "Disabling $alias (current state: $currentState)")
                pm.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            // Enable the selected alias (including default)
            val selectedComponent = ComponentName(packageName, enabledAlias)
            Log.d("IconChange", "Enabling $enabledAlias")
            pm.setComponentEnabledSetting(
                selectedComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // Update UI to show selection
            updateUISelection(selectedRadioId)

            // Show success message
            val isDiscreetIcon = enabledAlias.contains("WeatherAlias") || 
                                enabledAlias.contains("NotesAlias") || 
                                enabledAlias.contains("CalculatorAlias")
            
            val message = if (isDiscreetIcon) "App disguised successfully!" else "App icon changed!"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

            // Simple restart
            restartApp()

        } catch (e: Exception) {
            Log.e("IconChange", "Error changing icon", e)
            Toast.makeText(requireContext(), "Failed to change app icon: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUISelection(selectedRadioId: Int) {
        // Clear ALL radio buttons first
        iconComponentMap.keys.forEach { radioId ->
            val radioButton = binding.root.findViewById<RadioButton>(radioId)
            radioButton?.isChecked = false
            
            val layout = radioButton?.parent as? View
            layout?.setBackgroundResource(0)
        }

        // Set only the selected radio button
        val selectedRadioButton = binding.root.findViewById<RadioButton>(selectedRadioId)
        selectedRadioButton?.isChecked = true
        
        val selectedLayout = selectedRadioButton?.parent as? View
        selectedLayout?.setBackgroundResource(R.drawable.selected_icon_border)

        // Update references
        currentSelectedRadioButton = selectedRadioButton
        currentSelectedLayout = selectedLayout
    }

    private fun restorePreviousSelection() {
        // Restore the previous selection when user cancels
        currentSelectedRadioButton?.isChecked = true
        currentSelectedLayout?.setBackgroundResource(R.drawable.selected_icon_border)
    }

    private fun autoSelectCurrentIcon() {
        try {
        val pm = requireActivity().packageManager
        val packageName = requireActivity().packageName

            // Find which alias is currently enabled
        val currentAlias = iconComponentMap.values.find { alias ->
            val componentName = ComponentName(packageName, alias)
            pm.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }

            // If no alias is enabled, it means we're using the default icon (main activity)
            val selectedRadioId = if (currentAlias != null) {
                iconComponentMap.entries.find { it.value == currentAlias }?.key
            } else {
                R.id.radio_default
            }

        selectedRadioId?.let { radioId ->
                updateUISelection(radioId)
            }

        } catch (e: Exception) {
            Log.e("IconChange", "Error in autoSelectCurrentIcon", e)
            // If there's any error, default to the first icon
            updateUISelection(R.id.radio_default)
        }
    }

    private fun restartApp() {
        try {
            Log.d("IconChange", "Restarting app")
            
            // Force launcher to refresh by going to home screen first
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            requireActivity().startActivity(homeIntent)
            
            // Wait a bit for launcher to refresh
            Thread.sleep(1000)
            
            val intent = requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                requireActivity().startActivity(intent)
                requireActivity().finishAffinity()
                Log.d("IconChange", "App restart initiated")
        } else {
                Log.e("IconChange", "Failed to get launch intent")
                Toast.makeText(requireContext(), "Failed to restart app", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("IconChange", "Error restarting app", e)
            Toast.makeText(requireContext(), "Error restarting app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}