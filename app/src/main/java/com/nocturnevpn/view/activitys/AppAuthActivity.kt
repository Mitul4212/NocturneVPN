package com.nocturnevpn.view.activitys

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.nocturnevpn.databinding.ActivityAppAuthBinding
import com.nocturnevpn.utils.SocialAuthHelper
import com.nocturnevpn.utils.AuthFlowManager
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class AppAuthActivity : AppCompatActivity() {

    private lateinit var binding : ActivityAppAuthBinding
    private lateinit var socialAuthHelper: SocialAuthHelper
    private lateinit var authFlowManager: AuthFlowManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge; handle insets manually
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityAppAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply top/bottom insets to avoid overlap with system bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }
        
        socialAuthHelper = SocialAuthHelper(this)
        authFlowManager = AuthFlowManager.getInstance(this)
        
        // Mark that user has seen the login page
        authFlowManager.markLoginSeen()
        
        Log.d("AppAuthActivity", "Login page shown - marked as seen")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("AppAuthActivity", "onActivityResult called: requestCode=$requestCode, resultCode=$resultCode")
        
        // Handle Facebook login result
        socialAuthHelper.handleActivityResult(requestCode, resultCode, data)
    }
}