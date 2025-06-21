package com.example.nocturnevpn.utils

import android.content.Context
import com.example.nocturnevpn.db.HistoryHelper
import com.example.nocturnevpn.model.History
import java.util.Calendar
import java.util.Date

object SampleDataGenerator {
    
    fun generateSampleHistory(context: Context) {
        val historyHelper = HistoryHelper.getInstance(context)
        
        // Clear existing data first
        historyHelper.clearHistory()
        
        val calendar = Calendar.getInstance()
        
        // Generate sample data for the last 7 days
        val sampleServers = listOf(
            "US Server" to "United States",
            "UK Server" to "United Kingdom", 
            "Germany Server" to "Germany",
            "Japan Server" to "Japan",
            "Canada Server" to "Canada",
            "Australia Server" to "Australia",
            "France Server" to "France"
        )
        
        val sampleStatuses = listOf("Connected", "Disconnected", "Failed")
        
        for (i in 0 until 20) {
            // Generate random date within last 7 days
            calendar.add(Calendar.HOUR, -(i * 2))
            val connectionDate = calendar.time
            
            // Generate random duration (5 minutes to 2 hours)
            val duration = (5 + (Math.random() * 115)).toLong() * 60 * 1000
            
            // Generate random data usage (10MB to 500MB)
            val dataUsed = (10 + (Math.random() * 490)).toLong() * 1024 * 1024
            
            val serverIndex = i % sampleServers.size
            val statusIndex = i % sampleStatuses.size
            
            val history = History(
                sampleServers[serverIndex].first,
                sampleServers[serverIndex].second,
                "192.168.1.${100 + i}",
                connectionDate,
                duration,
                sampleStatuses[statusIndex],
                dataUsed
            )
            
            historyHelper.addHistory(history)
        }
    }
} 