package com.example.nocturnevpn.view.activitys

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.nocturnevpn.databinding.ActivityAppAuthBinding
import com.example.nocturnevpn.utils.SocialAuthHelper

class AppAuthActivity : AppCompatActivity() {

    private lateinit var binding : ActivityAppAuthBinding
    private lateinit var socialAuthHelper: SocialAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        socialAuthHelper = SocialAuthHelper(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("AppAuthActivity", "onActivityResult called: requestCode=$requestCode, resultCode=$resultCode")
        
        // Handle Facebook login result
        socialAuthHelper.handleActivityResult(requestCode, resultCode, data)
    }
}