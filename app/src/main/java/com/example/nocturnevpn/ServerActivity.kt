package com.example.nocturnevpn

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.nocturnevpn.databinding.ActivityServerBinding

class ServerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        binding = ActivityServerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backArrow.setOnClickListener{
            finish()
        }
    }
}