package com.example.nocturnevpn.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nocturnevpn.R
import com.example.nocturnevpn.databinding.FragmentForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class ForgotPasswordFragment : Fragment() {

    private var _binding : FragmentForgotPasswordBinding? = null
    private val binding get() = _binding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.sendLinkButton?.setOnClickListener {
            val email = binding?.forgotPasswordEmailText1?.text?.toString()?.trim() ?: ""
            binding?.forgotPasswordEmailLayout1?.error = null
            if (email.isEmpty()) {
                binding!!.forgotPasswordEmailLayout1.error = "Email is required."
                return@setOnClickListener
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding!!.forgotPasswordEmailLayout1.error = "Please enter a valid email address."
                return@setOnClickListener
            }
            // Check if email is registered in Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        binding!!.forgotPasswordEmailLayout1.error = "This email is not registered."
                    } else {
                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val action = ForgotPasswordFragmentDirections.actionForgotPasswordFragmentToPasswordResetConfirmationFragment(email)
                                    findNavController().navigate(action)
                                } else {
                                    binding!!.forgotPasswordEmailLayout1.error = "Failed to send reset email. Please try again."
                                }
                            }
                    }
                }
                .addOnFailureListener {
                    binding!!.forgotPasswordEmailLayout1.error = "Error checking user registration. Please try again."
                }
        }

        // Clear error when user focuses the field
        binding?.forgotPasswordEmailText1?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding?.forgotPasswordEmailLayout1?.error = null
        }

        binding?.ForgotPasswordSignInTextLink?.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPasswordFragment_to_signInFragment)
        }
    }


}