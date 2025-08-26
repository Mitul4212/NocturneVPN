package com.example.nocturnevpn.view.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nocturnevpn.R
import com.example.nocturnevpn.databinding.FragmentVerificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class VerificationFragment : Fragment() {

    private var _binding: FragmentVerificationBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private var autoCheckHandler: Handler? = null
    private var autoCheckRunnable: Runnable? = null
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = auth.currentUser
        binding.usersEmailForVerif.text = user?.email ?: "your email"

        // Resend verification email
        binding.resendButton.setOnClickListener {
            user?.sendEmailVerification()
                ?.addOnSuccessListener {
                    Toast.makeText(requireContext(), "Verification email resent.", Toast.LENGTH_SHORT).show()
                }?.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to resend email.", Toast.LENGTH_SHORT).show()
                }
        }

        // Sign In button checks verification
        binding.signinButton.setOnClickListener {
            checkAndRedirectIfVerified()
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndRedirectIfVerified(auto = true)
    }

    private fun checkAndRedirectIfVerified(auto: Boolean = false) {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener {
            if (user.isEmailVerified) {
                // Update Firestore: set isVerified = true (robust)
                val db = FirebaseFirestore.getInstance()
                val uid = user.uid
                val updateMap = mapOf("isVerified" to true)
                db.collection("users").document(uid).set(updateMap, SetOptions.merge())
                    .addOnSuccessListener {
                        if (!hasNavigated && isAdded) {
                            hasNavigated = true
                            findNavController().navigate(R.id.action_verificationFragment_to_signInFragment)
                        }
                    }
                    .addOnFailureListener {
                        if (!auto) Toast.makeText(requireContext(), "Could not update verification status in database.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                if (!auto) Toast.makeText(requireContext(), "Please verify your email first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hasNavigated = false
        _binding = null
    }
}
