package com.example.nocturnevpn.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nocturnevpn.R
import com.example.nocturnevpn.databinding.FragmentProfileBinding
import com.example.nocturnevpn.view.activitys.AppAuthActivity


class profileFragment : Fragment() {

    private var _binding: FragmentProfileBinding?= null
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_profile, container, false)
//        return view
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        _binding?.upgradeButton?.setOnClickListener{
            this.findNavController().navigate(R.id.action_profileFragment_to_premiumFragment)

        }

        _binding?.signInButton?.setOnClickListener{
            val intent = Intent(requireContext(), AppAuthActivity::class.java)
            startActivity(intent)
        }


    }




    companion object {

    }
}