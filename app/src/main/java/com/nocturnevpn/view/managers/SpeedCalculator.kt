package com.nocturnevpn.view.managers

import android.util.Log

data class SpeedValue(
    val value: Double,
    val unit: String
)

data class ByteInfo(
    val total: SpeedValue,
    val speed: SpeedValue
)

object SpeedCalculator {
    
    fun parseFormattedByteString(formattedString: String): ByteInfo {
        try {
            // Example input: "↓854.4 kB - 1.7 kB/s"
            // We want to extract both total (854.4 kB) and speed (1.7 kB/s)
            val parts = formattedString.split("-")
            if (parts.size >= 2) {
                // Parse total bytes (first part)
                val totalPart = parts[0].trim()
                val totalRegex = "([↓↑]?)([0-9.]+)\\s*([KMG]?B)".toRegex(RegexOption.IGNORE_CASE)
                val totalMatch = totalRegex.find(totalPart)
                
                // Parse speed (second part)
                val speedPart = parts[1].trim()
                val speedRegex = "([0-9.]+)\\s*([KMG]?B)/s".toRegex(RegexOption.IGNORE_CASE)
                val speedMatch = speedRegex.find(speedPart)
                
                val total = if (totalMatch != null) {
                    val (_, value, unit) = totalMatch.destructured
                    SpeedValue(value.toDouble(), unit.uppercase())
                } else {
                    SpeedValue(0.0, "B")
                }
                
                val speed = if (speedMatch != null) {
                    val (value, unit) = speedMatch.destructured
                    SpeedValue(value.toDouble(), unit.uppercase())
                } else {
                    SpeedValue(0.0, "B")
                }
                
                Log.d("VPN_Debug", "Parsed bytes - Total: ${total.value} ${total.unit}, Speed: ${speed.value} ${speed.unit}/s from $formattedString")
                return ByteInfo(total, speed)
            } else {
                Log.e("VPN_Debug", "Invalid format: $formattedString - expected format: '↓854.4 kB - 1.7 kB/s'")
            }
        } catch (e: Exception) {
            Log.e("VPN_Debug", "Error parsing formatted byte string: $formattedString", e)
        }
        return ByteInfo(SpeedValue(0.0, "B"), SpeedValue(0.0, "B"))
    }

    fun formatSpeedWithUnit(speed: SpeedValue): String {
        // Keep original unit and value, just format the number
        return when (speed.unit) {
            "KB" -> String.format("%.1f KB/s", speed.value)
            "MB" -> String.format("%.1f MB/s", speed.value)
            "GB" -> String.format("%.1f GB/s", speed.value)
            else -> String.format("%.1f B/s", speed.value)
        }
    }

    fun calculateSpeed(currentBytes: Long, previousBytes: Long, timeDiff: Long): SpeedValue {
        if (timeDiff <= 0) return SpeedValue(0.0, "B")
        
        val bytesDiff = currentBytes - previousBytes
        val bytesPerSecond = bytesDiff.toDouble() / (timeDiff / 1000.0)
        
        return when {
            bytesPerSecond >= 1024 * 1024 * 1024 -> SpeedValue(bytesPerSecond / (1024 * 1024 * 1024), "GB")
            bytesPerSecond >= 1024 * 1024 -> SpeedValue(bytesPerSecond / (1024 * 1024), "MB")
            bytesPerSecond >= 1024 -> SpeedValue(bytesPerSecond / 1024, "KB")
            else -> SpeedValue(bytesPerSecond, "B")
        }
    }
} 