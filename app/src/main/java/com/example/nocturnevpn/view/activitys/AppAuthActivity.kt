package com.example.nocturnevpn.view.activitys

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.nocturnevpn.databinding.ActivityAppAuthBinding
import com.example.nocturnevpn.utils.SocialAuthHelper
import com.example.nocturnevpn.utils.AuthFlowManager

class AppAuthActivity : AppCompatActivity() {

    private lateinit var binding : ActivityAppAuthBinding
    private lateinit var socialAuthHelper: SocialAuthHelper
    private lateinit var authFlowManager: AuthFlowManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
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