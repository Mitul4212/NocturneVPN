package com.example.nocturnevpn.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.nocturnevpn.databinding.FragmentPremiumBinding
import com.example.nocturnevpn.R
import androidx.navigation.fragment.findNavController

class PremiumFragment : Fragment() {

    private var _binding: FragmentPremiumBinding? = null
    private val binding get() = _binding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentPremiumBinding.inflate(inflater, container, false)
        return binding?.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.close?.setOnClickListener{
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // TEST: When user clicks any subscription plan, show AfterPremiumFragment after 5 seconds
        view.findViewById<View>(R.id.cardView4)?.setOnClickListener {
            it.isEnabled = false // Disable to prevent double click
            view.postDelayed({
                if (findNavController().currentDestination?.id == R.id.premiumFragment) {
                    navigateToAfterPremiumFragment()
                }
            }, 5000)
        }
        view.findViewById<View>(R.id.cardView5)?.setOnClickListener {
            it.isEnabled = false
            view.postDelayed({
                if (findNavController().currentDestination?.id == R.id.premiumFragment) {
                    navigateToAfterPremiumFragment()
                }
            }, 5000)
        }
    }

    private fun navigateToAfterPremiumFragment() {
        findNavController().navigate(R.id.action_premiumFragment_to_afterPremiumFragment)
    }

    companion object {

    }
}