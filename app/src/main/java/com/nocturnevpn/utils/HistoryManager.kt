package com.nocturnevpn.utils

import android.content.Context
import com.nocturnevpn.db.HistoryHelper
import com.nocturnevpn.model.History
import com.nocturnevpn.model.Server
import java.util.Date

class HistoryManager private constructor(private val context: Context) {
    
    private val historyHelper = HistoryHelper.getInstance(context)
    private var connectionStartTime: Long = 0
    private var currentServer: Server? = null
    private var isConnected = false
    private var isConnecting = false

    companion object {
        @Volatile
        private var INSTANCE: HistoryManager? = null

        fun getInstance(context: Context): HistoryManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HistoryManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun onConnectionStarted(server: Server) {
        connectionStartTime = System.currentTimeMillis()
        currentServer = server
        isConnecting = true
        isConnected = false
    }

    fun onConnectionEstablished() {
        isConnecting = false
        isConnected = true
    }

    fun onConnectionStopped() {
        if (currentServer != null) {
            val duration = System.currentTimeMillis() - connectionStartTime
            val status = when {
                isConnected -> "Connected"
                isConnecting -> "Disconnected"
                else -> "Disconnected"
            }
            
            val history = History(
                currentServer!!.hostName,
                currentServer!!.countryLong,
                currentServer!!.ipAddress,
                Date(connectionStartTime),
                duration,
                status,
                0 // You can implement data tracking if needed
            )
            historyHelper.addHistory(history)
        }
        resetConnection()
    }

    fun onConnectionFailed(server: Server, errorMessage: String = "Failed") {
        val duration = System.currentTimeMillis() - connectionStartTime
        val history = History(
            server.hostName,
            server.countryLong,
            server.ipAddress,
            Date(connectionStartTime),
            duration,
            errorMessage,
            0
        )
        historyHelper.addHistory(history)
        resetConnection()
    }

    fun onConnectionDisconnected() {
        if (currentServer != null) {
            val duration = System.currentTimeMillis() - connectionStartTime
            val status = if (isConnected) "Disconnected" else "Failed"
            
            val history = History(
                currentServer!!.hostName,
                currentServer!!.countryLong,
                currentServer!!.ipAddress,
                Date(connectionStartTime),
                duration,
                status,
                0
            )
            historyHelper.addHistory(history)
        }
        resetConnection()
    }

    private fun resetConnection() {
        connectionStartTime = 0
        currentServer = null
        isConnected = false
        isConnecting = false
    }

    fun getRecentHistory(limit: Int): List<History> {
        return historyHelper.getRecentHistory(limit)
    }

    fun getAllHistory(): List<History> {
        return historyHelper.getAllHistory()
    }

    fun clearHistory() {
        historyHelper.clearHistory()
    }
} 