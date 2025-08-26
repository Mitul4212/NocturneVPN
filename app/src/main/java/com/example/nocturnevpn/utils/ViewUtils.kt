package com.example.nocturnevpn.utils

import android.content.Context
import android.widget.Toast
import android.provider.Settings
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Context.getUserFriendlyDeviceId(): String {
    val prefs = getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
    var deviceId = prefs.getString("user_friendly_device_id", null)
    if (deviceId != null) return deviceId

    val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
    val model = Build.MODEL ?: "MODEL"
    // Generate a short hash (first 4 + last 4 of androidId + model hash)
    val shortId = (androidId.take(4) + androidId.takeLast(4) + model.hashCode().toString().takeLast(4)).uppercase()
    deviceId = "NOCT-$shortId"
    prefs.edit().putString("user_friendly_device_id", deviceId).apply()
    return deviceId
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val date = Date(timestamp)
    return sdf.format(date)
}