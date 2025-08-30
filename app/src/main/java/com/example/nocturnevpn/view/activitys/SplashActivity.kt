package com.example.nocturnevpn.view.activitys

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.nocturnevpn.R
import com.example.nocturnevpn.utils.AuthManager
import com.example.nocturnevpn.utils.AuthFlowManager
import com.google.android.material.button.MaterialButton
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
    
    // UI elements
    private lateinit var splashScreen: View
    private lateinit var firstScreen: View
    private lateinit var btnGuestProfile: MaterialButton
    private lateinit var btnSignIn: MaterialButton
    private lateinit var tvTermsOfService: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        Log.d("SplashActivity", "Splash activity started")
        
        // Initialize UI elements
        initializeViews()
        
        authManager = AuthManager.getInstance(this)
        authFlowManager = AuthFlowManager.getInstance(this)
        
        // Debug auth flow state
        authFlowManager.debugState()
        
        // Check if this is the first time opening the app
        if (authFlowManager.isFirstTimeLogin()) {
            Log.d("SplashActivity", "First time opening app - showing first screen")
            showFirstScreen()
        } else {
            Log.d("SplashActivity", "Not first time - showing splash screen")
            showSplashScreen()
        }
    }
    
    private fun initializeViews() {
        splashScreen = findViewById(R.id.splash_screen)
        firstScreen = findViewById(R.id.firstScreen)
        btnGuestProfile = findViewById(R.id.btn_yes)
        btnSignIn = findViewById(R.id.btn_no)
        
        // Find the Terms of Service text view
        tvTermsOfService = findViewById(R.id.tv_terms_of_service)
        
        // Set up button click listeners
        btnGuestProfile.setOnClickListener {
            Log.d("SplashActivity", "Guest profile selected")
            handleGuestProfileSelection()
        }
        
        btnSignIn.setOnClickListener {
            Log.d("SplashActivity", "Sign in selected")
            handleSignInSelection()
        }
        
        // Set up Terms of Service click listener
        tvTermsOfService.setOnClickListener {
            Log.d("SplashActivity", "Terms of Service clicked")
            openTermsOfService()
        }
    }
    
    private fun showFirstScreen() {
        splashScreen.visibility = View.GONE
        firstScreen.visibility = View.VISIBLE
    }
    
    private fun showSplashScreen() {
        splashScreen.visibility = View.VISIBLE
        firstScreen.visibility = View.GONE
        
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
    
    private fun handleGuestProfileSelection() {
        Log.d("SplashActivity", "User selected guest profile")
        
        // Mark that user has seen the login page (so they won't see it again)
        authFlowManager.markLoginSeen()
        
        // Navigate to HomeActivity for guest users
        val intent = Intent(this@SplashActivity, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun handleSignInSelection() {
        Log.d("SplashActivity", "User selected sign in")
        
        // Mark that user has seen the login page
        authFlowManager.markLoginSeen()
        
        // Navigate to AppAuthActivity for sign in
        val intent = Intent(this@SplashActivity, AppAuthActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun openTermsOfService() {
        val uri = Uri.parse("https://mitul4212.github.io/nocturnevpn-legal/terms-of-service.html")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
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