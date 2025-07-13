package com.example.nocturnevpn.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nocturnevpn.R
import com.example.nocturnevpn.databinding.FragmentSignUpBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.nocturnevpn.utils.getUserFriendlyDeviceId
import android.provider.Settings
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Clear errors when user starts typing
        binding.fullNameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.emailLayout1.error = null
        }
        binding.emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.emailLayoutSignUp.error = null
        }
        binding.passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.passwordLayout.error = null
        }
        binding.confirmPasswordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.confirmPasswordLayout.error = null
        }

        // Sign Up Button
        binding.signUpButton.setOnClickListener {
            registerUser()
        }

        // Sign In Link
        binding.signInTextLink.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
        }
    }

    private fun isValidPassword(password: String): Boolean {
        // At least 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special char
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{8,}$")
        return passwordPattern.containsMatchIn(password)
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun getPasswordError(password: String): String? {
        if (password.length < 8) return "At least 8 characters required."
        if (!password.any { it.isUpperCase() }) return "At least one uppercase letter required."
        if (!password.any { it.isLowerCase() }) return "At least one lowercase letter required."
        if (!password.any { it.isDigit() }) return "At least one number required."
        if (!password.any { "@#${'$'}%^&+=!".contains(it) }) return "At least one special character required (@#${'$'}%^&+=!)."
        return null
    }

    private fun registerUser() {
        val name = binding.fullNameEditText.text?.toString()?.trim() ?: ""
        val email = binding.emailEditText.text?.toString()?.trim() ?: ""
        val password = binding.passwordEditText.text?.toString()?.trim() ?: ""
        val confirmPassword = binding.confirmPasswordEditText.text?.toString()?.trim() ?: ""
        val agreedToTerms = binding.termsAndPolicyCheckButton.isChecked

        var hasError = false
        binding.emailLayout1.error = null
        binding.emailLayoutSignUp.error = null
        binding.passwordLayout.error = null
        binding.confirmPasswordLayout.error = null

        if (name.isEmpty()) {
            binding.emailLayout1.error = "Required."
            hasError = true
        }
        if (email.isEmpty()) {
            binding.emailLayoutSignUp.error = "Required."
            hasError = true
        } else if (!isValidEmail(email)) {
            binding.emailLayoutSignUp.error = "Invalid email."
            hasError = true
        }
        if (password.isEmpty()) {
            binding.passwordLayout.error = "Required."
            hasError = true
        } else {
            val passwordError = getPasswordError(password)
            if (passwordError != null) {
                binding.passwordLayout.error = passwordError
                hasError = true
            }
        }
        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordLayout.error = "Required."
            hasError = true
        } else if (password != confirmPassword) {
            binding.confirmPasswordLayout.error = "Passwords do not match."
            hasError = true
        }
        if (!binding.termsAndPolicyCheckButton.isChecked) {
            Toast.makeText(context, "Please agree to Terms of Service and Privacy Policy.", Toast.LENGTH_SHORT).show()
            hasError = true
        }
        if (hasError) return

        binding.signUpButton.isEnabled = false
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.signUpButton.isEnabled = true
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    saveUserDataToFirestore(userId, name, email, agreedToTerms)
                    uploadDeviceInfoToFirestore(userId)
                    sendEmailVerification()
                } else {
                    binding.emailLayoutSignUp.error = "Registration failed: ${task.exception?.message}"
                }
            }
    }

    private fun saveUserDataToFirestore(userId: String, name: String, email: String, agreedToTerms: Boolean) {
        val db = FirebaseFirestore.getInstance()
        val user = hashMapOf(
            "name" to name,
            "email" to email,
            "agreedToTerms" to agreedToTerms,
            "isVerified" to false
        )
        db.collection("users").document(userId).set(user)
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
                // Device exists, update last_login
                for (doc in querySnapshot.documents) {
                    doc.reference.update(
                        mapOf(
                            "last_login" to now,
                            "last_login_readable" to readableNow
                        )
                    )
                }
            }
        }
    }

    private fun sendEmailVerification() {
        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Verification email sent to ${user.email}", Toast.LENGTH_LONG).show()
                    // Navigate to verification screen
                    findNavController().navigate(R.id.action_signUpFragment_to_verificationFragment)
                } else {
                    Toast.makeText(context, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
