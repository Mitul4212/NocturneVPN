package com.example.nocturnevpn.view.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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

    private var lastCheckedRadioButton: RadioButton? = null
    private var lastSelectedItemLayout: View? = null

    // Map radio button IDs to launcher alias component names
    private val iconComponentMap = mapOf(
        R.id.radio_default to "com.example.nocturnevpn.DefaultAlias",
        R.id.radio_dark to "com.example.nocturnevpn.Icon2Alias",
        R.id.radio_dark2 to "com.example.nocturnevpn.Icon3Alias",
        R.id.radio_3d_1 to "com.example.nocturnevpn.Icon4Alias",
        R.id.radio_3d_2 to "com.example.nocturnevpn.Icon5Alias",
        R.id.radio_retro to "com.example.nocturnevpn.Icon6Alias",
        // Discreet icons for Row 3
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

        binding.backArrow.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        autoSelectCurrentIcon()

        iconComponentMap.keys.forEach { radioId ->
            val radioButton = binding.root.findViewById<RadioButton>(radioId)
            val parentLayout = radioButton?.parent as? View

            // Clear default selection behavior
            radioButton?.isClickable = false
            radioButton?.isFocusable = false

            // Handle clicks from both layout and radio
            parentLayout?.setOnClickListener {
                val alias = iconComponentMap[radioId]
                if (alias != null) {
                    showConfirmDialog(radioId, alias)
                }
            }
        }

        // Add click listeners for the discreet icon layouts (Row 3)
        binding.wetherIconLayout.setOnClickListener {
            val alias = iconComponentMap[R.id.radio_wether]
            if (alias != null) {
                showConfirmDialog(R.id.radio_wether, alias)
            }
        }

        binding.notesIconLayout.setOnClickListener {
            val alias = iconComponentMap[R.id.radio_notes]
            if (alias != null) {
                showConfirmDialog(R.id.radio_notes, alias)
            }
        }

        binding.calculeterIconLayout.setOnClickListener {
            val alias = iconComponentMap[R.id.radio_calculeter]
            if (alias != null) {
                showConfirmDialog(R.id.radio_calculeter, alias)
            }
        }
    }

    // ✅ Show confirmation dialog before changing icon
    private fun showConfirmDialog(selectedRadioId: Int, alias: String) {
        val builder = AlertDialog.Builder(requireContext())
        
        // Check if this is a discreet icon
        val isDiscreetIcon = selectedRadioId in listOf(
            R.id.radio_wether, 
            R.id.radio_notes, 
            R.id.radio_calculeter
        )
        
        if (isDiscreetIcon) {
            builder.setTitle("Change to Discreet Icon")
            builder.setMessage("This will disguise your VPN app with a different icon. The app will still function as NocturneVPN but will appear as a different app on your home screen. Continue?")
        } else {
            builder.setTitle("Change App Icon")
            builder.setMessage("Are you sure you want to change the app icon?")
        }
        
        builder.setPositiveButton("Yes") { _, _ ->
            proceedIconChange(selectedRadioId, alias)
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            // Restore previous selection and borders
            iconComponentMap.keys.forEach { id ->
                val rb = binding.root.findViewById<RadioButton>(id)
                rb?.isChecked = false
                val layout = rb?.parent as? View
                layout?.setBackgroundResource(0)
            }

            // Re-check last selected radio and layout
            lastCheckedRadioButton?.isChecked = true
            lastSelectedItemLayout?.setBackgroundResource(R.drawable.selected_icon_border)

            dialog.dismiss()
        }
        builder.show()
    }


    // ✅ Extracted icon change logic here
    private fun proceedIconChange(selectedRadioId: Int, alias: String) {
        // Uncheck all and remove borders
        iconComponentMap.keys.forEach { id ->
            val rb = binding.root.findViewById<RadioButton>(id)
            rb?.isChecked = id == selectedRadioId

            val parentLayout = rb?.parent as? View
            parentLayout?.setBackgroundResource(0)
        }

        // Set selected radio button and border
        val selectedRadioButton = binding.root.findViewById<RadioButton>(selectedRadioId)
        selectedRadioButton?.isChecked = true
        val selectedLayout = selectedRadioButton?.parent as? View
        selectedLayout?.setBackgroundResource(R.drawable.selected_icon_border)

        // Save current selection
        lastCheckedRadioButton = selectedRadioButton
        lastSelectedItemLayout = selectedLayout

        changeAppIcon(requireContext(), alias)
    }

    // ✅ Modified to show confirmation first
    private fun onIconSelected(selectedRadioId: Int) {
        val alias = iconComponentMap[selectedRadioId]
        if (alias != null) {
            showConfirmDialog(selectedRadioId, alias)
        }
    }

    // Automatically select currently active launcher icon
    private fun autoSelectCurrentIcon() {
        val pm = requireActivity().packageManager
        val packageName = requireActivity().packageName

        val currentAlias = iconComponentMap.values.find { alias ->
            val componentName = ComponentName(packageName, alias)
            pm.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }

        val selectedRadioId = iconComponentMap.entries.find { it.value == currentAlias }?.key

        selectedRadioId?.let { radioId ->
            val radioButton = binding.root.findViewById<RadioButton>(radioId)
            radioButton.isChecked = true
            lastCheckedRadioButton = radioButton
            lastSelectedItemLayout = radioButton.parent as? View
            lastSelectedItemLayout?.setBackgroundResource(R.drawable.selected_icon_border)
        }
    }

    // Change the app launcher icon
    private fun changeAppIcon(context: Context, enabledAlias: String) {
        val pm = context.packageManager
        val packageName = context.packageName

        iconComponentMap.values.forEach { alias ->
            pm.setComponentEnabledSetting(
                ComponentName(packageName, alias),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        pm.setComponentEnabledSetting(
            ComponentName(packageName, enabledAlias),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // Check if this is a discreet icon
        val isDiscreetIcon = enabledAlias.contains("WeatherAlias") || 
                            enabledAlias.contains("NotesAlias") || 
                            enabledAlias.contains("CalculatorAlias")
        
        if (isDiscreetIcon) {
            Toast.makeText(context, "App disguised successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "App icon changed!", Toast.LENGTH_SHORT).show()
        }

        restartApp(context)
    }

    private fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            if (context is Activity) context.finishAffinity()
            Runtime.getRuntime().exit(0)
        } else {
            Toast.makeText(context, "Failed to restart app", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}










//
//
//
//
//package com.example.nocturnevpn.view.fragment
//
//import android.app.AlertDialog
//import android.content.ComponentName
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.util.Log
//import android.view.Gravity
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.LinearLayout
//import android.widget.RadioButton
//import androidx.fragment.app.Fragment
//import com.example.nocturnevpn.R
//import com.example.nocturnevpn.databinding.FragmentAppIconBinding
//
//
//class appIconFragment : Fragment() {
//
//    private var _binding: FragmentAppIconBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var iconContainer: LinearLayout
//
//    private val PREFS_NAME = "icon_prefs"
//    private val KEY_SELECTED_ICON_ALIAS = "selected_icon_alias"
//
//    private var lastCheckedRadioButton: RadioButton? = null
//    private var lastSelectedItemLayout: LinearLayout? = null
//
//
//
//    // Replace with your actual package name
//    private val packageName = "com.example.nocturnevpn"
//
//    data class IconOption(val name: String, val resId: Int, val aliasName: String)
//
//    private val icons = listOf(
//        IconOption("Default Icon", R.mipmap.ic_launcher, "$packageName.DefaultAlias"),
//        IconOption("Dark Icon", R.mipmap.ic_launcher_dark, "$packageName.Icon2Alias"),
//        IconOption("Dark 2 Icon", R.mipmap.ic_launcher_dark_2, "$packageName.Icon3Alias"),
//        IconOption("3D Icon 1", R.mipmap.ic_launcher_3d_1, "$packageName.Icon4Alias"),
//        IconOption("3D Icon 2", R.mipmap.ic_launcher_3d_2, "$packageName.Icon5Alias"),
//        IconOption("Retro Icon", R.mipmap.ic_launcher_3d_retro, "$packageName.Icon6Alias")
//    )
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout for this fragment
//        _binding = FragmentAppIconBinding.inflate(layoutInflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        iconContainer = binding.iconContainer
//        displayIconOptions()
//
//
//    }
//
//    private fun displayIconOptions() {
//        val rowItemCount = 3
//        var currentRow: LinearLayout? = null
//
//        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
//        val selectedAlias = prefs.getString(KEY_SELECTED_ICON_ALIAS, null)
//
//        icons.forEachIndexed { index, iconOption ->
//            if (index % rowItemCount == 0) {
//                currentRow = LinearLayout(requireContext()).apply {
//                    orientation = LinearLayout.HORIZONTAL
//                    layoutParams = LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.MATCH_PARENT,
//                        LinearLayout.LayoutParams.WRAP_CONTENT
//                    )
//                    gravity = Gravity.CENTER
//                }
//                iconContainer.addView(currentRow)
//            }
//
//            val itemLayout = LinearLayout(requireContext()).apply {
//                orientation = LinearLayout.VERTICAL
//                gravity = Gravity.CENTER_HORIZONTAL
//                setPadding(24, 24, 24, 24)
//            }
//
//            val imageView = ImageView(requireContext()).apply {
//                setImageResource(iconOption.resId)
//                layoutParams = LinearLayout.LayoutParams(150, 150)
//
//            }
//
//            val isSelected = selectedAlias == null && iconOption.aliasName.endsWith("DefaultAlias") ||
//                    iconOption.aliasName == selectedAlias
//
//            val radioButton = RadioButton(requireContext()).apply {
//                text = iconOption.name
//                textSize = 12f
//                gravity = Gravity.CENTER
//                isChecked = isSelected
//
//                Log.d("IconDebug", "Saved alias = $selectedAlias")
//            }
//
//            if (isSelected) {
//                itemLayout.setBackgroundResource(R.drawable.selected_icon_border)
//                lastCheckedRadioButton = radioButton
//                lastSelectedItemLayout = itemLayout
//            } else {
//                itemLayout.setBackgroundResource(0) // remove background for unselected
//            }
//
//
//            radioButton.setOnClickListener {
//                // Temporarily mark this one checked (optimistic)
//                lastCheckedRadioButton?.isChecked = false
//                lastSelectedItemLayout?.setBackgroundResource(0)
//
//                radioButton.isChecked = true
//                itemLayout.setBackgroundResource(R.drawable.selected_icon_border)
//
//                // Save references in case we need to roll back
//                val previousRadioButton = lastCheckedRadioButton
//                val previousItemLayout = lastSelectedItemLayout
//
//                lastCheckedRadioButton = radioButton
//                lastSelectedItemLayout = itemLayout
//
//                // Show confirmation dialog
//                AlertDialog.Builder(requireContext())
//                    .setTitle("Change App Icon")
//                    .setMessage("Do you want to change the app icon to '${iconOption.name}'?")
//                    .setPositiveButton("Yes") { _, _ ->
//                        changeIcon(iconOption.aliasName)
//                    }
//                    .setNegativeButton("Cancel") { _, _ ->
//                        // Roll back visual state
//                        radioButton.isChecked = false
//                        itemLayout.setBackgroundResource(0)
//
//                        previousRadioButton?.isChecked = true
//                        previousItemLayout?.setBackgroundResource(R.drawable.selected_icon_border)
//
//                        // Restore previous selected references
//                        lastCheckedRadioButton = previousRadioButton
//                        lastSelectedItemLayout = previousItemLayout
//                    }
//                    .show()
//            }
//
//
//            itemLayout.addView(imageView)
//            itemLayout.addView(radioButton)
//
//            currentRow?.addView(itemLayout)
//        }
//    }
//
//
//
//
//    private fun showConfirmationDialog(iconOption: IconOption) {
//        AlertDialog.Builder(requireContext())
//            .setTitle("Change App Icon")
//            .setMessage("Do you want to change the app icon to '${iconOption.name}'?")
//            .setPositiveButton("Yes") { _, _ ->
//                changeIcon(iconOption.aliasName)
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//
//    private fun changeIcon(enabledAlias: String) {
//        val pm = requireActivity().packageManager
//
//        // Save selected alias in SharedPreferences
//        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
//        prefs.edit().putString(KEY_SELECTED_ICON_ALIAS, enabledAlias).commit()  // Use commit, not apply
//
//
//
//        icons.forEach {
//            val state = if (it.aliasName == enabledAlias)
//                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
//            else
//                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
//
//            pm.setComponentEnabledSetting(
//                ComponentName(requireContext(), it.aliasName),
//                state,
//                PackageManager.DONT_KILL_APP
//            )
//        }
//
//        val intent = requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)
//        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
//        if (intent != null) {
//            startActivity(intent)
//        }
//        requireActivity().finish()
//        Runtime.getRuntime().exit(0)
//    }
//
//
//    companion object {
//
//    }
//}