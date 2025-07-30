package com.example.nocturnevpn.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthManager private constructor(context: Context) {
    
    companion object {
        private const val PREF_NAME = "auth_preferences"
        private const val KEY_IS_USER_SIGNED_IN = "is_user_signed_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
        
        @Volatile
        private var INSTANCE: AuthManager? = null
        
        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    /**
     * Check if user is currently signed in
     */
    fun isUserSignedIn(): Boolean {
        val isSignedIn = sharedPreferences.getBoolean(KEY_IS_USER_SIGNED_IN, false)
        val firebaseUser = auth.currentUser
        
        Log.d("AuthManager", "Checking auth state - SharedPrefs: $isSignedIn, FirebaseUser: ${firebaseUser != null}")
        
        // If SharedPreferences says signed in but Firebase user is null, 
        // this indicates a state mismatch - clear the invalid state
        if (isSignedIn && firebaseUser == null) {
            Log.d("AuthManager", "SharedPrefs says signed in but Firebase user is null - clearing invalid state")
            clearAuthState()
            return false
        }
        
        // If Firebase user exists but SharedPreferences says not signed in, update state
        if (!isSignedIn && firebaseUser != null) {
            Log.d("AuthManager", "Firebase user exists but SharedPreferences says not signed in - updating state")
            saveAuthState(firebaseUser.uid, firebaseUser.email ?: "", firebaseUser.displayName ?: "", "firebase")
            return true
        }
        
        // If both say signed in, return true
        if (isSignedIn && firebaseUser != null) {
            Log.d("AuthManager", "Both SharedPrefs and Firebase confirm user is signed in")
            return true
        }
        
        Log.d("AuthManager", "User is not signed in")
        return false
    }
    
    /**
     * Save authentication state after successful sign in
     */
    fun saveAuthState(userId: String, email: String, name: String, provider: String) {
        Log.d("AuthManager", "Saving auth state for user: $email")
        try {
            val success = sharedPreferences.edit().apply {
                putBoolean(KEY_IS_USER_SIGNED_IN, true)
                putString(KEY_USER_ID, userId)
                putString(KEY_USER_EMAIL, email)
                putString(KEY_USER_NAME, name)
                putString(KEY_AUTH_PROVIDER, provider)
            }.commit() // Use commit() instead of apply() for immediate save
            
            if (success) {
                Log.d("AuthManager", "Auth state committed successfully")
            } else {
                Log.e("AuthManager", "Failed to commit auth state")
            }
            
            // Verify the save was successful
            val savedState = sharedPreferences.getBoolean(KEY_IS_USER_SIGNED_IN, false)
            val savedUserId = sharedPreferences.getString(KEY_USER_ID, null)
            Log.d("AuthManager", "Auth state saved successfully - isSignedIn: $savedState, userId: $savedUserId")
        } catch (e: Exception) {
            Log.e("AuthManager", "Error saving auth state: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Clear authentication state after sign out
     */
    fun clearAuthState() {
        Log.d("AuthManager", "Clearing auth state")
        sharedPreferences.edit().apply {
            putBoolean(KEY_IS_USER_SIGNED_IN, false)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            remove(KEY_AUTH_PROVIDER)
            apply()
        }
    }
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        // First try to get from SharedPreferences (fast)
        val cachedUserId = sharedPreferences.getString(KEY_USER_ID, null)
        if (cachedUserId != null && cachedUserId.isNotEmpty()) {
            // Verify that Firebase Auth also has a user (state consistency)
            val firebaseUser = auth.currentUser
            if (firebaseUser != null && firebaseUser.uid == cachedUserId) {
                return cachedUserId
            } else if (firebaseUser == null) {
                // Firebase user is null but we have cached ID - clear invalid state
                Log.d("AuthManager", "Firebase user is null but cached ID exists - clearing invalid state")
                clearAuthState()
                return null
            }
        }
        
        // If not in SharedPreferences, try to get from Firebase Auth
        val firebaseUser = auth.currentUser
        if (firebaseUser != null && firebaseUser.uid.isNotEmpty()) {
            Log.d("AuthManager", "Getting user ID from Firebase Auth: ${firebaseUser.uid}")
            // Update SharedPreferences with the Firebase user ID
            sharedPreferences.edit().putString(KEY_USER_ID, firebaseUser.uid).apply()
            return firebaseUser.uid
        }
        
        // If still not found, return null
        Log.d("AuthManager", "No user ID found in SharedPreferences or Firebase Auth")
        return null
    }
    
    /**
     * Get current user email
     */
    fun getCurrentUserEmail(): String? {
        // First try to get from SharedPreferences (fast)
        val cachedEmail = sharedPreferences.getString(KEY_USER_EMAIL, null)
        if (cachedEmail != null && cachedEmail.isNotEmpty()) {
            return cachedEmail
        }
        
        // If not in SharedPreferences, try to get from Firebase Auth
        val firebaseUser = auth.currentUser
        if (firebaseUser != null && firebaseUser.email != null && firebaseUser.email!!.isNotEmpty()) {
            return firebaseUser.email
        }
        
        // If still not found, return null (will be fetched from Firestore by the calling code)
        return null
    }
    
    /**
     * Fetch user email from Firestore database
     */
    fun fetchUserEmailFromFirestore(callback: (String?) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.d("AuthManager", "No user ID found, cannot fetch email from Firestore")
            callback(null)
            return
        }
        
        Log.d("AuthManager", "Fetching user email from Firestore for user: $userId")
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userEmail = document.getString("email")
                    Log.d("AuthManager", "Fetched user email from Firestore: $userEmail")
                    
                    // Update SharedPreferences with the fetched email
                    if (userEmail != null && userEmail.isNotEmpty()) {
                        sharedPreferences.edit().putString(KEY_USER_EMAIL, userEmail).apply()
                    }
                    
                    callback(userEmail)
                } else {
                    Log.d("AuthManager", "No user document found in Firestore")
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("AuthManager", "Error fetching user email from Firestore: ${exception.message}")
                callback(null)
            }
    }
    
    /**
     * Get current user name
     */
    fun getCurrentUserName(): String? {
        // First try to get from SharedPreferences (fast)
        val cachedName = sharedPreferences.getString(KEY_USER_NAME, null)
        if (cachedName != null && cachedName.isNotEmpty()) {
            return cachedName
        }
        
        // If not in SharedPreferences, try to get from Firebase Auth
        val firebaseUser = auth.currentUser
        if (firebaseUser != null && firebaseUser.displayName != null && firebaseUser.displayName!!.isNotEmpty()) {
            return firebaseUser.displayName
        }
        
        // If still not found, return null (will be fetched from Firestore by the calling code)
        return null
    }
    
    /**
     * Fetch user name from Firestore database
     */
    fun fetchUserNameFromFirestore(callback: (String?) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.d("AuthManager", "No user ID found, cannot fetch from Firestore")
            callback(null)
            return
        }
        
        Log.d("AuthManager", "Fetching user name from Firestore for user: $userId")
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userName = document.getString("name")
                    Log.d("AuthManager", "Fetched user name from Firestore: $userName")
                    
                    // Update SharedPreferences with the fetched name
                    if (userName != null && userName.isNotEmpty()) {
                        sharedPreferences.edit().putString(KEY_USER_NAME, userName).apply()
                    }
                    
                    callback(userName)
                } else {
                    Log.d("AuthManager", "No user document found in Firestore")
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("AuthManager", "Error fetching user name from Firestore: ${exception.message}")
                callback(null)
            }
    }
    
    /**
     * Get authentication provider
     */
    fun getAuthProvider(): String? {
        return sharedPreferences.getString(KEY_AUTH_PROVIDER, null)
    }
    
    /**
     * Sign out user
     */
    fun signOut() {
        Log.d("AuthManager", "Signing out user")
        auth.signOut()
        clearAuthState()
    }
    
    /**
     * Get Firebase Auth instance
     */
    fun getFirebaseAuth(): FirebaseAuth {
        return auth
    }
    
    /**
     * Get current Firebase user
     */
    fun getCurrentFirebaseUser(): com.google.firebase.auth.FirebaseUser? {
        return auth.currentUser
    }
    
    /**
     * Add auth state listener to handle Firebase auth state changes
     */
    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.addAuthStateListener(listener)
    }
    
    /**
     * Remove auth state listener
     */
    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }
    
    /**
     * Update user name in SharedPreferences (for caching)
     */
    fun updateUserName(newUserName: String) {
        Log.d("AuthManager", "Updating cached user name: $newUserName")
        sharedPreferences.edit().putString(KEY_USER_NAME, newUserName).apply()
    }
    
    /**
     * Force refresh authentication state
     */
    fun refreshAuthState(): Boolean {
        Log.d("AuthManager", "Refreshing authentication state")
        
        // Check if we have cached user data
        val cachedUserId = sharedPreferences.getString(KEY_USER_ID, null)
        val cachedEmail = sharedPreferences.getString(KEY_USER_EMAIL, null)
        
        if (cachedUserId != null && cachedEmail != null) {
            Log.d("AuthManager", "Found cached user data - attempting to restore session")
            
            // Try to get current Firebase user
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                Log.d("AuthManager", "Firebase user exists - updating state")
                saveAuthState(firebaseUser.uid, firebaseUser.email ?: "", firebaseUser.displayName ?: "", "firebase")
                return true
            } else {
                Log.d("AuthManager", "No Firebase user - user needs to sign in again")
                clearAuthState()
                return false
            }
        }
        
        return false
    }
} 