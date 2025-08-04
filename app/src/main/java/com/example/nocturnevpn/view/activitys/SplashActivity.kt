package com.example.nocturnevpn.view.activitys

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.nocturnevpn.R
import com.example.nocturnevpn.utils.AuthManager
import com.example.nocturnevpn.utils.AuthFlowManager
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    
    companion object {
        private const val SPLASH_DELAY = 2000L // 2 seconds
        private const val MAX_WAIT_TIME = 5000L // 5 seconds max wait
    }
    
    private lateinit var authManager: AuthManager
    private lateinit var authFlowManager: AuthFlowManager
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var hasNavigated = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        Log.d("SplashActivity", "Splash activity started")
        
        authManager = AuthManager.getInstance(this)
        authFlowManager = AuthFlowManager.getInstance(this)
        
        // Debug auth flow state
        authFlowManager.debugState()
        
        // Set up Firebase auth state listener
        setupAuthStateListener()
        
        // Check authentication state after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthState()
        }, SPLASH_DELAY)
        
        // Fallback - if Firebase takes too long, proceed anyway
        Handler(Looper.getMainLooper()).postDelayed({
            if (!hasNavigated) {
                Log.d("SplashActivity", "Firebase auth taking too long, proceeding with SharedPrefs state")
                checkAuthState()
            }
        }, MAX_WAIT_TIME)
    }
    
    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (!hasNavigated) {
                Log.d("SplashActivity", "Firebase auth state changed: ${firebaseAuth.currentUser != null}")
                checkAuthState()
            }
        }
        authManager.addAuthStateListener(authStateListener!!)
    }
    
    private fun checkAuthState() {
        if (hasNavigated) return
        
        // Wait for Firebase Auth to restore session if needed
        authManager.waitForAuthRestoration { isSignedIn ->
            if (hasNavigated) return@waitForAuthRestoration
            
            Log.d("SplashActivity", "Auth restoration completed - User signed in: $isSignedIn")
            
            // Validate and clean auth state if needed
            if (!isSignedIn) {
                authManager.validateAndCleanAuthState()
            }
            
            hasNavigated = true
            
            // Use AuthFlowManager to determine destination
            val destinationClass = authFlowManager.getDestinationClass(authManager)
            
            Log.d("SplashActivity", "Navigating to: ${destinationClass.simpleName}")
            val intent = Intent(this@SplashActivity, destinationClass)
            startActivity(intent)
            
            // Finish splash activity
            finish()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove auth state listener
        authStateListener?.let { authManager.removeAuthStateListener(it) }
    }
} 