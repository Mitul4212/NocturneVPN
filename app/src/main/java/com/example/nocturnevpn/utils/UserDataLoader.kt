package com.example.nocturnevpn.utils

import android.content.Context
import android.util.Log
import android.widget.TextView
import com.example.nocturnevpn.SharedPreference

object UserDataLoader {
    
    private const val TAG = "UserDataLoader"
    
    /**
     * Load user name into a TextView with fallback options
     */
    fun loadUserName(context: Context, textView: TextView, authManager: AuthManager, sharedPreference: SharedPreference?) {
        val isUserSignedIn = authManager.isUserSignedIn()
        
        if (isUserSignedIn) {
            // Try to get user name from AuthManager first, then fallback to SharedPreference
            val authUserName = authManager.getCurrentUserName()
            val sharedPrefUserName = sharedPreference?.getUserName()
            
            val userName = authUserName ?: sharedPrefUserName
            if (userName != null && userName.isNotEmpty()) {
                textView.text = userName
                Log.d(TAG, "User name loaded from cache: $userName")
            } else {
                // If no cached name, fetch from Firestore
                textView.text = "Loading..."
                authManager.fetchUserNameFromFirestore { firestoreUserName ->
                    if (firestoreUserName != null && firestoreUserName.isNotEmpty()) {
                        textView.text = firestoreUserName
                        Log.d(TAG, "User name loaded from Firestore: $firestoreUserName")
                    } else {
                        textView.text = "Signed In User"
                        Log.d(TAG, "No user name found, using default")
                    }
                }
            }
        } else {
            // User is not signed in
            textView.text = "Guest User"
            Log.d(TAG, "User not signed in, showing guest user")
        }
    }
    
    /**
     * Load user email into a TextView with fallback options
     */
    fun loadUserEmail(context: Context, textView: TextView, authManager: AuthManager) {
        val isUserSignedIn = authManager.isUserSignedIn()
        
        if (isUserSignedIn) {
            // Try to get user email from AuthManager first
            val authUserEmail = authManager.getCurrentUserEmail()
            
            if (authUserEmail != null && authUserEmail.isNotEmpty()) {
                textView.text = authUserEmail
                Log.d(TAG, "User email loaded from cache: $authUserEmail")
            } else {
                // If no cached email, fetch from Firestore
                textView.text = "Loading..."
                authManager.fetchUserEmailFromFirestore { firestoreUserEmail ->
                    if (firestoreUserEmail != null && firestoreUserEmail.isNotEmpty()) {
                        textView.text = firestoreUserEmail
                        Log.d(TAG, "User email loaded from Firestore: $firestoreUserEmail")
                    } else {
                        textView.text = "No email available"
                        Log.d(TAG, "No user email found, using default")
                    }
                }
            }
        } else {
            // User is not signed in
            textView.text = "Guest User"
            Log.d(TAG, "User not signed in, showing guest user")
        }
    }
} 