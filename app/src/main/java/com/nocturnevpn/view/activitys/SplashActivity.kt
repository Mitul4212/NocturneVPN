package com.nocturnevpn.view.activitys

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.nocturnevpn.R
import com.nocturnevpn.utils.AuthManager
import com.nocturnevpn.utils.AuthFlowManager
import com.google.firebase.auth.FirebaseAuth
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import android.net.Uri
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

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

        // Draw edge-to-edge; we'll handle insets in layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Apply top inset to the root to prevent content under status bar on splash
        val root: View = findViewById(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }
        
        Log.d("SplashActivity", "Splash activity started")
        
        authManager = AuthManager.getInstance(this)
        authFlowManager = AuthFlowManager.getInstance(this)
        
        // Debug auth flow state
        authFlowManager.debugState()

        // Find views from layout
        val splashScreen: ConstraintLayout = findViewById(R.id.splash_screen)
        val firstScreen: ConstraintLayout = findViewById(R.id.firstScreen)
        val btnContinueAsGuest: MaterialButton? = findViewById(R.id.btn_yes)
        val btnSignIn: MaterialButton? = findViewById(R.id.btn_no)
        val tvTerms: TextView? = findViewById(R.id.tv_terms_of_service)

        val isSignedIn = authManager.isUserSignedIn()
        val isFirstTime = authFlowManager.isFirstTimeLogin()

        // If first time and not signed in, show the onboarding first screen and skip auto navigation
        if (isFirstTime && !isSignedIn) {
            Log.d("SplashActivity", "First app open detected. Showing first screen onboarding")

            splashScreen.visibility = View.GONE
            firstScreen.visibility = View.VISIBLE

            // Handle actions
            btnContinueAsGuest?.setOnClickListener {
                // Mark that user has seen the onboarding/login once
                authFlowManager.markLoginSeen()
                navigateToHome()
            }

            btnSignIn?.setOnClickListener {
                // User chooses sign in - mark seen so we don't show onboarding again
                authFlowManager.markLoginSeen()
                navigateToAuth()
            }

            tvTerms?.setOnClickListener {
                val termsUrl = "https://mitul4212.github.io/nocturnevpn-legal/terms-of-service.html"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(termsUrl))
                startActivity(browserIntent)
            }

            // Do not set up auth listeners or timers when onboarding is shown
            return
        }

        // Not first time or already signed in -> proceed with normal splash behavior
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

    private fun navigateToHome() {
        if (hasNavigated) return
        hasNavigated = true
        Log.d("SplashActivity", "Navigating to HomeActivity from onboarding")
        startActivity(Intent(this@SplashActivity, Class.forName("com.nocturnevpn.view.activitys.HomeActivity")))
        finish()
    }

    private fun navigateToAuth() {
        if (hasNavigated) return
        hasNavigated = true
        Log.d("SplashActivity", "Navigating to AppAuthActivity from onboarding")
        startActivity(Intent(this@SplashActivity, Class.forName("com.nocturnevpn.view.activitys.AppAuthActivity")))
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove auth state listener
        authStateListener?.let { authManager.removeAuthStateListener(it) }
    }
    
} 