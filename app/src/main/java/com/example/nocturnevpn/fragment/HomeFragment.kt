package com.example.nocturnevpn.fragment

import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nocturnevpn.R
import com.example.nocturnevpn.ServerActivity
import com.example.nocturnevpn.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {


    private var _binding : FragmentHomeBinding?= null
    private val binding get() = _binding





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout first
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    var isConnected = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.goProButton?.setOnClickListener{
            this.findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)
        }



        val chooseSerevr = view.findViewById<CardView>(R.id.choose_server)
        val connectButton = view.findViewById<CardView>(R.id.connect_button)

        val proLayout = view.findViewById<LinearLayout>(R.id.go_pro_button)
        val proText = view.findViewById<TextView>(R.id.go_pro) // Assuming you have a TextView inside the layout

        // Apply gradient text effect
        val paint = proText.paint
        val width = paint.measureText(proText.text.toString())
        proText.paint.shader = LinearGradient(
            0f, 0f, width, proText.textSize,
            intArrayOf(Color.parseColor("#6622CC"), Color.parseColor("#22CCC2")),
            null, Shader.TileMode.REPEAT
        )

        // Click listeners with proper context

        chooseSerevr.setOnClickListener{
            Toast.makeText(requireContext(),"Choose server", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireActivity(), ServerActivity::class.java)
            startActivity(intent)
        }

        connectButton.setOnClickListener {

            isConnected = !isConnected // Toggle state
            val message = if (isConnected) "Connected" else "Disconnected"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

//        connectButton.setOnClickListener{
//            Toast.makeText(requireContext(),"Connected", Toast.LENGTH_SHORT).show()
//        }

    }

    companion object {

    }
}













//package com.example.nocturnevpn.fragment
//
//import android.content.Intent
//import android.graphics.Color
//import android.graphics.LinearGradient
//import android.graphics.Shader
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.LinearLayout
//import android.widget.TextView
//import android.widget.Toast
//import androidx.cardview.widget.CardView
//import androidx.fragment.app.Fragment
//import androidx.navigation.fragment.findNavController
//import com.example.nocturnevpn.R
//import com.example.nocturnevpn.ServerActivity
//import com.example.nocturnevpn.databinding.FragmentHomeBinding
//
//
//class HomeFragment : Fragment() {
//
//    private var _binding : FragmentHomeBinding?= null
//    private val binding get() = _binding
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
//        // Inflate the layout first
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        return binding?.root
//    }
//
//    var isConnected = false
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        binding?.goProButton?.setOnClickListener{
//            this.findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)
//        }
//
//
//
//        val chooseSerevr = view.findViewById<CardView>(R.id.choose_server)
//        val connectButton = view.findViewById<CardView>(R.id.connect_button)
//
//        val proLayout = view.findViewById<LinearLayout>(R.id.go_pro_button)
//        val proText = view.findViewById<TextView>(R.id.go_pro) // Assuming you have a TextView inside the layout
//
//        // Apply gradient text effect
//        val paint = proText.paint
//        val width = paint.measureText(proText.text.toString())
//        proText.paint.shader = LinearGradient(
//            0f, 0f, width, proText.textSize,
//            intArrayOf(Color.parseColor("#6622CC"), Color.parseColor("#22CCC2")),
//            null, Shader.TileMode.REPEAT
//        )
//
//        // Click listeners with proper context
//
//        chooseSerevr.setOnClickListener{
//            Toast.makeText(requireContext(),"Choose server", Toast.LENGTH_SHORT).show()
//            val intent = Intent(requireActivity(), ServerActivity::class.java)
//            startActivity(intent)
//        }
//
//        connectButton.setOnClickListener {
//
//            isConnected = !isConnected // Toggle state
//            val message = if (isConnected) "Connected" else "Disconnected"
//            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
//        }
//
////        connectButton.setOnClickListener{
////            Toast.makeText(requireContext(),"Connected", Toast.LENGTH_SHORT).show()
////        }
//
//    }
//
//    companion object {
//
//    }
//}