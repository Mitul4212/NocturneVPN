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
import com.example.nocturnevpn.utils.FirebaseUtils
import com.example.nocturnevpn.view.activitys.HomeActivity
import com.example.nocturnevpn.SharedPreference
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
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        Log.d("SignInFragment", "onViewCreated called")

        binding.signInButton.setOnClickListener {
            Log.d("SignInFragment", "Sign In button clicked!")
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
        
        // Add retry functionality when there are network errors
        binding.emailLayout1.setEndIconOnClickListener {
            if (binding.emailLayout1.error?.contains("network", ignoreCase = true) == true ||
                binding.emailLayout1.error?.contains("connection", ignoreCase = true) == true) {
                binding.emailLayout1.error = null
                // Retry the sign in process
                signInUser()
            }
        }
    }

    private fun signInUser() {
        Log.d("SignInFragment", "=== SIGN IN USER FUNCTION CALLED ===")
        val email = binding.emailEditText1.text?.toString()?.trim() ?: ""
        val password = binding.passwordEditTextSignIn.text?.toString()?.trim() ?: ""
        Log.d("SignInFragment", "Email: $email, Password length: ${password.length}")
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
        
        // Check network connectivity first
        Log.d("SignInFragment", "Checking network connectivity...")
        val networkAvailable = isNetworkAvailable()
        Log.d("SignInFragment", "Network available: $networkAvailable")
        
        if (!networkAvailable) {
            binding.emailLayout1.error = "No internet connection. Please check your network."
            binding.signInButton.isEnabled = true
            return
        }
        
        // Check Firebase configuration
        Log.d("SignInFragment", "Checking Firebase configuration...")
        val authConfigured = FirebaseUtils.isFirebaseAuthConfigured()
        val firestoreConfigured = FirebaseUtils.isFirestoreConfigured()
        Log.d("SignInFragment", "Firebase Auth configured: $authConfigured, Firestore configured: $firestoreConfigured")
        
        if (!authConfigured || !firestoreConfigured) {
            binding.emailLayout1.error = "Authentication service not configured. Please contact support."
            binding.signInButton.isEnabled = true
            return
        }
        
        // Show loading state
        binding.signInButton.text = "Signing in..."
        
        // Try direct Firebase Auth first (simpler approach)
        Log.d("SignInFragment", "Attempting direct Firebase Auth...")
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                handleAuthResult(task, email, password)
            }
    }
    
    private fun handleAuthResult(task: com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>, email: String, password: String) {
        binding.signInButton.isEnabled = true
        binding.signInButton.text = "Sign In"
        
        if (task.isSuccessful) {
            val user = auth.currentUser
            Log.d("SignInFragment", "Firebase Auth successful: ${user?.email}")
            Log.d("SignInFragment", "User email verified: ${user?.isEmailVerified}")
            
            if (user != null && user.isEmailVerified) {
                Log.d("SignInFragment", "User is verified, proceeding to home...")
                // Upload device info to Firestore
                uploadDeviceInfoToFirestore(user.uid)
                
                // Save user data to AuthManager and SharedPreferences
                saveUserDataToLocalStorage(user)
                
                // Mark login as seen in AuthFlowManager
                val authFlowManager = com.example.nocturnevpn.utils.AuthFlowManager.getInstance(requireContext())
                authFlowManager.markSuccessfulLogin()
                
                Toast.makeText(context, "Sign in successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(), HomeActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            } else {
                Log.d("SignInFragment", "User email not verified, showing verification message")
                Toast.makeText(context, "Please verify your email before signing in.", Toast.LENGTH_LONG).show()
            }
        } else {
            val exception = task.exception
            Log.e("SignInFragment", "Firebase Auth failed: ${exception?.message}", exception)
            
            if (exception is FirebaseAuthInvalidCredentialsException) {
                binding.passwordLayoutSignIn.error = "Incorrect password."
            } else if (exception is FirebaseAuthInvalidUserException) {
                binding.emailLayout1.error = "User is not registered."
            } else {
                // If direct auth fails, try the complex approach
                Log.d("SignInFragment", "Direct auth failed, trying complex approach...")
                tryComplexSignIn(email, password)
            }
        }
    }
    
    private fun tryComplexSignIn(email: String, password: String) {
        // Use coroutine for better error handling
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Check if user exists in Firestore directly
                val userExistsResult = FirebaseUtils.checkUserExists(email)
                
                if (userExistsResult.isSuccess) {
                    val userExists = userExistsResult.getOrNull() ?: false
                    
                    if (!userExists) {
                        binding.emailLayout1.error = "User is not registered."
                        binding.signInButton.isEnabled = true
                        binding.signInButton.text = "Sign In"
                        return@launch
                    }
                    
                    // User exists, proceed with Firebase Auth sign in
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            handleAuthResult(task, email, password)
                        }
                } else {
                    // Handle Firestore query failure - try Firebase Auth directly as fallback
                    Log.w("SignInFragment", "Firestore query failed, trying Firebase Auth directly")
                    
                    // Try Firebase Auth directly without checking Firestore first
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            handleAuthResult(task, email, password)
                        }
                }
                
            } catch (e: Exception) {
                val errorMessage = FirebaseUtils.getErrorMessage(e)
                binding.emailLayout1.error = errorMessage
                Log.e("SignInFragment", "Sign in process failed: ${e.message}", e)
            } finally {
                binding.signInButton.isEnabled = true
                binding.signInButton.text = "Sign In"
            }
        }
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
        )
    }
    
    private fun saveUserDataToLocalStorage(user: com.google.firebase.auth.FirebaseUser) {
        try {
            Log.d("SignInFragment", "Saving user data to local storage...")
            
            // Save to AuthManager
            val authManager = com.example.nocturnevpn.utils.AuthManager.getInstance(requireContext())
            val userEmail = user.email ?: ""
            val userName = user.displayName ?: "User"
            authManager.saveAuthState(user.uid, userEmail, userName, "email")
            
            // Save to SharedPreferences
            val sharedPreference = SharedPreference(requireContext())
            sharedPreference.saveUserName(userName)
            sharedPreference.setUserSignedIn(true)
            
            Log.d("SignInFragment", "User data saved successfully - Name: $userName, Email: $userEmail")
            
            // Fetch user name from Firestore if display name is not available
            if (user.displayName.isNullOrEmpty()) {
                fetchUserNameFromFirestore(user.uid) { firestoreUserName ->
                    if (firestoreUserName != null && firestoreUserName.isNotEmpty()) {
                        Log.d("SignInFragment", "Fetched user name from Firestore: $firestoreUserName")
                        // Update AuthManager with the fetched name
                        authManager.updateUserName(firestoreUserName)
                        // Update SharedPreferences
                        sharedPreference.saveUserName(firestoreUserName)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e("SignInFragment", "Error saving user data to local storage: ${e.message}", e)
        }
    }
    
    private fun fetchUserNameFromFirestore(userId: String, callback: (String?) -> Unit) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userName = document.getString("name")
                    callback(userName)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("SignInFragment", "Error fetching user name from Firestore: ${exception.message}")
                callback(null)
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
                
                // Mark login as seen in AuthFlowManager
                val authFlowManager = com.example.nocturnevpn.utils.AuthFlowManager.getInstance(requireContext())
                authFlowManager.markSuccessfulLogin()
                
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
                // Mark login as seen in AuthFlowManager
                val authFlowManager = com.example.nocturnevpn.utils.AuthFlowManager.getInstance(requireContext())
                authFlowManager.markSuccessfulLogin()
                
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