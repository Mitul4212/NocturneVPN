package com.example.nocturnevpn.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nocturnevpn.R
import com.example.nocturnevpn.databinding.FragmentSignInBinding
import com.example.nocturnevpn.view.activitys.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestore

class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
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
        _binding = FragmentSignInBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signInButton.setOnClickListener {
            signInUser()
        }

        binding.signUpTextLink.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }

        binding.forgetPasswordTextLink.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_forgotPasswordFragment)
        }

        // Clear errors when user focuses on the fields
        binding.emailEditText1.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.emailLayout1.error = null
        }
        binding.passwordEditTextSignIn.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.passwordLayoutSignIn.error = null
        }
    }

    private fun signInUser() {
        val email = binding.emailEditText1.text?.toString()?.trim() ?: ""
        val password = binding.passwordEditTextSignIn.text?.toString()?.trim() ?: ""
        binding.emailLayout1.error = null
        binding.passwordLayoutSignIn.error = null
        // Note: Cursor visibility is handled by XML attribute. If still invisible, try changing the color in edittext_cursor_color.xml or update Material Components library.
        var hasError = false
        if (email.isEmpty()) {
            binding.emailLayout1.error = "Required."
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout1.error = "Invalid email format."
            hasError = true
        }
        if (password.isEmpty()) {
            binding.passwordLayoutSignIn.error = "Required."
            hasError = true
        }
        if (hasError) return
        binding.signInButton.isEnabled = false
        // First, check if email is registered in Firestore
        val db = FirebaseFirestore.getInstance()
        db.collection("users").whereEqualTo("email", email).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    binding.emailLayout1.error = "User is not registered."
                    binding.signInButton.isEnabled = true
                } else {
                    // Email is registered, proceed to FirebaseAuth sign in
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            binding.signInButton.isEnabled = true
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null && user.isEmailVerified) {
                                    Toast.makeText(context, "Sign in successful!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(requireContext(), HomeActivity::class.java)
                                    startActivity(intent)
                                    requireActivity().finish()
                                } else {
                                    Toast.makeText(context, "Please verify your email before signing in.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                val exception = task.exception
                                if (exception is FirebaseAuthInvalidCredentialsException) {
                                    binding.passwordLayoutSignIn.error = "Incorrect password."
                                } else {
                                    binding.passwordLayoutSignIn.error = "Invalid email or password."
                                }
                            }
                        }
                }
            }
            .addOnFailureListener {
                binding.emailLayout1.error = "Error checking user registration."
                binding.signInButton.isEnabled = true
            }
    }

    companion object {

    }
}