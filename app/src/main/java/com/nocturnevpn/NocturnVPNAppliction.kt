package com.nocturnevpn

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nocturnevpn.utils.AuthManager
import com.nocturnevpn.utils.KeyHashGenerator
import com.nocturnevpn.utils.ThemeManager
import com.nocturnevpn.workers.ServerFetchWorker
import com.nocturnevpn.view.managers.AdManager
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.TimeUnit

class NocturnVPNAppliction : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize third-party SDKs only if enabled
        if (!BuildConfig.DISABLE_THIRD_PARTY_SDKS) {
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
                Log.d("NocturnVPNAppliction", "Firebase Auth initialized, current user: ${firebaseAuth.currentUser != null}")
            } catch (e: Exception) {
                Log.e("NocturnVPNAppliction", "Error initializing Firebase Auth: ${e.message}")
                e.printStackTrace()
            }
            
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
            
            // Initialize AdManager
            try {
                val adManager = AdManager.getInstance(this)
                adManager.initialize()
                Log.d("NocturnVPNAppliction", "AdManager initialized successfully")
            } catch (e: Exception) {
                Log.e("NocturnVPNAppliction", "Error initializing AdManager: ${e.message}")
                e.printStackTrace()
            }
            
            // Configure WebView for ads compatibility
            try {
                android.webkit.WebView.setWebContentsDebuggingEnabled(true)
                Log.d("NocturnVPNAppliction", "WebView configured for ads compatibility")
            } catch (e: Exception) {
                Log.e("NocturnVPNAppliction", "Error configuring WebView for ads: ${e.message}")
                e.printStackTrace()
            }
        } else {
            Log.w("NocturnVPNAppliction", "Third-party SDK initialization disabled by BuildConfig flag")
        }
        
        setupPeriodicServerFetch()
        
        // Generate key hashes for Facebook authentication (debug only)
        if (BuildConfig.DEBUG) {
            KeyHashGenerator.generateKeyHash(this)
            KeyHashGenerator.generateSHA1Hash(this)
            KeyHashGenerator.generateSHA256Hash(this)
        }
        
        // Apply saved theme preference
        try {
            ThemeManager.applyTheme(this)
            Log.d("NocturnVPNAppliction", "Theme applied successfully")
        } catch (e: Exception) {
            Log.e("NocturnVPNAppliction", "Error applying theme: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupPeriodicServerFetch() {
        try {
            val constraints = Constraints.Builder()
                .build()

            val repeatInterval = 15L
            val flexInterval = 5L

            val workRequest = PeriodicWorkRequestBuilder<ServerFetchWorker>(
                repeatInterval, TimeUnit.MINUTES,
                flexInterval, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).cancelUniqueWork("vpn_server_fetch")

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