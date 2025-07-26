package com.example.nocturnevpn.view.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nocturnevpn.R
import com.example.nocturnevpn.databinding.FragmentSignInBinding
import com.example.nocturnevpn.utils.SocialAuthHelper
import com.example.nocturnevpn.view.activitys.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestore
import com.example.nocturnevpn.utils.getUserFriendlyDeviceId
import android.provider.Settings
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var socialAuthHelper: SocialAuthHelper

    // Modern Activity Result API
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("SignInFragment", "Google Sign-In result received: ${result.resultCode}")
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            socialAuthHelper.handleActivityResult(9001, result.resultCode, result.data)
        } else {
            Log.d("SignInFragment", "Google Sign-In was cancelled or failed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        socialAuthHelper = SocialAuthHelper(requireContext())
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

        // Social Authentication Buttons
        binding.googleSignInBtn.setOnClickListener {
            signInWithGoogle()
        }

        binding.facebookSignInBtn.setOnClickListener {
            signInWithFacebook()
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
                                    // Upload device info to Firestore
                                    uploadDeviceInfoToFirestore(user.uid)
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

    private fun uploadDeviceInfoToFirestore(userId: String) {
        val db = FirebaseFirestore.getInstance()
        val context = requireContext()
        val userFriendlyId = context.getUserFriendlyDeviceId()
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
        val model = Build.MODEL ?: "MODEL"
        val manufacturer = Build.MANUFACTURER ?: "MANUFACTURER"
        val osVersion = Build.VERSION.RELEASE ?: "RELEASE"
        val now = System.currentTimeMillis()
        val readableNow = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(now))
        val deviceInfo = hashMapOf(
            "device_id" to userFriendlyId,
            "android_id" to androidId,
            "model" to model,
            "manufacturer" to manufacturer,
            "os_version" to osVersion,
            "first_login" to now,
            "first_login_readable" to readableNow,
            "last_login" to now,
            "last_login_readable" to readableNow
        )
        val userDoc = db.collection("users").document(userId)
        val devicesCol = userDoc.collection("devices")
        // Check if device already exists for this user
        devicesCol.whereEqualTo("device_id", userFriendlyId).get().addOnSuccessListener { querySnapshot ->
            if (querySnapshot.isEmpty) {
                // New device for this user
                devicesCol.add(deviceInfo)
            } else {
                // Device exists, update last_login and set first_login_readable if missing
                for (doc in querySnapshot.documents) {
                    val updates = mutableMapOf<String, Any>(
                        "last_login" to now,
                        "last_login_readable" to readableNow
                    )
                    val firstLoginReadable = doc.getString("first_login_readable")
                    if (firstLoginReadable.isNullOrEmpty()) {
                        updates["first_login_readable"] = readableNow
                    }
                    doc.reference.update(updates)
                }
            }
        }
    }

    private fun signInWithGoogle() {
        Log.d("SignInFragment", "Google sign-in button clicked")
        socialAuthHelper.signInWithGoogleModern(googleSignInLauncher, object : SocialAuthHelper.AuthCallback {
            override fun onSuccess(userId: String, email: String, name: String) {
                Log.d("SignInFragment", "Google sign-in successful for user: $email")
                Toast.makeText(context, "Google sign-in successful!", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }

            override fun onFailure(errorMessage: String) {
                Log.e("SignInFragment", "Google sign-in failed: $errorMessage")
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun signInWithFacebook() {
        socialAuthHelper.signInWithFacebook(object : SocialAuthHelper.AuthCallback {
            override fun onSuccess(userId: String, email: String, name: String) {
                Toast.makeText(context, "Facebook sign-in successful!", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }

            override fun onFailure(errorMessage: String) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun navigateToHome() {
        val intent = Intent(requireContext(), HomeActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    // Removed old onActivityResult since we're using modern Activity Result API

    companion object {

    }
}