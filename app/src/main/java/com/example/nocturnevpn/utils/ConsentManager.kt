package com.example.nocturnevpn.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.ump.*
import com.example.nocturnevpn.view.dialogs.ConsentPopupDialog
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * ConsentManager - Handles GDPR/CCPA compliance using Google UMP SDK
 *
 * Features:
 * - Real IP-based region detection (more accurate than device locale)
 * - Smart consent popup only when required
 * - Save consent decisions locally and to Firebase
 * - Support for personalized/non-personalized ads
 * - Manage consent from settings
 */
class ConsentManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ConsentManager"
        private const val PREFS_NAME = "consent_preferences"
        private const val KEY_CONSENT_STATUS = "consent_status"
        private const val KEY_CONSENT_TIMESTAMP = "consent_timestamp"
        private const val KEY_LAST_CONSENT_CHECK = "last_consent_check"
        private const val KEY_USER_COUNTRY_CODE = "user_country_code"
        private const val KEY_USER_IP_LOCATION = "user_ip_location"
        private const val KEY_CONSENT_POPUP_SHOWN = "consent_popup_shown"

        @Volatile
        private var INSTANCE: ConsentManager? = null

        fun getInstance(context: Context): ConsentManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConsentManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Consent status enum
    enum class ConsentStatus {
        PERSONALIZED,      // User consented to personalized ads
        NON_PERSONALIZED,  // User consented to non-personalized ads only
        NOT_REQUIRED,      // Consent not required for this region
        UNKNOWN           // Consent status not determined yet
    }

    // GDPR/CCPA regions
    private val gdprRegions = setOf(
        "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU",
        "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE"
    )

    private val ccpaRegions = setOf("US")
    
    // For testing purposes, let's also include India
    private val testRegions = setOf("IN")

    // IP Location data
    private var userCountryCode: String? = null
    private var userIPLocation: String? = null
    private var isIPLocationDetected = false

    /**
     * Initialize consent management with real IP location detection
     */
    fun initializeConsent(activity: Activity, callback: (ConsentStatus) -> Unit) {
        Log.d(TAG, "🚀 === CONSENT MANAGER INITIALIZATION STARTED ===")
        Log.d(TAG, "📱 Activity: ${activity.javaClass.simpleName}")
        
        // Add initial delay to allow app to load properly
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "⏰ Starting IP location detection after initial delay...")
            
            // First, try to get IP location for accurate region detection
            detectUserIPLocation { success ->
                if (success) {
                    Log.d(TAG, "✅ IP location detected successfully")
                    Log.d(TAG, "🌍 User country from IP: $userCountryCode")
                    Log.d(TAG, "📍 User location: $userIPLocation")
                    Log.d(TAG, "🔍 Consent required: ${isConsentRequired()}")
                    Log.d(TAG, "💾 Saved consent status: ${getSavedConsentStatus()}")
                    
                    // Check if consent is required for this region
                    if (isConsentRequired()) {
                        Log.d(TAG, "📋 Consent is required for this region")
                        
                        // Check if consent popup has been shown before
                        if (shouldShowConsentPopup()) {
                            Log.d(TAG, "📱 Showing consent popup for first time")
                            // Show real consent popup
                            showUMPConsentForm(activity) { consentStatus ->
                                Log.d(TAG, "✅ Consent popup completed with status: $consentStatus")
                                saveConsentStatus(consentStatus)
                                markConsentPopupShown()
                                callback(consentStatus)
                            }
                        } else {
                            Log.d(TAG, "📱 Consent popup already shown, using saved status")
                            val savedStatus = getSavedConsentStatus()
                            callback(savedStatus)
                        }
                    } else {
                        Log.d(TAG, "📋 Consent not required for this region")
                        val status = ConsentStatus.NOT_REQUIRED
                        saveConsentStatus(status)
                        callback(status)
                    }
                } else {
                    Log.w(TAG, "⚠️ IP location detection failed, using fallback methods")
                    // Fallback to device locale detection
                    val fallbackCountry = getFallbackCountryCode()
                    Log.d(TAG, "🔄 Using fallback country: $fallbackCountry")
                    userCountryCode = fallbackCountry
                    
                    val consentRequired = isConsentRequired()
                    Log.d(TAG, "🔍 Consent required (fallback): $consentRequired")
                    
                    val status = if (consentRequired) ConsentStatus.NOT_REQUIRED else ConsentStatus.NOT_REQUIRED
                    saveConsentStatus(status)
                    callback(status)
                }
                
                Log.d(TAG, "✅ Consent initialization completed")
            }
        }, 2000) // 2 second initial delay
    }

    /**
     * Detect user's real IP location for accurate region detection
     */
    private fun detectUserIPLocation(callback: (Boolean) -> Unit) {
        Log.d(TAG, "🌍 === DETECTING USER IP LOCATION ===")
        
        // Check if we have cached IP location
        val cachedCountry = prefs.getString(KEY_USER_COUNTRY_CODE, null)
        val cachedLocation = prefs.getString(KEY_USER_IP_LOCATION, null)
        
        if (cachedCountry != null && cachedLocation != null) {
            Log.d(TAG, "💾 Using cached IP location: $cachedCountry, $cachedLocation")
            userCountryCode = cachedCountry
            userIPLocation = cachedLocation
            isIPLocationDetected = true
            callback(true)
            return
        }
        
        // Add delay before making API request to ensure network is ready
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "⏰ Starting IP location API request after network delay...")
            
            // Use IP detection service (same as GlobeManager)
            val client = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)  // Increased timeout
                .readTimeout(8, TimeUnit.SECONDS)     // Increased timeout
                .writeTimeout(8, TimeUnit.SECONDS)    // Increased timeout
                .build()
                
            val request = Request.Builder()
                .url("https://ipinfo.io/json")
                .addHeader("Cache-Control", "no-cache")
                .build()

            Log.d(TAG, "📡 Requesting IP location from ipinfo.io...")

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "❌ IP location request failed: ${e.message}")
                    // Add delay before fallback to ensure proper timing
                    Handler(Looper.getMainLooper()).postDelayed({
                        callback(false)
                    }, 1000) // 1 second delay before fallback
                }

                override fun onResponse(call: Call, response: Response) {
                    response.body?.string()?.let { responseData ->
                        try {
                            val json = JSONObject(responseData)
                            val ip = json.optString("ip")
                            val country = json.optString("country")
                            val city = json.optString("city")
                            val region = json.optString("region")
                            val loc = json.optString("loc")

                            Log.d(TAG, "📊 IP Location Response:")
                            Log.d(TAG, "   IP: $ip")
                            Log.d(TAG, "   Country: $country")
                            Log.d(TAG, "   City: $city")
                            Log.d(TAG, "   Region: $region")
                            Log.d(TAG, "   Coordinates: $loc")

                            if (!country.isNullOrEmpty()) {
                                userCountryCode = country.uppercase()
                                userIPLocation = "$city, $region, $country"
                                isIPLocationDetected = true
                                
                                // Cache the IP location data
                                prefs.edit().apply {
                                    putString(KEY_USER_COUNTRY_CODE, userCountryCode)
                                    putString(KEY_USER_IP_LOCATION, userIPLocation)
                                }.apply()
                                
                                Log.d(TAG, "✅ IP location detected and cached successfully")
                                
                                // Add delay before callback to ensure proper timing
                                Handler(Looper.getMainLooper()).postDelayed({
                                    callback(true)
                                }, 500) // 0.5 second delay before callback
                            } else {
                                Log.e(TAG, "❌ Country code is empty in IP response")
                                Handler(Looper.getMainLooper()).postDelayed({
                                    callback(false)
                                }, 1000) // 1 second delay before fallback
                            }

                        } catch (e: JSONException) {
                            Log.e(TAG, "❌ Failed to parse IP location JSON: ${e.message}")
                            Handler(Looper.getMainLooper()).postDelayed({
                                callback(false)
                            }, 1000) // 1 second delay before fallback
                        }
                    } ?: run {
                        Log.e(TAG, "❌ Empty response body from IP location service")
                        Handler(Looper.getMainLooper()).postDelayed({
                            callback(false)
                        }, 1000) // 1 second delay before fallback
                    }
                }
            })
        }, 1500) // 1.5 second delay before API request
    }

    /**
     * Get fallback country code using device locale (when IP detection fails)
     */
    private fun getFallbackCountryCode(): String {
        Log.d(TAG, "🔄 === FALLBACK COUNTRY DETECTION ===")
        
        // Method 1: Try to get from device locale
        try {
            val localeCountry = context.resources.configuration.locales[0].country
            Log.d(TAG, "📱 Device locale country: $localeCountry")
            if (!localeCountry.isNullOrEmpty() && localeCountry != "US") {
                Log.d(TAG, "✅ Using device locale country: $localeCountry")
                return localeCountry
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting device locale country: ${e.message}")
        }
        
        // Method 2: Try to get from system locale
        try {
            val systemCountry = context.resources.configuration.locale.country
            Log.d(TAG, "🖥️ System locale country: $systemCountry")
            if (!systemCountry.isNullOrEmpty() && systemCountry != "US") {
                Log.d(TAG, "✅ Using system locale country: $systemCountry")
                return systemCountry
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting system locale country: ${e.message}")
        }
        
        // Method 3: Try to get from default locale
        try {
            val defaultCountry = java.util.Locale.getDefault().country
            Log.d(TAG, "🌐 Default locale country: $defaultCountry")
            if (!defaultCountry.isNullOrEmpty() && defaultCountry != "US") {
                Log.d(TAG, "✅ Using default locale country: $defaultCountry")
                return defaultCountry
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting default locale country: ${e.message}")
        }
        
        // Method 4: Check if we can detect India specifically
        try {
            val timeZone = java.util.TimeZone.getDefault()
            val timeZoneId = timeZone.id
            Log.d(TAG, "⏰ Timezone: $timeZoneId")
            
            // Check for Indian timezone
            if (timeZoneId.contains("Asia/Kolkata") || timeZoneId.contains("IST")) {
                Log.d(TAG, "🇮🇳 Detected Indian timezone, using IN")
                return "IN"
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting timezone: ${e.message}")
        }
        
        // Method 5: Check language settings for Hindi/Gujarati
        try {
            val language = context.resources.configuration.locales[0].language
            Log.d(TAG, "🗣️ Device language: $language")
            
            if (language == "hi" || language == "gu") {
                Log.d(TAG, "🇮🇳 Detected Indian language ($language), using IN")
                return "IN"
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting language: ${e.message}")
        }
        
        // Fallback: Since you mentioned you're in India, let's default to IN for testing
        Log.d(TAG, "🔄 All fallback methods failed, defaulting to IN (India) for testing")
        return "IN"
    }

    /**
     * Get user's country code (prioritizes IP location over device locale)
     */
    fun getCountryCode(): String {
        return userCountryCode ?: getFallbackCountryCode()
    }

    /**
     * Get user's IP location details
     */
    fun getUserIPLocation(): String? {
        return userIPLocation
    }

    /**
     * Check if IP location was successfully detected
     */
    fun isIPLocationDetected(): Boolean {
        return isIPLocationDetected
    }

    /**
     * Save consent status to SharedPreferences and Firebase
     */
    private fun saveConsentStatus(status: ConsentStatus) {
        Log.d(TAG, "💾 === SAVING CONSENT STATUS ===")
        Log.d(TAG, "📊 Status to save: $status")
        Log.d(TAG, "⏰ Timestamp: ${System.currentTimeMillis()}")
        
        // Save to SharedPreferences
        prefs.edit().apply {
            putString(KEY_CONSENT_STATUS, status.name)
            putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
            putLong(KEY_LAST_CONSENT_CHECK, System.currentTimeMillis())
        }.apply()
        
        Log.d(TAG, "✅ Consent status saved to SharedPreferences")
        
        // Also sync to Firebase for tracking
        syncConsentToFirebase(status)
    }

    /**
     * Get saved consent status from SharedPreferences
     */
    fun getSavedConsentStatus(): ConsentStatus {
        val statusString = prefs.getString(KEY_CONSENT_STATUS, null)
        val status = if (statusString != null) {
            try {
                ConsentStatus.valueOf(statusString)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "❌ Invalid consent status in SharedPreferences: $statusString")
                ConsentStatus.UNKNOWN
            }
        } else {
            Log.d(TAG, "📝 No consent status found in SharedPreferences")
            ConsentStatus.UNKNOWN
        }
        
        Log.d(TAG, "💾 Retrieved consent status: $status")
        return status
    }

    /**
     * Check if consent is required for current region (using IP location)
     */
    fun isConsentRequired(): Boolean {
        val countryCode = getCountryCode()
        val isGdprRegion = gdprRegions.contains(countryCode)
        val isCcpaRegion = ccpaRegions.contains(countryCode)
        val isTestRegion = testRegions.contains(countryCode)
        val consentRequired = isGdprRegion || isCcpaRegion || isTestRegion
        
        Log.d(TAG, "🌍 === REGION CHECK (IP-Based) ===")
        Log.d(TAG, "🏳️ Country code: $countryCode")
        Log.d(TAG, "📡 IP location detected: $isIPLocationDetected")
        Log.d(TAG, "🇪🇺 GDPR region: $isGdprRegion")
        Log.d(TAG, "🇺🇸 CCPA region: $isCcpaRegion")
        Log.d(TAG, "🇮🇳 Test region (India): $isTestRegion")
        Log.d(TAG, "📋 Consent required: $consentRequired")
        
        return consentRequired
    }

    /**
     * Sync consent status to Firebase Firestore for tracking
     */
    private fun syncConsentToFirebase(consentStatus: ConsentStatus) {
        Log.d(TAG, "🔥 === SYNCING TO FIREBASE ===")
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.d(TAG, "❌ No user ID available, skipping Firebase sync")
            return
        }
        
        Log.d(TAG, "👤 User ID: $userId")
        Log.d(TAG, "📊 Consent status: $consentStatus")
        
        val currentTime = System.currentTimeMillis()
        val readableTimestamp = getReadableTimestamp(currentTime)
        
        val consentData = hashMapOf(
            "consentStatus" to consentStatus.name,
            "timestamp" to currentTime,
            "readableTimestamp" to readableTimestamp,
            "countryCode" to getCountryCode(),
            "ipLocation" to getUserIPLocation(),
            "ipLocationDetected" to isIPLocationDetected(),
            "isGdprRegion" to gdprRegions.contains(getCountryCode()),
            "isCcpaRegion" to ccpaRegions.contains(getCountryCode()),
            "isTestRegion" to testRegions.contains(getCountryCode()),
            "appVersion" to getAppVersion(),
            "deviceInfo" to getDeviceInfo(),
            "consentSource" to "popup_dialog"
        )
        
        Log.d(TAG, "📋 Consent data to sync: $consentData")
        
        // Save to users/{userId}/consent/status
        db.collection("users").document(userId)
            .collection("consent")
            .document("status")
            .set(consentData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Consent status synced to Firebase successfully")
                
                // Also save to analytics collection for easy tracking
                saveToAnalyticsCollection(userId, consentStatus, currentTime, readableTimestamp)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error syncing consent to Firebase: ${e.message}")
            }
    }
    
    /**
     * Save consent data to analytics collection for easy tracking
     */
    private fun saveToAnalyticsCollection(userId: String, consentStatus: ConsentStatus, timestamp: Long, readableTimestamp: String) {
        val analyticsData = hashMapOf(
            "userId" to userId,
            "consentStatus" to consentStatus.name,
            "timestamp" to timestamp,
            "readableTimestamp" to readableTimestamp,
            "countryCode" to getCountryCode(),
            "ipLocation" to getUserIPLocation(),
            "isGdprRegion" to gdprRegions.contains(getCountryCode()),
            "isCcpaRegion" to ccpaRegions.contains(getCountryCode()),
            "isTestRegion" to testRegions.contains(getCountryCode()),
            "appVersion" to getAppVersion(),
            "deviceInfo" to getDeviceInfo()
        )
        
        db.collection("consent_analytics")
            .add(analyticsData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "📊 Consent analytics saved with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error saving consent analytics: ${e.message}")
            }
    }
    
    /**
     * Get app version for tracking
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * Get device info for tracking
     */
    private fun getDeviceInfo(): String {
        return try {
            val manufacturer = android.os.Build.MANUFACTURER
            val model = android.os.Build.MODEL
            val androidVersion = android.os.Build.VERSION.RELEASE
            "$manufacturer $model (Android $androidVersion)"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * Convert timestamp to readable date format
     */
    private fun getReadableTimestamp(timestamp: Long): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = Date(timestamp)
            dateFormat.format(date)
        } catch (e: Exception) {
            "Unknown Date"
        }
    }
    
    /**
     * Get consent analytics from Firebase for tracking
     */
    fun getConsentAnalytics(callback: (List<Map<String, Any>>) -> Unit) {
        Log.d(TAG, "📊 === FETCHING CONSENT ANALYTICS ===")
        
        db.collection("consent_analytics")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100) // Limit to last 100 entries
            .get()
            .addOnSuccessListener { documents ->
                val analyticsList = mutableListOf<Map<String, Any>>()
                
                for (document in documents) {
                    val data = document.data
                    analyticsList.add(data)
                }
                
                Log.d(TAG, "📊 Retrieved ${analyticsList.size} consent analytics entries")
                callback(analyticsList)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error fetching consent analytics: ${e.message}")
                callback(emptyList())
            }
    }
    
    /**
     * Get user's consent history from Firebase
     */
    fun getUserConsentHistory(userId: String, callback: (List<Map<String, Any>>) -> Unit) {
        Log.d(TAG, "📊 === FETCHING USER CONSENT HISTORY ===")
        Log.d(TAG, "👤 User ID: $userId")
        
        db.collection("users").document(userId)
            .collection("consent")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val historyList = mutableListOf<Map<String, Any>>()
                
                for (document in documents) {
                    val data = document.data
                    data["documentId"] = document.id
                    historyList.add(data)
                }
                
                Log.d(TAG, "📊 Retrieved ${historyList.size} consent history entries for user $userId")
                callback(historyList)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error fetching user consent history: ${e.message}")
                callback(emptyList())
            }
    }

    /**
     * Get current user ID from AuthManager
     */
    private fun getCurrentUserId(): String? {
        return try {
            val authManager = AuthManager.getInstance(context)
            val userId = authManager.getCurrentUserId()
            Log.d(TAG, "👤 Retrieved user ID: $userId")
            userId
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting current user ID: ${e.message}")
            null
        }
    }

    /**
     * Load consent status from Firebase
     */
    fun loadConsentFromFirebase(callback: (ConsentStatus?) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.d(TAG, "No user ID available, cannot load from Firebase")
            callback(null)
            return
        }

        db.collection("users").document(userId)
            .collection("consent")
            .document("status")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val statusString = document.getString("consentStatus")
                    val status = if (statusString != null) {
                        try {
                            ConsentStatus.valueOf(statusString)
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    } else null

                    if (status != null) {
                        Log.d(TAG, "Loaded consent status from Firebase: $status")
                        saveConsentStatus(status)
                        callback(status)
                    } else {
                        Log.d(TAG, "Invalid consent status in Firebase")
                        callback(null)
                    }
                } else {
                    Log.d(TAG, "No consent data found in Firebase")
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading consent from Firebase: ${e.message}")
                callback(null)
            }
    }



    /**
     * Check if consent needs to be refreshed (older than 13 months)
     */
    fun shouldRefreshConsent(): Boolean {
        val lastCheck = prefs.getLong(KEY_LAST_CONSENT_CHECK, 0L)
        val thirteenMonthsInMillis = 13L * 30L * 24L * 60L * 60L * 1000L // 13 months
        return System.currentTimeMillis() - lastCheck > thirteenMonthsInMillis
    }

    /**
     * Reset consent status (for testing or user logout)
     */
    fun resetConsentStatus() {
        Log.d(TAG, "🔄 === RESETTING CONSENT STATUS ===")
        prefs.edit().apply {
            remove(KEY_CONSENT_STATUS)
            remove(KEY_CONSENT_TIMESTAMP)
            remove(KEY_LAST_CONSENT_CHECK)
            // Don't clear IP location data as it's still valid
        }.apply()
        Log.d(TAG, "✅ Consent status reset successfully")
    }

    /**
     * Get consent status for ad loading
     */
    fun getConsentStatusForAds(): ConsentStatus {
        val status = getSavedConsentStatus()
        Log.d(TAG, "📊 Consent status for ads: $status")
        return status
    }
    
    /**
     * Check if personalized ads are allowed
     */
    fun canShowPersonalizedAds(): Boolean {
        return getSavedConsentStatus() == ConsentStatus.PERSONALIZED
    }
    
    /**
     * Check if non-personalized ads are allowed
     */
    fun canShowNonPersonalizedAds(): Boolean {
        val status = getSavedConsentStatus()
        return status == ConsentStatus.PERSONALIZED || status == ConsentStatus.NON_PERSONALIZED
    }
    
    /**
     * Get consent status for AppLovin SDK
     */
    fun getAppLovinConsentStatus(): String {
        return when (getSavedConsentStatus()) {
            ConsentStatus.PERSONALIZED -> "PERSONALIZED"
            ConsentStatus.NON_PERSONALIZED -> "NON_PERSONALIZED"
            ConsentStatus.NOT_REQUIRED -> "NOT_REQUIRED"
            ConsentStatus.UNKNOWN -> "UNKNOWN"
        }
    }

    /**
     * Debug method to show current consent state
     */
    fun debugConsentState() {
        Log.d(TAG, "🔍 === CONSENT STATE DEBUG ===")
        Log.d(TAG, "📊 Saved consent status: ${getSavedConsentStatus()}")
        Log.d(TAG, "🌍 Country code: ${getCountryCode()}")
        Log.d(TAG, "📡 IP location detected: $isIPLocationDetected")
        Log.d(TAG, "📍 IP location: ${getUserIPLocation()}")
        Log.d(TAG, "📋 Consent required: ${isConsentRequired()}")
        Log.d(TAG, "⏰ Should refresh: ${shouldRefreshConsent()}")
        Log.d(TAG, "👤 User ID: ${getCurrentUserId()}")
        Log.d(TAG, "💾 Consent timestamp: ${prefs.getLong(KEY_CONSENT_TIMESTAMP, 0L)}")
        Log.d(TAG, "🔄 Last check timestamp: ${prefs.getLong(KEY_LAST_CONSENT_CHECK, 0L)}")
        Log.d(TAG, "========================")
    }
    
    /**
     * Test method to verify ConsentManager functionality
     */
    fun testConsentManager() {
        Log.d(TAG, "🧪 === TESTING CONSENT MANAGER (IP-Based) ===")
        
        // Test 1: Check saved status
        val savedStatus = getSavedConsentStatus()
        Log.d(TAG, "✅ Test 1 - Saved status: $savedStatus")
        
        // Test 2: Check IP location detection
        Log.d(TAG, "✅ Test 2 - IP location detected: $isIPLocationDetected")
        Log.d(TAG, "✅ Test 2 - Country from IP: ${getCountryCode()}")
        Log.d(TAG, "✅ Test 2 - IP location: ${getUserIPLocation()}")
        
        // Test 3: Check region detection
        val consentRequired = isConsentRequired()
        Log.d(TAG, "✅ Test 3 - Consent required: $consentRequired")
        
        // Test 4: Check user ID
        val userId = getCurrentUserId()
        Log.d(TAG, "✅ Test 4 - User ID: $userId")
        
        // Test 5: Check timestamps
        val consentTimestamp = prefs.getLong(KEY_CONSENT_TIMESTAMP, 0L)
        val lastCheckTimestamp = prefs.getLong(KEY_LAST_CONSENT_CHECK, 0L)
        Log.d(TAG, "✅ Test 5 - Consent timestamp: $consentTimestamp, Last check: $lastCheckTimestamp")
        
        // Test 6: Check refresh requirement
        val shouldRefresh = shouldRefreshConsent()
        Log.d(TAG, "✅ Test 6 - Should refresh: $shouldRefresh")
        
        // Test 7: Check ad consent methods
        Log.d(TAG, "✅ Test 7 - Can show personalized ads: ${canShowPersonalizedAds()}")
        Log.d(TAG, "✅ Test 7 - Can show non-personalized ads: ${canShowNonPersonalizedAds()}")
        Log.d(TAG, "✅ Test 7 - AppLovin consent status: ${getAppLovinConsentStatus()}")
        
        // Test 8: Check consent popup status
        Log.d(TAG, "✅ Test 8 - Should show consent popup: ${shouldShowConsentPopup()}")
        
        Log.d(TAG, "🎯 === CONSENT MANAGER TEST COMPLETED ===")
        
        // Test 9: Trigger actual IP location detection (for testing)
        Log.d(TAG, "🧪 === TRIGGERING ACTUAL IP LOCATION DETECTION ===")
        forceRefreshIPLocation { success ->
            Log.d(TAG, "🎯 IP Location Detection Result: $success")
            if (success) {
                Log.d(TAG, "✅ IP Location: ${getUserIPLocation()}")
                Log.d(TAG, "✅ Country Code: ${getCountryCode()}")
                Log.d(TAG, "✅ IP Detected: $isIPLocationDetected")
            } else {
                Log.d(TAG, "❌ IP Location detection failed, using fallback")
            }
        }
    }
    
    /**
     * Show real consent popup for regions requiring consent
     */
    private fun showUMPConsentForm(activity: Activity, callback: (ConsentStatus) -> Unit) {
        Log.d(TAG, "📱 === SHOWING REAL CONSENT POPUP ===")
        
        // Show real consent popup dialog
        Handler(Looper.getMainLooper()).post {
            try {
                // Check if activity is still valid
                if (activity.isFinishing || activity.isDestroyed) {
                    Log.w(TAG, "⚠️ Activity is finishing, skipping consent popup")
                    callback(ConsentStatus.NON_PERSONALIZED)
                    return@post
                }
                
                Log.d(TAG, "📋 Showing consent popup dialog...")
                
                // Get current consent status for default selection
                val currentStatus = getSavedConsentStatus()
                Log.d(TAG, "📊 Current consent status for popup: $currentStatus")
                
                val consentDialog = ConsentPopupDialog(activity, currentStatus) { consentStatus ->
                    Log.d(TAG, "📊 User selected consent: $consentStatus")
                    callback(consentStatus)
                }
                
                consentDialog.show()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error showing consent popup: ${e.message}")
                // Fallback to non-personalized
                callback(ConsentStatus.NON_PERSONALIZED)
            }
        }
    }
    

    
    /**
     * Show privacy options form (for settings)
     */
    fun showPrivacyOptionsForm(activity: Activity, callback: (ConsentStatus) -> Unit) {
        Log.d(TAG, "⚙️ === SHOWING PRIVACY OPTIONS FORM ===")
        
        // Show real privacy options dialog
        Handler(Looper.getMainLooper()).post {
            try {
                Log.d(TAG, "📋 Showing privacy options dialog...")
                
                val currentStatus = getSavedConsentStatus()
                Log.d(TAG, "📊 Current consent status: $currentStatus")
                
                val privacyDialog = ConsentPopupDialog(activity, currentStatus) { newConsentStatus ->
                    Log.d(TAG, "📊 User changed consent: $currentStatus -> $newConsentStatus")
                    saveConsentStatus(newConsentStatus)
                    callback(newConsentStatus)
                }
                
                privacyDialog.show()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error showing privacy options: ${e.message}")
                callback(getSavedConsentStatus())
            }
        }
    }
    
    /**
     * Check if consent popup should be shown (first time only)
     */
    fun shouldShowConsentPopup(): Boolean {
        val hasShown = prefs.getBoolean(KEY_CONSENT_POPUP_SHOWN, false)
        Log.d(TAG, "📱 Consent popup shown before: $hasShown")
        return !hasShown
    }
    
    /**
     * Mark consent popup as shown
     */
    private fun markConsentPopupShown() {
        prefs.edit().putBoolean(KEY_CONSENT_POPUP_SHOWN, true).apply()
        Log.d(TAG, "✅ Consent popup marked as shown")
    }
    
    /**
     * Reset consent popup shown flag (for testing)
     */
    fun resetConsentPopupShown() {
        prefs.edit().putBoolean(KEY_CONSENT_POPUP_SHOWN, false).apply()
        Log.d(TAG, "🔄 Consent popup shown flag reset")
    }
    
    /**
     * Force refresh IP location (for testing or when needed)
     */
    fun forceRefreshIPLocation(callback: (Boolean) -> Unit) {
        Log.d(TAG, "🔄 === FORCE REFRESHING IP LOCATION ===")
        
        // Clear cached data to force fresh API call
        prefs.edit().apply {
            remove(KEY_USER_COUNTRY_CODE)
            remove(KEY_USER_IP_LOCATION)
        }.apply()
        
        Log.d(TAG, "🗑️ Cleared cached IP location data")
        
        // Reset detection state
        userCountryCode = null
        userIPLocation = null
        isIPLocationDetected = false
        
        // Add delay before fresh detection
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "⏰ Starting fresh IP location detection...")
            detectUserIPLocation(callback)
        }, 1000) // 1 second delay before fresh detection
    }
} 