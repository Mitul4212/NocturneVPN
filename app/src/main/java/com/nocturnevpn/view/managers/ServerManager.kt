package com.nocturnevpn.view.managers

import android.content.Context
import android.util.Log
import com.nocturnevpn.R
import com.nocturnevpn.SharedPreference
import com.nocturnevpn.databinding.FragmentHomeBinding
import com.nocturnevpn.model.Server
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

            // Show country flag using FlagKit
            val countryCode = server.getCountryShort()?.lowercase() ?: ""
            val flagResId = FlagKit.getResId(context, countryCode)
            if (flagResId != 0) {
                binding?.countryFlag?.setImageResource(flagResId)
            } else {
                binding?.countryFlag?.setImageResource(R.drawable.ic_server_flage_icon)
            }

            // --- FIX: Get latest ping from server list if available ---
            val latestServerList = sharedPreference.loadServerList()
            val matchedServer = latestServerList?.find { it.ipAddress == server.getIpAddress() }
            val pingToShow = matchedServer?.ping ?: server.ping
            val pingValue = parsePing(pingToShow)
            val signalRes = getSignalResId(pingValue)
            binding?.selectedServerPing?.setImageResource(signalRes)
            // --- END FIX ---

            isServerSelected = true
        } else {
            // Handle the case when the server is null
            binding?.serverFlagName?.text = context.getString(R.string.country_name)
            binding?.serverFlagDes?.text = context.getString(R.string.IP_address)
            binding?.countryFlag?.setImageResource(R.drawable.ic_server_flage_icon)
            binding?.selectedServerPing?.setImageResource(R.drawable.ic_signal_no_signal)
            isServerSelected = false
        }
    }

    private fun parsePing(ping: String?): Int {
        if (ping == null) return -1
        return try {
            val digits = ping.replace(Regex("[^0-9]"), "")
            digits.toInt()
        } catch (e: Exception) {
            -1
        }
    }

    private fun getSignalResId(ping: Int): Int {
        return when {
            ping == 0 -> R.drawable.ic_signal_no_signal
            ping in 1..20 -> R.drawable.ic_signal_four
            ping in 21..50 -> R.drawable.ic_signal_three
            ping in 51..100 -> R.drawable.ic_signal_two
            ping in 101..250 -> R.drawable.ic_signal_one
            else -> R.drawable.ic_signal_no_signal
        }
    }
} 