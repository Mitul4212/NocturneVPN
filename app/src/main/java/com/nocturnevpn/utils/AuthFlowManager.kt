package com.nocturnevpn.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * AuthFlowManager - Manages authentication flow and first-time login logic
 * 
 * This manager handles the logic for showing login page only on first app install.
 * After first login, users go directly to home page even if not signed in.
 */
class AuthFlowManager private constructor(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "auth_flow_prefs"
        private const val KEY_IS_FIRST_TIME_LOGIN = "is_first_time_login"
        private const val KEY_HAS_SEEN_LOGIN = "has_seen_login"
        
        @Volatile
        private var INSTANCE: AuthFlowManager? = null
        
        fun getInstance(context: Context): AuthFlowManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthFlowManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Check if this is the first time the user is opening the app
     */
    fun isFirstTimeLogin(): Boolean {
        return prefs.getBoolean(KEY_IS_FIRST_TIME_LOGIN, true)
    }
    
    /**
     * Check if user has seen the login page at least once
     */
    fun hasSeenLogin(): Boolean {
        return prefs.getBoolean(KEY_HAS_SEEN_LOGIN, false)
    }
    
    /**
     * Mark that user has seen the login page
     */
    fun markLoginSeen() {
        prefs.edit()
            .putBoolean(KEY_HAS_SEEN_LOGIN, true)
            .putBoolean(KEY_IS_FIRST_TIME_LOGIN, false)
            .apply()
        Log.d("AuthFlowManager", "Login page marked as seen")
    }
    
    /**
     * Mark successful login (for future reference)
     */
    fun markSuccessfulLogin() {
        prefs.edit()
            .putBoolean(KEY_HAS_SEEN_LOGIN, true)
            .putBoolean(KEY_IS_FIRST_TIME_LOGIN, false)
            .apply()
        Log.d("AuthFlowManager", "Successful login marked")
    }
    
    /**
     * Reset first-time login flag (for testing or app reset)
     */
    fun resetFirstTimeLogin() {
        prefs.edit()
            .putBoolean(KEY_IS_FIRST_TIME_LOGIN, true)
            .putBoolean(KEY_HAS_SEEN_LOGIN, false)
            .apply()
        Log.d("AuthFlowManager", "First time login flag reset")
    }
    
    /**
     * Determine if user should see login page or go directly to home
     * Updated logic: Show login page only if user is not signed in AND it's first time
     */
    fun shouldShowLoginPage(authManager: AuthManager): Boolean {
        val isSignedIn = authManager.isUserSignedIn()
        val isFirstTime = isFirstTimeLogin()
        
        // Show login page only if user is not signed in AND it's first time
        val shouldShow = !isSignedIn && isFirstTime
        
        Log.d("AuthFlowManager", "Should show login page: $shouldShow (isSignedIn: $isSignedIn, isFirstTime: $isFirstTime, hasSeenLogin: ${hasSeenLogin()})")
        return shouldShow
    }
    
    /**
     * Get the appropriate destination based on auth state and first-time logic
     * Updated logic: Prioritize authentication state over first-time logic
     */
    fun getDestinationClass(authManager: AuthManager): Class<*> {
        val isSignedIn = authManager.isUserSignedIn()
        val shouldShowLogin = shouldShowLoginPage(authManager)
        
        Log.d("AuthFlowManager", "Getting destination - isSignedIn: $isSignedIn, shouldShowLogin: $shouldShowLogin")
        
        return when {
            isSignedIn -> {
                // User is signed in, go to home normally
                Log.d("AuthFlowManager", "User signed in, going to HomeActivity")
                Class.forName("com.nocturnevpn.view.activitys.HomeActivity")
            }
            shouldShowLogin -> {
                // First time user and not signed in, show login page
                Log.d("AuthFlowManager", "First time user and not signed in, going to AppAuthActivity")
                Class.forName("com.nocturnevpn.view.activitys.AppAuthActivity")
            }
            else -> {
                // User has seen login before but not signed in, go to home
                Log.d("AuthFlowManager", "User has seen login before but not signed in, going to HomeActivity")
                Class.forName("com.nocturnevpn.view.activitys.HomeActivity")
            }
        }
    }
    
    /**
     * Debug method to show current state
     */
    fun debugState() {
        Log.d("AuthFlowManager", "=== AUTH FLOW STATE ===")
        Log.d("AuthFlowManager", "isFirstTimeLogin: ${isFirstTimeLogin()}")
        Log.d("AuthFlowManager", "hasSeenLogin: ${hasSeenLogin()}")
        Log.d("AuthFlowManager", "=======================")
    }
    
    /**
     * Clear all auth flow preferences (for testing)
     */
    fun clearAllPreferences() {
        prefs.edit().clear().apply()
        Log.d("AuthFlowManager", "All auth flow preferences cleared")
    }
} 