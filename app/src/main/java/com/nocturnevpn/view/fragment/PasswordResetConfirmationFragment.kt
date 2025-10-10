package com.nocturnevpn.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nocturnevpn.databinding.FragmentPasswordResetConfirmationBinding
import com.nocturnevpn.R
import com.google.firebase.auth.FirebaseAuth

class PasswordResetConfirmationFragment : Fragment() {
    private var _binding: FragmentPasswordResetConfirmationBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<PasswordResetConfirmationFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordResetConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Show the user's email
        binding.enterdEmailForVerif.text = args.email

        // Sign In button
        binding.signinButton.setOnClickListener {
            findNavController().navigate(R.id.action_passwordResetConfirmationFragment_to_signInFragment)
        }

        // Resend link button
        binding.forgotPasswordResendButton.setOnClickListener {
            val email = args.email
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        android.widget.Toast.makeText(requireContext(), "Reset link resent to $email", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(requireContext(), "Failed to resend reset link. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 