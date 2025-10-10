package com.nocturnevpn.view.activitys

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nocturnevpn.databinding.ActivitySocialAuthTestBinding
import com.nocturnevpn.utils.SocialAuthHelper
import com.google.firebase.auth.FirebaseAuth
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class SocialAuthTestActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySocialAuthTestBinding
    private lateinit var socialAuthHelper: SocialAuthHelper
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge; handle insets
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivitySocialAuthTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
        
        socialAuthHelper = SocialAuthHelper(this)
        auth = FirebaseAuth.getInstance()
        
        setupClickListeners()
        updateAuthStatus()
    }
    
    private fun setupClickListeners() {
        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
        
        binding.facebookSignInButton.setOnClickListener {
            signInWithFacebook()
        }
        
        binding.signOutButton.setOnClickListener {
            signOut()
        }
    }
    
    private fun signInWithGoogle() {
        socialAuthHelper.signInWithGoogle(this, object : SocialAuthHelper.AuthCallback {
            override fun onSuccess(userId: String, email: String, name: String) {
                Toast.makeText(this@SocialAuthTestActivity, "Google sign-in successful!", Toast.LENGTH_SHORT).show()
                updateAuthStatus()
            }
            
            override fun onFailure(errorMessage: String) {
                Toast.makeText(this@SocialAuthTestActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
        })
    }
    
    private fun signInWithFacebook() {
        socialAuthHelper.signInWithFacebook(object : SocialAuthHelper.AuthCallback {
            override fun onSuccess(userId: String, email: String, name: String) {
                Toast.makeText(this@SocialAuthTestActivity, "Facebook sign-in successful!", Toast.LENGTH_SHORT).show()
                updateAuthStatus()
            }
            
            override fun onFailure(errorMessage: String) {
                Toast.makeText(this@SocialAuthTestActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
        })
    }
    
    private fun signOut() {
        auth.signOut()
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
        updateAuthStatus()
    }
    
    private fun updateAuthStatus() {
        val user = auth.currentUser
        if (user != null) {
            binding.authStatusText.text = "Signed in as: ${user.displayName ?: user.email}"
            binding.signOutButton.isEnabled = true
            binding.googleSignInButton.isEnabled = false
            binding.facebookSignInButton.isEnabled = false
        } else {
            binding.authStatusText.text = "Not signed in"
            binding.signOutButton.isEnabled = false
            binding.googleSignInButton.isEnabled = true
            binding.facebookSignInButton.isEnabled = true
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        socialAuthHelper.handleActivityResult(requestCode, resultCode, data)
    }
} 