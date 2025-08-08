package com.example.nocturnevpn

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.nocturnevpn.utils.AuthManager
import com.example.nocturnevpn.utils.KeyHashGenerator
import com.example.nocturnevpn.workers.ServerFetchWorker
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.TimeUnit

class NocturnVPNAppliction : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Facebook SDK
        try {
            FacebookSdk.sdkInitialize(applicationContext)
            AppEventsLogger.activateApp(this)
            Log.d("NocturnVPNAppliction", "Facebook SDK initialized successfully")
        } catch (e: Exception) {
            Log.e("NocturnVPNAppliction", "Error initializing Facebook SDK: ${e.message}")
            e.printStackTrace()
        }
        
        // Initialize Firebase Auth state
        try {
            val authManager = AuthManager.getInstance(this)
            val firebaseAuth = authManager.getFirebaseAuth()
            
            // Firebase Auth persistence is enabled by default
            // We'll handle session restoration in AuthManager with better logic
            
            Log.d("NocturnVPNAppliction", "Firebase Auth initialized, current user: ${firebaseAuth.currentUser != null}")
        } catch (e: Exception) {
            Log.e("NocturnVPNAppliction", "Error initializing Firebase Auth: ${e.message}")
            e.printStackTrace()
        }
        
        setupPeriodicServerFetch()
        
        // Initialize Google MobileAds
        try {
            MobileAds.initialize(this) { initializationStatus ->
                Log.d("NocturnVPNAppliction", "MobileAds initialization completed: ${initializationStatus.adapterStatusMap}")
            }
            Log.d("NocturnVPNAppliction", "MobileAds initialized successfully")
        } catch (e: Exception) {
            Log.e("NocturnVPNAppliction", "Error initializing MobileAds: ${e.message}")
            e.printStackTrace()
        }
        
        // Configure WebView for ads compatibility
        try {
            // Enable WebView debugging for ads
            android.webkit.WebView.setWebContentsDebuggingEnabled(true)
            
            Log.d("NocturnVPNAppliction", "WebView configured for ads compatibility")
        } catch (e: Exception) {
            Log.e("NocturnVPNAppliction", "Error configuring WebView for ads: ${e.message}")
            e.printStackTrace()
        }
        
        // Generate key hashes for Facebook authentication (debug only)
        if (BuildConfig.DEBUG) {
            KeyHashGenerator.generateKeyHash(this)
            KeyHashGenerator.generateSHA1Hash(this)
            KeyHashGenerator.generateSHA256Hash(this)
        }
    }

    private fun setupPeriodicServerFetch() {
        try {
            val constraints = Constraints.Builder()
                // No network type constraint, allow any network
                .build()

            // Set to 30 minutes for both debug and release
            val repeatInterval = 15L
            val flexInterval = 5L

            val workRequest = PeriodicWorkRequestBuilder<ServerFetchWorker>(
                repeatInterval, TimeUnit.MINUTES,
                flexInterval, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            // Cancel any existing work first
            WorkManager.getInstance(this).cancelUniqueWork("vpn_server_fetch")

            // Schedule new work
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "vpn_server_fetch",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )

            Log.d("TVPNApplication", "Scheduled periodic server fetch every $repeatInterval minutes")
        } catch (e: Exception) {
            Log.e("TVPNApplication", "Error setting up periodic work", e)
        }
    }

}