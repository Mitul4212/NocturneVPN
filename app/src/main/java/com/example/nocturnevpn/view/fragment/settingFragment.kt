package com.example.nocturnevpn.view.fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openNotificationSettings(context: android.content.Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }



    companion object {

    }
}