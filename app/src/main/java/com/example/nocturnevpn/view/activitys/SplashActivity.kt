package com.example.nocturnevpn.view.activitys

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.nocturnevpn.R
import com.example.nocturnevpn.utils.AuthManager
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    
    companion object {
        private const val SPLASH_DELAY = 2000L // 2 seconds
        private const val MAX_WAIT_TIME = 5000L // 5 seconds max wait
    }
    
    private lateinit var authManager: AuthManager
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var hasNavigated = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        Log.d("SplashActivity", "Splash activity started")
        
        authManager = AuthManager.getInstance(this)
        
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
        
        val isSignedIn = authManager.isUserSignedIn()
        
        Log.d("SplashActivity", "User signed in: $isSignedIn")
        
        hasNavigated = true
        
        if (isSignedIn) {
            // User is signed in, go to Home Activity
            Log.d("SplashActivity", "User is signed in, navigating to HomeActivity")
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        } else {
            // User is not signed in, go to Auth Activity
            Log.d("SplashActivity", "User is not signed in, navigating to AppAuthActivity")
            val intent = Intent(this, AppAuthActivity::class.java)
            startActivity(intent)
        }
        
        // Finish splash activity
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove auth state listener
        authStateListener?.let { authManager.removeAuthStateListener(it) }
    }
} 