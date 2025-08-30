package com.nocturnevpn.view.managers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.nocturnevpn.utils.ConsentManager

/**
 * BannerAdManager - Handles banner ad operations across fragments
 * 
 * Features:
 * - Centralized banner ad management
 * - Automatic ad loading and error handling
 * - Lifecycle management
 * - Retry mechanisms
 * - Performance monitoring
 */
class BannerAdManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "BannerAdManager"
        private const val AD_RETRY_DELAY = 30000L // 30 seconds (reduced from 60)
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val AD_REFRESH_INTERVAL = 60000L // 60 seconds (reduced from 90)
        private const val AD_LOAD_DELAY = 500L // 500ms (reduced from 2000ms)
        
        @Volatile
        private var INSTANCE: BannerAdManager? = null
        
        fun getInstance(context: Context): BannerAdManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BannerAdManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // AdManager instance
    private val adManager = AdManager.getInstance(context)
    private val consentManager = ConsentManager.getInstance(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Ad refresh runnable
    private val adRefreshRunnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "🔄 Refreshing banner ads...")
            // This will be called by individual banner ads
            mainHandler.postDelayed(this, AD_REFRESH_INTERVAL)
        }
    }
    
    /**
     * Initialize banner ad with proper lifecycle management
     */
    fun initializeBannerAd(
        adView: AdView,
        onAdLoaded: (() -> Unit)? = null,
        onAdFailed: ((String) -> Unit)? = null
    ) {
        Log.d(TAG, "🚀 Initializing banner ad...")
        
        // Set ad listener
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "✅ Banner ad loaded successfully")
                adView.visibility = View.VISIBLE
                onAdLoaded?.invoke()
            }
            
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e(TAG, "❌ Banner ad failed to load: ${loadAdError.message}")
                adView.visibility = View.GONE
                onAdFailed?.invoke(loadAdError.message ?: "Unknown error")
                
                // Handle specific errors with faster retry
                handleAdLoadError(adView, loadAdError)
            }
            
            override fun onAdOpened() {
                Log.d(TAG, "🔓 Banner ad opened")
            }
            
            override fun onAdClosed() {
                Log.d(TAG, "🔒 Banner ad closed")
            }
            
            override fun onAdClicked() {
                Log.d(TAG, "👆 Banner ad clicked")
            }
        }
        
        // Load ad immediately for fastest appearance
        loadBannerAd(adView)
    }
    
    /**
     * Load banner ad with retry mechanism
     */
    private fun loadBannerAd(adView: AdView, retryCount: Int = 0) {
        if (retryCount >= MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "⚠️ Max retry attempts reached for banner ad")
            return
        }
        
        try {
            Log.d(TAG, "🔄 Loading banner ad (attempt ${retryCount + 1})")
            // Create AdRequest immediately for faster loading
            val adRequest = adManager.createAdRequest()
            adView.loadAd(adRequest)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading banner ad: ${e.message}")
            // Retry after shorter delay for faster recovery
            mainHandler.postDelayed({
                loadBannerAd(adView, retryCount + 1)
            }, if (retryCount == 0) 500L else AD_RETRY_DELAY) // 500ms for first retry, then 30 seconds
        }
    }
    
    /**
     * Handle specific ad load errors
     */
    private fun handleAdLoadError(adView: AdView, loadAdError: LoadAdError) {
        when (loadAdError.code) {
            AdRequest.ERROR_CODE_INTERNAL_ERROR -> {
                Log.e(TAG, "🔧 Internal error - retrying...")
                retryBannerAd(adView)
            }
            AdRequest.ERROR_CODE_INVALID_REQUEST -> {
                Log.e(TAG, "🔧 Invalid request - check ad unit ID")
            }
            AdRequest.ERROR_CODE_NETWORK_ERROR -> {
                Log.e(TAG, "🌐 Network error - retrying...")
                retryBannerAd(adView)
            }
            AdRequest.ERROR_CODE_NO_FILL -> {
                Log.w(TAG, "📭 No fill - no ads available")
            }
            else -> {
                Log.e(TAG, "❌ Unknown error - retrying...")
                retryBannerAd(adView)
            }
        }
    }
    
    /**
     * Retry banner ad loading
     */
    private fun retryBannerAd(adView: AdView) {
        mainHandler.postDelayed({
            loadBannerAd(adView)
        }, 1000L) // 1 second delay for faster retry
    }
    
    /**
     * Refresh banner ad
     */
    fun refreshBannerAd(adView: AdView) {
        Log.d(TAG, "🔄 Refreshing banner ad...")
        loadBannerAd(adView)
    }
    
    /**
     * Pause banner ad
     */
    fun pauseBannerAd(adView: AdView) {
        try {
            adView.pause()
            Log.d(TAG, "⏸️ Banner ad paused")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error pausing banner ad: ${e.message}")
        }
    }
    
    /**
     * Resume banner ad
     */
    fun resumeBannerAd(adView: AdView) {
        try {
            adView.resume()
            Log.d(TAG, "▶️ Banner ad resumed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error resuming banner ad: ${e.message}")
        }
    }
    
    /**
     * Destroy banner ad
     */
    fun destroyBannerAd(adView: AdView) {
        try {
            adView.destroy()
            Log.d(TAG, "🗑️ Banner ad destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error destroying banner ad: ${e.message}")
        }
    }
    
    /**
     * Check if banner ad is loaded
     */
    fun isBannerAdLoaded(adView: AdView): Boolean {
        return adView.adSize != null && adView.visibility == View.VISIBLE
    }
    
    /**
     * Hide banner ad
     */
    fun hideBannerAd(adView: AdView) {
        adView.visibility = View.GONE
        Log.d(TAG, "👻 Banner ad hidden")
    }
    
    /**
     * Show banner ad
     */
    fun showBannerAd(adView: AdView) {
        adView.visibility = View.VISIBLE
        Log.d(TAG, "👁️ Banner ad shown")
    }
    
    /**
     * Start ad refresh cycle
     */
    fun startAdRefreshCycle() {
        mainHandler.postDelayed(adRefreshRunnable, AD_REFRESH_INTERVAL)
        Log.d(TAG, "🔄 Started ad refresh cycle")
    }
    
    /**
     * Stop ad refresh cycle
     */
    fun stopAdRefreshCycle() {
        mainHandler.removeCallbacks(adRefreshRunnable)
        Log.d(TAG, "⏹️ Stopped ad refresh cycle")
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stopAdRefreshCycle()
        Log.d(TAG, "🧹 BannerAdManager cleaned up")
    }
}
