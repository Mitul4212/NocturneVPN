package com.example.nocturnevpn.view.managers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.BounceInterpolator
import android.view.animation.ScaleAnimation
import androidx.core.content.ContextCompat
import com.example.nocturnevpn.R
import com.example.nocturnevpn.SharedPreference
import com.example.nocturnevpn.databinding.FragmentHomeBinding

class ConnectionStatusManager(
    private val context: Context,
    private val binding: FragmentHomeBinding?,
    private val sharedPreference: SharedPreference
) {
    private var lastUpdateTime: Long = 0
    private var isAnimating = false
    private var currentAnimation: Animation? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastStatus = ""
    private var isButtonPressed = false

    @SuppressLint("SuspiciousIndentation")
    fun updateConnectionStatus(
        duration: String,
        lastPacketReceive: String,
        byteIn: String,
        byteOut: String
    ) {
        Log.d("VPN_Debug", "=== Updating Connection Status ===")
        Log.d("VPN_Debug", "Current values - Duration: $duration, LastPacket: $lastPacketReceive")
        Log.d("VPN_Debug", "Current bytes - In: $byteIn, Out: $byteOut")

        try {
            binding?.vpnConnectionTime?.text = duration

            // Update more frequently (every 100ms) for smoother speed display
            val now = System.currentTimeMillis()
            val timeDiff = now - lastUpdateTime
            if (timeDiff >= 100) { // Update more frequently
                try {
                    // Parse the formatted byte strings to get speeds
                    val downInfo = SpeedCalculator.parseFormattedByteString(byteIn)
                    val upInfo = SpeedCalculator.parseFormattedByteString(byteOut)

                    Log.d("VPN_Debug", "Parsed speeds - Down: ${downInfo.speed.value} ${downInfo.speed.unit}/s, Up: ${upInfo.speed.value} ${upInfo.speed.unit}/s")

                    // Format and display speeds maintaining original units
                    val downSpeedText = SpeedCalculator.formatSpeedWithUnit(downInfo.speed)
                    val upSpeedText = SpeedCalculator.formatSpeedWithUnit(upInfo.speed)

                    Log.d("VPN_Debug", "Formatted speeds - Down: $downSpeedText, Up: $upSpeedText")

                    // Update UI
                    binding?.downloadSpeed?.text = downSpeedText
                    binding?.uploadSpeed?.text = upSpeedText

                    lastUpdateTime = now
                    Log.d("VPN_Debug", "UI updated with new speeds")

                } catch (e: Exception) {
                    Log.e("VPN_Debug", "Error calculating speeds", e)
                    e.printStackTrace()
                }
            } else {
                Log.d("VPN_Debug", "Skipping update - too soon since last update (${timeDiff}ms)")
            }
        } catch (e: Exception) {
            Log.e("VPN_Debug", "Error updating connection status", e)
            e.printStackTrace()
        }
    }

    fun updateVPNStatusText(status: String, wasConnectedOnce: Boolean) {
        // Only update if status actually changed
        if (status == lastStatus) {
            return
        }
        
        Log.d("ConnectionStatusManager", "Status changed from '$lastStatus' to '$status'")
        lastStatus = status
        
        binding?.connectionTextStatus?.text = when (status) {
            "CONNECTED" -> "Connected"
            "DISCONNECTED" -> if (wasConnectedOnce) "Disconnected" else "Not Connected"
            "NOPROCESS" -> "Not Connected"
            "WAIT" -> "Waiting for server connection"
            "AUTH" -> "Authenticating"
            "RECONNECTING" -> "Reconnecting..."
            "NONETWORK" -> "No network"
            else -> status
        }
        
        // Update connect button UI based on connection status with better animations
        updateConnectButtonUI(status)
    }
    
    // Simple and effective button click animation
    fun animateButtonClick() {
        binding?.connectionButtonImage?.let { imageView ->
            // Simple scale down and up animation
            val clickAnimation = ScaleAnimation(
                1.0f, 0.95f, 1.0f, 0.95f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 150
                repeatCount = 1
                repeatMode = Animation.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
            }
            
            imageView.startAnimation(clickAnimation)
            Log.d("ConnectionStatusManager", "Button click animation played")
        }
    }
    
    private fun updateConnectButtonUI(status: String) {
        // Stop any current animation first
        stopCurrentAnimation()
        
        try {
            when (status) {
                "CONNECTED" -> {
                    // VPN is connected - simple success animation
                    animateConnectionSuccess()
                }
                "DISCONNECTED", "NOPROCESS" -> {
                    // VPN is disconnected - simple disconnect animation
                    animateDisconnection()
                }
                "WAIT", "AUTH", "RECONNECTING" -> {
                    // VPN is connecting - simple connecting animation
                    animateConnecting()
                }
                else -> {
                    // Other states - reset to default
                    setDefaultButtonStyle()
                }
            }
        } catch (e: Exception) {
            Log.e("ConnectionStatusManager", "Error updating connect button UI", e)
            isAnimating = false
        }
    }
    
    private fun stopCurrentAnimation() {
        binding?.connectionButtonImage?.let { imageView ->
            imageView.clearAnimation()
            currentAnimation?.cancel()
            currentAnimation = null
        }
        isAnimating = false
    }
    
    // Simple and reliable connecting animation
    private fun animateConnecting() {
        isAnimating = true
        
        binding?.connectionButtonImage?.let { imageView ->
            // Keep original colors - don't change to orange during authentication
            // imageView.setBackgroundColor(Color.parseColor("#FF9500")) // Orange color removed
            // imageView.setColorFilter(Color.WHITE) // White tint removed
            // binding?.vpnConnectionTime?.setTextColor(Color.WHITE) // White text removed
            
            // Simple pulse animation without color change
            val pulseAnimation = ScaleAnimation(
                1.0f, 1.05f, 1.0f, 1.05f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 1000
                repeatCount = Animation.INFINITE
                repeatMode = Animation.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
            }
            
            currentAnimation = pulseAnimation
            imageView.startAnimation(pulseAnimation)
            
            Log.d("ConnectionStatusManager", "Connecting animation started (no color change)")
        }
    }
    
    // Simple and effective success animation
    private fun animateConnectionSuccess() {
        isAnimating = true
        
        binding?.connectionButtonImage?.let { imageView ->
            // Stop any ongoing animations
            stopCurrentAnimation()
            
            // Set connected colors immediately
            imageView.setBackgroundColor(Color.parseColor("#6622CC"))
            imageView.setColorFilter(Color.WHITE)
            binding?.vpnConnectionTime?.setTextColor(Color.WHITE)
            
            // Simple success animation - scale up and down
            val successAnimation = ScaleAnimation(
                1.0f, 1.1f, 1.0f, 1.1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 300
                repeatCount = 2
                repeatMode = Animation.REVERSE
                interpolator = BounceInterpolator()
            }
            
            successAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    Log.d("ConnectionStatusManager", "Success animation started")
                }
                
                override fun onAnimationEnd(animation: Animation?) {
                    Log.d("ConnectionStatusManager", "Success animation completed")
                    isAnimating = false
                }
                
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            
            imageView.startAnimation(successAnimation)
        }
    }
    
    // Simple and reliable disconnect animation
    private fun animateDisconnection() {
        isAnimating = true
        
        binding?.connectionButtonImage?.let { imageView ->
            // Stop any ongoing animations
            stopCurrentAnimation()
            
            // Set disconnected colors immediately
            imageView.setBackgroundColor(Color.WHITE)
            imageView.setColorFilter(Color.parseColor("#6622CC"))
            binding?.vpnConnectionTime?.setTextColor(Color.BLACK)
            
            // Simple disconnect animation - scale down and up
            val disconnectAnimation = ScaleAnimation(
                1.0f, 0.95f, 1.0f, 0.95f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 200
                repeatCount = 1
                repeatMode = Animation.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
            }
            
            disconnectAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    Log.d("ConnectionStatusManager", "Disconnect animation started")
                }
                
                override fun onAnimationEnd(animation: Animation?) {
                    Log.d("ConnectionStatusManager", "Disconnect animation completed")
                    isAnimating = false
                }
                
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            
            imageView.startAnimation(disconnectAnimation)
        }
    }
    
    private fun setConnectedButtonStyle() {
        binding?.connectionButtonImage?.let { imageView ->
            // Stop any ongoing animations
            stopCurrentAnimation()
            
            // Set purple background
            imageView.setBackgroundColor(Color.parseColor("#6622CC"))
            // Set white tint for the icon
            imageView.setColorFilter(Color.WHITE)
            
            Log.d("ConnectionStatusManager", "Connected style applied")
        }
        
        binding?.vpnConnectionTime?.let { textView ->
            // Set white text color
            textView.setTextColor(Color.WHITE)
        }
    }
    
    private fun setDisconnectedButtonStyle() {
        binding?.connectionButtonImage?.let { imageView ->
            // Stop any ongoing animations
            stopCurrentAnimation()
            
            // Set white background
            imageView.setBackgroundColor(Color.WHITE)
            // Set purple tint for the icon
            imageView.setColorFilter(Color.parseColor("#6622CC"))
            
            Log.d("ConnectionStatusManager", "Disconnected style applied")
        }
        
        binding?.vpnConnectionTime?.let { textView ->
            // Set black text color
            textView.setTextColor(Color.BLACK)
        }
    }
    
    private fun setDefaultButtonStyle() {
        binding?.connectionButtonImage?.let { imageView ->
            // Stop any ongoing animations
            stopCurrentAnimation()
            
            // Reset to default background
            imageView.background = ContextCompat.getDrawable(context, R.drawable.connect_button_bnt)
            // Clear color filter
            imageView.clearColorFilter()
            
            Log.d("ConnectionStatusManager", "Default style applied")
        }
        
        binding?.vpnConnectionTime?.let { textView ->
            // Set default text color
            textView.setTextColor(Color.BLACK)
        }
        
        isAnimating = false
    }
} 