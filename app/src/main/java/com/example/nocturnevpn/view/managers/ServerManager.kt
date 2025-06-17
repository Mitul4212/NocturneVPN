package com.example.nocturnevpn.view.managers

import android.content.Context
import android.util.Log
import com.example.nocturnevpn.R
import com.example.nocturnevpn.SharedPreference
import com.example.nocturnevpn.databinding.FragmentHomeBinding
import com.example.nocturnevpn.model.Server
import com.murgupluoglu.flagkit.FlagKit

class ServerManager(
    private val context: Context,
    private val binding: FragmentHomeBinding?,
    private val sharedPreference: SharedPreference
) {
    private var isServerSelected: Boolean = false

    fun isServerSelected(): Boolean = isServerSelected

    fun updateServer(server: Server) {
        // Save to SharedPreferences
        sharedPreference.saveServer(server)
        
        // Update UI with new server
        updateServerUI(server)
        
        Log.d("ServerRetrieve", "Selected Server IP: ${server.ipAddress}")
        isServerSelected = true
    }

    fun loadSavedServer() {
        if (sharedPreference.isPrefsHasServer()) {
            val selectedServer = sharedPreference.getServer()
            Log.d("ServerRetrieve", "Retrieved server IP: ${selectedServer?.ipAddress}")
            updateServerUI(selectedServer)
        } else {
            updateServerUI(null)
        }
    }

    fun saveCurrentServer() {
        val selectedServer = sharedPreference.getServer()
        if (selectedServer != null) {
            sharedPreference.saveServer(selectedServer)
        }
    }

    private fun updateServerUI(server: Server?) {
        if (server != null) {
            // Update the UI elements with the server details
            binding?.serverFlagName?.text = server.getCountryLong()
            binding?.serverFlagDes?.text = server.getIpAddress()
            binding?.connectionIp?.text = server.getIpAddress()

            // Show country flag using FlagKit
            val countryCode = server.getCountryShort()?.lowercase() ?: ""
            val flagResId = FlagKit.getResId(context, countryCode)
            if (flagResId != 0) {
                binding?.countryFlag?.setImageResource(flagResId)
            } else {
                binding?.countryFlag?.setImageResource(R.drawable.ic_server_flage_icon)
            }

            isServerSelected = true
        } else {
            // Handle the case when the server is null
            binding?.serverFlagName?.text = context.getString(R.string.country_name)
            binding?.serverFlagDes?.text = context.getString(R.string.IP_address)
            binding?.connectionIp?.text = context.getString(R.string.IP_address)
            binding?.countryFlag?.setImageResource(R.drawable.ic_server_flage_icon)
            isServerSelected = false
        }
    }
} 