package com.nocturnevpn.view.fragment

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nocturnevpn.databinding.FragmentEditUserInfoBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.nocturnevpn.utils.AuthManager
import android.util.Log

class EditUserInfoFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentEditUserInfoBinding? = null
    private val binding get() = _binding!!

    private var currentUserName: String = ""
    private var currentUserEmail: String = ""
    private var isUserVerified = false // You can set this based on your user data
    
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var authManager: AuthManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditUserInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize AuthManager
        authManager = AuthManager.getInstance(requireContext())

        // Debug: Check authentication state
        Log.d("EditUserInfo", "=== onViewCreated ===")
        Log.d("EditUserInfo", "AuthManager isUserSignedIn: ${authManager.isUserSignedIn()}")
        Log.d("EditUserInfo", "AuthManager getCurrentUserId: ${authManager.getCurrentUserId()}")
        Log.d("EditUserInfo", "AuthManager getCurrentFirebaseUser: ${authManager.getCurrentFirebaseUser()?.uid}")

        // Get user data from arguments
        arguments?.let { args ->
            currentUserName = args.getString("user_name", "")
            currentUserEmail = args.getString("user_email", "")
        }

        setupUI()
        setupClickListeners()
        checkInitialVerificationStatus()
        // No need for custom animation, BottomSheetDialogFragment handles it
    }

    private fun setupUI() {
        // Set current user data
        binding.userNameEditText.setText(currentUserName)
        binding.emailTextView.text = currentUserEmail
    }

    private fun setupClickListeners() {
        // Cancel button
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        // Save button
        binding.saveButton.setOnClickListener {
            saveUserInfo()
        }

        // Verify button
        binding.verifyButton.setOnClickListener {
            Log.d("EditUserInfo", "=== Verify button clicked ===")
            verifyEmail()
        }
    }

    private fun verifyEmail() {
        Log.d("EditUserInfo", "=== Starting verifyEmail process ===")
        
        // Check if user is signed in first
        val isUserSignedIn = authManager.isUserSignedIn()
        Log.d("EditUserInfo", "User signed in status: $isUserSignedIn")
        
        // Try to get Firebase user directly from Firebase Auth
        val directFirebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        Log.d("EditUserInfo", "Direct Firebase Auth user: ${directFirebaseUser?.uid}")
        
        // Get current Firebase user from AuthManager
        val firebaseUser = authManager.getCurrentFirebaseUser()
        if (firebaseUser == null) {
            Log.e("EditUserInfo", "Firebase user is null - attempting to refresh auth state")
            
            // Try to refresh authentication state
            val refreshSuccess = authManager.refreshAuthState()
            if (refreshSuccess) {
                Log.d("EditUserInfo", "Auth state refreshed successfully")
                val refreshedUser = authManager.getCurrentFirebaseUser()
                if (refreshedUser != null) {
                    Log.d("EditUserInfo", "Firebase user available after refresh: ${refreshedUser.uid}")
                    proceedWithVerification(refreshedUser)
                } else {
                    Log.e("EditUserInfo", "Firebase user still null after refresh")
                    Toast.makeText(context, "Error: Please sign in again to verify your email", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.e("EditUserInfo", "Failed to refresh auth state - user needs to sign in again")
                Toast.makeText(context, "Error: Please sign in again to verify your email", Toast.LENGTH_LONG).show()
            }
            return
        }

        Log.d("EditUserInfo", "Firebase user found: ${firebaseUser.uid}, email: ${firebaseUser.email}")
        proceedWithVerification(firebaseUser)
    }
    
    private fun proceedWithVerification(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        Log.d("EditUserInfo", "=== proceedWithVerification ===")
        Log.d("EditUserInfo", "Firebase user UID: ${firebaseUser.uid}")
        Log.d("EditUserInfo", "Firebase user email: ${firebaseUser.email}")
        Log.d("EditUserInfo", "Firebase user isEmailVerified: ${firebaseUser.isEmailVerified}")
        
        // First check if user is already verified in database
        val userId = authManager.getCurrentUserId()
        Log.d("EditUserInfo", "User ID from AuthManager: $userId")
        
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val isVerified = document.getBoolean("isVerified") ?: false
                        if (isVerified) {
                            // User is already verified in database
                            isUserVerified = true
                            updateVerificationUI()
                            Toast.makeText(context, "Email is already verified!", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                    }
                    // User is not verified - proceed with sending verification email
                    sendVerificationEmail(firebaseUser)
                }
                .addOnFailureListener { exception ->
                    Log.e("EditUserInfo", "Error checking verification status: ${exception.message}")
                    // On error, check Firebase Auth status as fallback
                    if (firebaseUser.isEmailVerified) {
                        isUserVerified = true
                        updateVerificationUI()
                        Toast.makeText(context, "Email is already verified!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Send verification email
                        sendVerificationEmail(firebaseUser)
                    }
                }
        } else {
            // No user ID - check Firebase Auth status
            if (firebaseUser.isEmailVerified) {
                isUserVerified = true
                updateVerificationUI()
                Toast.makeText(context, "Email is already verified!", Toast.LENGTH_SHORT).show()
            } else {
                // Send verification email
                sendVerificationEmail(firebaseUser)
            }
        }
    }
    
    private fun sendVerificationEmail(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        Log.d("EditUserInfo", "Sending verification email to: ${firebaseUser.email}")
        // Show loading state
        binding.verifyButton.isEnabled = false
        binding.verifyButton.text = "Sending..."

        // Send email verification
        firebaseUser.sendEmailVerification()
            .addOnSuccessListener {
                Log.d("EditUserInfo", "Verification email sent successfully")
                Toast.makeText(context, "Verification email sent! Please check your inbox and click the verification link.", Toast.LENGTH_LONG).show()
                
                // Reset button state
                binding.verifyButton.isEnabled = true
                binding.verifyButton.text = "Verify Email"
                
                // Start checking for verification status
                startVerificationCheck()
            }
            .addOnFailureListener { exception ->
                Log.e("EditUserInfo", "Error sending verification email: ${exception.message}")
                Toast.makeText(context, "Error sending verification email: ${exception.message}", Toast.LENGTH_SHORT).show()
                
                // Reset button state
                binding.verifyButton.isEnabled = true
                binding.verifyButton.text = "Verify Email"
            }
    }
    
    private fun startVerificationCheck() {
        // Check verification status every 2 seconds
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                checkEmailVerificationStatus()
                if (!isUserVerified) {
                    handler.postDelayed(this, 2000) // Check again in 2 seconds
                }
            }
        }
        handler.post(runnable)
    }
    
    private fun checkEmailVerificationStatus() {
        val firebaseUser = authManager.getFirebaseAuth().currentUser
        if (firebaseUser != null) {
            // Reload user to get latest verification status
            firebaseUser.reload()
                .addOnSuccessListener {
                    if (firebaseUser.isEmailVerified) {
                        Log.d("EditUserInfo", "Email verified successfully!")
                        isUserVerified = true
                        updateVerificationUI()
                        
                        // Update verification status in Firestore
                        updateVerificationStatusInFirestore(true)
                        
                        Toast.makeText(context, "Email verified successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("EditUserInfo", "Error reloading user: ${exception.message}")
                }
        }
    }
    
    private fun updateVerificationStatusInFirestore(isVerified: Boolean) {
        val userId = authManager.getCurrentUserId()
        if (userId != null) {
            val userData = hashMapOf(
                "isVerified" to isVerified
            )
            
            db.collection("users").document(userId).update(userData as Map<String, Any>)
                .addOnSuccessListener {
                    Log.d("EditUserInfo", "Verification status updated in Firestore")
                }
                .addOnFailureListener { exception ->
                    Log.e("EditUserInfo", "Error updating verification status in Firestore: ${exception.message}")
                }
        }
    }
    
    private fun checkInitialVerificationStatus() {
        val userId = authManager.getCurrentUserId()
        if (userId != null) {
            // First check verification status from Firebase database
            Log.d("EditUserInfo", "Checking verification status from Firestore for user: $userId")
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val isVerified = document.getBoolean("isVerified") ?: false
                        Log.d("EditUserInfo", "Verification status from Firestore: $isVerified")
                        
                        if (isVerified) {
                            // User is verified in database - show tick mark
                            isUserVerified = true
                            updateVerificationUI()
                            Log.d("EditUserInfo", "User is verified - showing tick mark")
                        } else {
                            // User is not verified in database - show verify button
                            isUserVerified = false
                            updateVerificationUI()
                            Log.d("EditUserInfo", "User is not verified - showing verify button")
                        }
                    } else {
                        // No document found - assume not verified
                        Log.d("EditUserInfo", "No user document found in Firestore - assuming not verified")
                        isUserVerified = false
                        updateVerificationUI()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("EditUserInfo", "Error checking verification status from Firestore: ${exception.message}")
                    // On error, fallback to Firebase Auth verification status
                    checkFirebaseAuthVerificationStatus()
                }
        } else {
            Log.d("EditUserInfo", "No user ID found - cannot check verification status")
            updateVerificationUI()
        }
    }
    
    private fun checkFirebaseAuthVerificationStatus() {
        val firebaseUser = authManager.getCurrentFirebaseUser()
        if (firebaseUser != null) {
            // Fallback to Firebase Auth verification status
            if (firebaseUser.isEmailVerified) {
                isUserVerified = true
                updateVerificationUI()
                Log.d("EditUserInfo", "Fallback: Email verified in Firebase Auth")
            } else {
                isUserVerified = false
                updateVerificationUI()
                Log.d("EditUserInfo", "Fallback: Email not verified in Firebase Auth")
            }
        } else {
            isUserVerified = false
            updateVerificationUI()
            Log.d("EditUserInfo", "Fallback: No Firebase user found")
        }
    }

    // No need for custom animation or overlay logic

    private fun saveUserInfo() {
        val newUserName = binding.userNameEditText.text.toString().trim()
        
        if (newUserName.isEmpty()) {
            binding.userNameInputLayout.error = "Name cannot be empty"
            return
        }

        // Show loading state
        binding.saveButton.isEnabled = false
        binding.saveButton.text = "Saving..."

        // Get current user ID
        val userId = authManager.getCurrentUserId()
        if (userId == null) {
            Toast.makeText(context, "Error: User not found", Toast.LENGTH_SHORT).show()
            binding.saveButton.isEnabled = true
            binding.saveButton.text = "Save"
            return
        }

        // Update user name in Firebase database
        val userData = hashMapOf(
            "name" to newUserName
        )

        Log.d("EditUserInfo", "Updating user name in Firestore for user: $userId")
        db.collection("users").document(userId).update(userData as Map<String, Any>)
            .addOnSuccessListener {
                Log.d("EditUserInfo", "User name updated successfully in Firestore")
                
                // Update AuthManager's cached user name
                authManager.updateUserName(newUserName)
                
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                
                // Notify the parent fragment about the update
                val result = Bundle().apply {
                    putString("updated_user_name", newUserName)
                    putString("updated_user_email", currentUserEmail)
                }
                // Send result back to parent fragment using the appropriate fragment manager
                if (parentFragmentManager.fragments.contains(this)) {
                    // If added as child fragment, send to parent
                    parentFragmentManager.setFragmentResult("edit_user_info_result", result)
                } else {
                    // If added via navigation, send to activity
                    requireActivity().supportFragmentManager.setFragmentResult("edit_user_info_result", result)
                }
                dismiss()
            }
            .addOnFailureListener { exception ->
                Log.e("EditUserInfo", "Error updating user name in Firestore: ${exception.message}")
                Toast.makeText(context, "Error updating profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.saveButton.isEnabled = true
                binding.saveButton.text = "Save"
            }
    }

    private fun updateVerificationUI() {
        if (isUserVerified) {
            binding.verifyButton.visibility = View.GONE
            binding.tickIcon.visibility = View.VISIBLE
        } else {
            binding.verifyButton.visibility = View.VISIBLE
            binding.tickIcon.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(userName: String, userEmail: String): EditUserInfoFragment {
            return EditUserInfoFragment().apply {
                arguments = Bundle().apply {
                    putString("user_name", userName)
                    putString("user_email", userEmail)
                }
            }
        }
    }
} 