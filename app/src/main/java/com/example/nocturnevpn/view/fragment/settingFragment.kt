package com.example.nocturnevpn.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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



        binding.backArrow.setOnClickListener{
            findNavController().navigateUp()
        }


    }


    companion object {

    }
}