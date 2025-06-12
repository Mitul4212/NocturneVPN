package com.example.nocturnevpn.view.activitys

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.nocturnevpn.databinding.ActivityAppAuthBinding

class AppAuthActivity : AppCompatActivity() {

    private lateinit var binding : ActivityAppAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}