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
import com.nocturnevpn.utils.SubscriptionSyncManager
import com.nocturnevpn.workers.ServerFetchWorker
import com.nocturnevpn.workers.SubscriptionVerifyWorker
import com.nocturnevpn.view.managers.AdManager
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.TimeUnit

class NocturnVPNAppliction : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Log app installation source (Play Store detection)
        logAppInstallationSource()
        
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
                
                // Restore subscription if user is signed in
                if (authManager.isUserSignedIn()) {
                    val subscriptionSyncManager = SubscriptionSyncManager.getInstance(this)
                    subscriptionSyncManager.restoreSubscriptionFromFirebase(
                        onSuccess = { subscriptionStatus ->
                            Log.d("NocturnVPNAppliction", "Subscription restored: status=${subscriptionStatus?.status}, expiry=${subscriptionStatus?.expiryTimeMillis}")
                        },
                        onFailure = { error ->
                            Log.w("NocturnVPNAppliction", "Failed to restore subscription: $error")
                        }
                    )
                }
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
        scheduleDailySubscriptionVerification()
        setupSubscriptionExpiryEnforcement()
        
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

    private fun setupSubscriptionExpiryEnforcement() {
        try {
            val constraints = Constraints.Builder().build()
            val repeatInterval = 15L
            val workRequest = PeriodicWorkRequestBuilder<com.nocturnevpn.workers.SubscriptionExpiryWorker>(
                repeatInterval, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).cancelUniqueWork("subscription_expiry_enforcement")
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "subscription_expiry_enforcement",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            Log.d("TVPNApplication", "Scheduled subscription expiry enforcement every $repeatInterval minutes")
        } catch (e: Exception) {
            Log.e("TVPNApplication", "Error scheduling expiry enforcement", e)
        }
    }

    private fun scheduleDailySubscriptionVerification() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
            val dailyVerify = PeriodicWorkRequestBuilder<SubscriptionVerifyWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .addTag("subscription_verify_daily")
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "subscription_verify_daily",
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyVerify
            )
            Log.d("NocturnVPNAppliction", "Scheduled daily subscription verification")
        } catch (e: Exception) {
            Log.e("NocturnVPNAppliction", "Error scheduling SubscriptionVerifyWorker: ${e.message}")
        }
    }

    /**
     * Log app installation source to detect if downloaded from Play Store
     */
    private fun logAppInstallationSource() {
        try {
            val installer = packageManager.getInstallerPackageName(packageName)
            val appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, 0).longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionCode.toString()
            }
            
            Log.d("NocturnVPNAppliction", "📱 App Installation Source:")
            Log.d("NocturnVPNAppliction", "   Installer Package: $installer")
            Log.d("NocturnVPNAppliction", "   App Version: $appVersion (Code: $versionCode)")
            Log.d("NocturnVPNAppliction", "   Package Name: $packageName")
            
            when (installer) {
                "com.android.vending" -> {
                    Log.d("NocturnVPNAppliction", "✅ Downloaded from Google Play Store")
                }
                "com.amazon.venezia" -> {
                    Log.d("NocturnVPNAppliction", "🛒 Downloaded from Amazon Appstore")
                }
                "com.samsung.android.galaxyapps" -> {
                    Log.d("NocturnVPNAppliction", "📱 Downloaded from Samsung Galaxy Store")
                }
                null -> {
                    Log.d("NocturnVPNAppliction", "🔧 Downloaded from unknown source (sideloaded)")
                }
                else -> {
                    Log.d("NocturnVPNAppliction", "📦 Downloaded from: $installer")
                }
            }
        } catch (e: Exception) {
            Log.e("NocturnVPNAppliction", "Error detecting installation source: ${e.message}")
        }
    }

}