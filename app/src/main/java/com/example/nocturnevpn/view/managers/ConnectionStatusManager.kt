package com.example.nocturnevpn.view.managers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
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
    private var connectionStartTime: Long = 0
    private var isConnected = false

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
        
        // Update connect button UI based on connection status
        updateConnectButtonUI(status)
    }

    fun animateButtonClick() {
        if (isAnimating) return
        
        binding?.buttonAnimation?.let { lottieView ->
            // Simple scale animation for immediate feedback
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
            
            lottieView.startAnimation(clickAnimation)
            Log.d("ConnectionStatusManager", "Button click animation played")
        }
    }
    
    private fun updateConnectButtonUI(status: String) {
        // Stop any current animation first
        stopCurrentAnimation()
        
        try {
            when (status) {
                "CONNECTED" -> {
                    // VPN is connected - show success state
                    animateConnectionSuccess()
                }
                "DISCONNECTED", "NOPROCESS", "NONETWORK", "EXITING", "VPN_GENERATE_CONFIG"  -> {
                    // VPN is disconnected - show default state
                    animateDisconnection()
                }
                "WAIT", "AUTH", "RECONNECTING", "TCP_CONNECT", "ASSIGN_IP", "GET_CONFIG", "ADD_ROUTES", "AUTH_PENDING"-> {
                    // VPN is connecting - show loading animation
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
        binding?.buttonAnimation?.let { lottieView ->
            lottieView.clearAnimation()
            lottieView.pauseAnimation()
        }
        binding?.loaderAnimation?.let { loaderView ->
            loaderView.clearAnimation()
            loaderView.pauseAnimation()
            loaderView.visibility = View.GONE
        }
        currentAnimation?.cancel()
        currentAnimation = null
        isAnimating = false
    }
    
    // Show loading animation during connection
    private fun animateConnecting() {
        isAnimating = true

        // Hide connect button card
        binding?.connectButtonCard?.visibility = View.GONE

        // Show and play button animation (set to orange, smooth fade-in)
        binding?.buttonAnimation?.let { buttonView ->
            buttonView.visibility = View.VISIBLE
            buttonView.setAnimation(R.raw.button_base)
            buttonView.speed = 1.05f // Smoother, just above normal
            buttonView.alpha = 0f
            buttonView.animate().alpha(1f).setDuration(250).start() // Fade in
            buttonView.setRenderMode(RenderMode.HARDWARE)
            setLottieButtonColor(context.getColor(R.color.orange)) // Set to orange
            buttonView.repeatCount = LottieDrawable.INFINITE
            buttonView.repeatMode = LottieDrawable.RESTART
            buttonView.playAnimation()
        }

        // Show and play loader animation simultaneously (smooth fade-in)
        binding?.loaderAnimation?.let { loaderView ->
            loaderView.visibility = View.VISIBLE
            loaderView.speed = 1.1f // Smoother, just above normal
            loaderView.alpha = 0f
            loaderView.animate().alpha(1f).setDuration(250).start() // Fade in
            loaderView.setRenderMode(RenderMode.HARDWARE)
            loaderView.repeatCount = LottieDrawable.INFINITE
            loaderView.repeatMode = LottieDrawable.RESTART
            loaderView.playAnimation()
        }

        // Show white icon and timer
        binding?.whiteConnectionIcon?.visibility = View.VISIBLE
        binding?.vpnConnectionTime?.visibility = View.VISIBLE
        binding?.vpnConnectionTime?.setTextColor(Color.WHITE)
    }
    
    // Show success animation when connected
    private fun animateConnectionSuccess() {
        isAnimating = true

        // Hide connect button card
        binding?.connectButtonCard?.visibility = View.GONE

        // Show button animation
        binding?.buttonAnimation?.let { buttonView ->
            buttonView.visibility = View.VISIBLE
            buttonView.setAnimation(R.raw.button_base)
            buttonView.speed = 1.0f
            setLottieButtonColor(context.getColor(R.color.strong_violet)) // Set to default
            buttonView.playAnimation()
        }

        // Show white icon and timer
        binding?.whiteConnectionIcon?.visibility = View.VISIBLE
        binding?.vpnConnectionTime?.visibility = View.VISIBLE
        binding?.vpnConnectionTime?.setTextColor(Color.WHITE)

        // Hide loader
        binding?.loaderAnimation?.visibility = View.GONE
    }
    
    // Show disconnect animation
    private fun animateDisconnection() {
        isAnimating = true

        // Show connect button card
        binding?.connectButtonCard?.visibility = View.VISIBLE

        // Hide button animation, white icon, timer, loader
        binding?.buttonAnimation?.visibility = View.GONE
        binding?.whiteConnectionIcon?.visibility = View.GONE
        binding?.vpnConnectionTime?.visibility = View.GONE
        binding?.loaderAnimation?.visibility = View.GONE

        isAnimating = false
    }
    
    private fun setDefaultButtonStyle() {
        // Hide loader and show button animation
        binding?.loaderAnimation?.let { loaderView ->
            loaderView.pauseAnimation()
            loaderView.visibility = View.GONE
        }
        
        binding?.buttonAnimation?.let { buttonView ->
            buttonView.visibility = View.VISIBLE
            buttonView.setAnimation(R.raw.button_base)
            buttonView.speed = 1.0f  // Normal speed for default state
            buttonView.playAnimation()
            
            // Set default text color
            binding.vpnConnectionTime.setTextColor(Color.BLACK)
            
            Log.d("ConnectionStatusManager", "Default style applied")
        }
        
        isAnimating = false
    }

    private fun setLottieButtonColor(color: Int) {
        binding?.buttonAnimation?.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
            LottieValueCallback(android.graphics.PorterDuffColorFilter(color, android.graphics.PorterDuff.Mode.SRC_ATOP))
        )
    }
} 