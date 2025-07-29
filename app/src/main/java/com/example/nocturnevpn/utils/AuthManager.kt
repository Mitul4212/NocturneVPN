package com.example.nocturnevpn.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

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
    
    /**
     * Check if user is currently signed in
     */
    fun isUserSignedIn(): Boolean {
        val isSignedIn = sharedPreferences.getBoolean(KEY_IS_USER_SIGNED_IN, false)
        val firebaseUser = auth.currentUser
        
        Log.d("AuthManager", "Checking auth state - SharedPrefs: $isSignedIn, FirebaseUser: ${firebaseUser != null}")
        
        // If SharedPreferences says signed in but Firebase user is null, 
        // wait a bit for Firebase to initialize and try again
        if (isSignedIn && firebaseUser == null) {
            Log.d("AuthManager", "SharedPrefs says signed in but Firebase user is null - waiting for Firebase to initialize")
            // Return true if SharedPreferences says signed in, Firebase will catch up
            return true
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
        
        // If SharedPreferences says signed in, trust it (Firebase might not be ready yet)
        if (isSignedIn) {
            Log.d("AuthManager", "Trusting SharedPrefs state - user is signed in")
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
        sharedPreferences.edit().apply {
            putBoolean(KEY_IS_USER_SIGNED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_AUTH_PROVIDER, provider)
            apply()
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
        return sharedPreferences.getString(KEY_USER_ID, null)
    }
    
    /**
     * Get current user email
     */
    fun getCurrentUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }
    
    /**
     * Get current user name
     */
    fun getCurrentUserName(): String? {
        return sharedPreferences.getString(KEY_USER_NAME, null)
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
} 