package com.nocturnevpn.view.managers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.*
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.nocturnevpn.utils.ConsentManager

/**
 * AdManager - Centralized ad management for NocturneVPN
 * 
 * Features:
 * - Singleton pattern for global access
 * - Banner, Interstitial, Rewarded, and Native ad support
 * - Automatic ad lifecycle management
 * - Error handling and retry mechanisms
 * - Consent-aware ad loading
 * - Performance monitoring and analytics
 */
class AdManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AdManager"
        
        // Test Ad Unit IDs (replace with real ones for production)
        private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
        
        // Production Ad Unit IDs (to be replaced)
        private const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-6998123470344633/7185705851"
        private const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6998123470344633/2367691113"
        private const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-6998123470344633/8307215835"
        private const val PROD_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
        
        // Ad loading delays and retry settings
        private const val AD_LOAD_DELAY = 500L // 500ms (reduced from 2 seconds)
        private const val AD_RETRY_DELAY = 30000L // 30 seconds (reduced from 1 minute)
        private const val MAX_RETRY_ATTEMPTS = 3
        
        @Volatile
        private var INSTANCE: AdManager? = null
        
        fun getInstance(context: Context): AdManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Ad instances
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var nativeAd: NativeAd? = null
    
    // Ad loading states
    private var isInterstitialAdLoading = false
    private var isRewardedAdLoading = false
    private var isNativeAdLoading = false
    
    // Retry counters
    private var interstitialRetryCount = 0
    private var rewardedRetryCount = 0
    private var nativeRetryCount = 0
    
    // Pending callbacks
    private var interstitialOnClosedCallback: (() -> Unit)? = null
    
    // Consent manager
    private val consentManager = ConsentManager.getInstance(context)
    
    // Handler for delayed operations
    private val mainHandler = Handler(Looper.getMainLooper())
    
    /**
     * Initialize the AdManager
     */
    fun initialize() {
        Log.d(TAG, "🚀 === INITIALIZING AD MANAGER ===")
        
        // Check consent status
        val consentStatus = consentManager.getConsentStatusForAds()
        Log.d(TAG, "📊 Consent status: $consentStatus")
        
        // Preload ads immediately for faster appearance
        preloadInterstitialAd()
        preloadRewardedAd()
        preloadNativeAd()
        
        Log.d(TAG, "✅ AdManager initialized successfully")
    }

    /**
     * Call when consent status changes to refresh ad requests accordingly
     */
    fun onConsentUpdated() {
        Log.d(TAG, "🛠️ Consent updated → refreshing ad requests")
        // Clear cached ads and reload with new targeting
        cleanup()
        preloadInterstitialAd()
        preloadRewardedAd()
        // Note: Banner ads are managed by BannerAdManager; it should call load with a fresh request
    }
    
    /**
     * Preload banner ads for faster appearance
     */
    fun preloadBannerAds() {
        Log.d(TAG, "🔄 Preloading banner ads for faster appearance...")
        // This will be called by BannerAdManager to ensure ads are ready
    }
    
    /**
     * Get banner ad unit ID (test or production)
     */
    fun getBannerAdUnitId(): String {
        return if (isTestMode()) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID
    }
    
    /**
     * Get interstitial ad unit ID (test or production)
     */
    fun getInterstitialAdUnitId(): String {
        return if (isTestMode()) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID
    }
    
    /**
     * Get rewarded ad unit ID (test or production)
     */
    fun getRewardedAdUnitId(): String {
        return if (isTestMode()) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
    }
    
    /**
     * Get native ad unit ID (test or production)
     */
    fun getNativeAdUnitId(): String {
        return if (isTestMode()) TEST_NATIVE_AD_UNIT_ID else PROD_NATIVE_AD_UNIT_ID
    }
    
    /**
     * Check if we're in test mode
     */
    private fun isTestMode(): Boolean {
        // TODO: Implement proper test mode detection
        // For now, always use test ads
        return false
    }
    
    /**
     * Create AdRequest with consent awareness
     */
    fun createAdRequest(): AdRequest {
        val builder = AdRequest.Builder()

        // Determine consent precisely
        val consentStatus = consentManager.getConsentStatusForAds()
        when (consentStatus) {
            ConsentManager.ConsentStatus.NON_PERSONALIZED -> {
                val extras = android.os.Bundle().apply { putString("npa", "1") }
                builder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                Log.d(TAG, "🔒 Requesting NON-PERSONALIZED ads (npa=1)")
            }
            ConsentManager.ConsentStatus.PERSONALIZED,
            ConsentManager.ConsentStatus.NOT_REQUIRED -> {
                Log.d(TAG, "✅ Requesting PERSONALIZED ads (status=$consentStatus)")
            }
            ConsentManager.ConsentStatus.UNKNOWN -> {
                // Safe default: personalized unless your policy requires stricter default.
                Log.d(TAG, "ℹ️ Consent UNKNOWN, defaulting to PERSONALIZED ad request")
            }
        }

        return builder.build()
    }
    
    /**
     * Preload interstitial ad
     */
    fun preloadInterstitialAd() {
        if (isInterstitialAdLoading || interstitialAd != null) {
            Log.d(TAG, "⏳ Interstitial ad already loading or loaded")
            return
        }
        
        isInterstitialAdLoading = true
        Log.d(TAG, "🔄 Preloading interstitial ad...")
        
        InterstitialAd.load(
            context,
            getInterstitialAdUnitId(),
            createAdRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "✅ Interstitial ad loaded successfully")
                    interstitialAd = ad
                    isInterstitialAdLoading = false
                    interstitialRetryCount = 0
                    
                    // Set ad listener
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "🔒 Interstitial ad dismissed")
                            interstitialAd = null
                            // Invoke pending callback if any
                            interstitialOnClosedCallback?.invoke()
                            interstitialOnClosedCallback = null
                            // Preload next ad
                            preloadInterstitialAd()
                        }
                        
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "❌ Interstitial ad failed to show: ${adError.message}")
                            interstitialAd = null
                            // Invoke pending callback to continue flow
                            interstitialOnClosedCallback?.invoke()
                            interstitialOnClosedCallback = null
                        }
                        
                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "🔓 Interstitial ad showed full screen content")
                        }
                    }
                }
                
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "❌ Interstitial ad failed to load: ${loadAdError.message}")
                    isInterstitialAdLoading = false
                    interstitialAd = null
                    
                    // Retry logic
                    if (interstitialRetryCount < MAX_RETRY_ATTEMPTS) {
                        interstitialRetryCount++
                        Log.d(TAG, "🔄 Retrying interstitial ad load (attempt $interstitialRetryCount)")
                        mainHandler.postDelayed({
                            preloadInterstitialAd()
                        }, AD_RETRY_DELAY)
                    } else {
                        Log.w(TAG, "⚠️ Max retry attempts reached for interstitial ad")
                        interstitialRetryCount = 0
                    }
                }
            }
        )
    }
    
    /**
     * Preload rewarded ad
     */
    fun preloadRewardedAd() {
        if (isRewardedAdLoading || rewardedAd != null) {
            Log.d(TAG, "⏳ Rewarded ad already loading or loaded")
            return
        }
        
        isRewardedAdLoading = true
        Log.d(TAG, "🔄 Preloading rewarded ad...")
        
        RewardedAd.load(
            context,
            getRewardedAdUnitId(),
            createAdRequest(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "✅ Rewarded ad loaded successfully")
                    rewardedAd = ad
                    isRewardedAdLoading = false
                    rewardedRetryCount = 0
                }
                
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "❌ Rewarded ad failed to load: ${loadAdError.message}")
                    isRewardedAdLoading = false
                    rewardedAd = null
                    
                    // Retry logic
                    if (rewardedRetryCount < MAX_RETRY_ATTEMPTS) {
                        rewardedRetryCount++
                        Log.d(TAG, "🔄 Retrying rewarded ad load (attempt $rewardedRetryCount)")
                        mainHandler.postDelayed({
                            preloadRewardedAd()
                        }, AD_RETRY_DELAY)
                    } else {
                        Log.w(TAG, "⚠️ Max retry attempts reached for rewarded ad")
                        rewardedRetryCount = 0
                    }
                }
            }
        )
    }
    
    /**
     * Preload native ad
     */
    fun preloadNativeAd() {
        if (isNativeAdLoading || nativeAd != null) {
            Log.d(TAG, "⏳ Native ad already loading or loaded")
            return
        }
        
        isNativeAdLoading = true
        Log.d(TAG, "🔄 Preloading native ad...")
        
        // TODO: Implement native ad loading
        // NativeAd.load() requires more complex implementation
        isNativeAdLoading = false
    }
    
    /**
     * Check if interstitial ad is ready
     */
    fun isInterstitialAdReady(): Boolean {
        return interstitialAd != null
    }
    
    /**
     * Check if rewarded ad is ready
     */
    fun isRewardedAdReady(): Boolean {
        return rewardedAd != null
    }
    
    /**
     * Check if native ad is ready
     */
    fun isNativeAdReady(): Boolean {
        return nativeAd != null
    }
    
    /**
     * Get interstitial ad (consumes the ad)
     */
    fun getInterstitialAd(): InterstitialAd? {
        val ad = interstitialAd
        interstitialAd = null
        // Preload next ad
        preloadInterstitialAd()
        return ad
    }
    
    /**
     * Get rewarded ad (consumes the ad)
     */
    fun getRewardedAd(): RewardedAd? {
        val ad = rewardedAd
        rewardedAd = null
        // Preload next ad
        preloadRewardedAd()
        return ad
    }
    
    /**
     * Get native ad (consumes the ad)
     */
    fun getNativeAd(): NativeAd? {
        val ad = nativeAd
        nativeAd = null
        // Preload next ad
        preloadNativeAd()
        return ad
    }
    
    /**
     * Show interstitial ad if ready
     */
    fun showInterstitialAd(activity: android.app.Activity, onAdClosed: (() -> Unit)? = null) {
        val ad = getInterstitialAd()
        if (ad != null) {
            Log.d(TAG, "🔓 Showing interstitial ad")
            // Store callback to invoke on dismissal or failure-to-show
            interstitialOnClosedCallback = onAdClosed
            ad.show(activity)
        } else {
            Log.w(TAG, "⚠️ Interstitial ad not ready")
            onAdClosed?.invoke()
        }
    }
    
    /**
     * Show rewarded ad if ready
     */
    fun showRewardedAd(
        activity: android.app.Activity,
        onRewarded: (() -> Unit)? = null,
        onAdClosed: (() -> Unit)? = null
    ) {
        val ad = getRewardedAd()
        if (ad != null) {
            Log.d(TAG, "🔓 Showing rewarded ad")
            ad.show(activity) { 
                Log.d(TAG, "🎁 User earned reward")
                onRewarded?.invoke()
            }
        } else {
            Log.w(TAG, "⚠️ Rewarded ad not ready")
            onAdClosed?.invoke()
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        Log.d(TAG, "🧹 Cleaning up AdManager resources")
        interstitialAd = null
        rewardedAd = null
        nativeAd = null
        isInterstitialAdLoading = false
        isRewardedAdLoading = false
        isNativeAdLoading = false
        interstitialRetryCount = 0
        rewardedRetryCount = 0
        nativeRetryCount = 0
    }
}
