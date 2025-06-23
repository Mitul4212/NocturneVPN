package com.example.nocturnevpn.view.fragment

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nocturnevpn.R
import com.example.nocturnevpn.databinding.FragmentSettingBinding

class settingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)// Non-null assertion operator to safely access binding

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout using View Binding
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
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

        // Rating dialog logic
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasRated = prefs.getBoolean("has_rated", false)

        binding.rateApp.setOnClickListener {
            showRatingDialog(prefs)
        }

        // Show rating dialog after delay if not rated
        if (!hasRated) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (!prefs.getBoolean("has_rated", false)) {
                    showRatingDialog(prefs)
                }
            }, 1000 * 60 * 2) // 2 minutes delay
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
        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }

        val ratingBar = dialog.findViewById<RatingBar>(R.id.ratingBar)
        val btnSubmit = dialog.findViewById<Button>(R.id.btnSubmit)
        val tvNoThanks = dialog.findViewById<TextView>(R.id.tvNoThanks)
        val ivClose = dialog.findViewById<LinearLayout>(R.id.ivClose)

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating
            if (rating >= 4) {
                // Redirect to Play Store
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

    companion object {

    }
}