package com.nocturnevpn.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nocturnevpn.data.model.SubscriptionStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * SubscriptionSyncManager - Handles Firebase sync for subscription data
 * 
 * Features:
 * - Save subscription status to Firestore
 * - Restore subscription from Firestore on app start/login
 * - Sync subscription data across devices
 * - Handle subscription expiry and renewal
 */
class SubscriptionSyncManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SubscriptionSyncManager"
        private const val PREFS = "reward_prefs"
        private const val KEY_PRO_TIMER_END = "pro_timer_end"
        private const val KEY_PRO_TIMER_TYPE = "pro_timer_type"
        private const val KEY_PENDING_STATUS = "pending_subscription_status"
        private const val KEY_PENDING_EXPIRY = "pending_subscription_expiry"
        private const val KEY_PENDING_AUTORENEW = "pending_subscription_autorenew"
        private const val KEY_PENDING_PRODUCT_ID = "pending_subscription_product_id"
        private const val KEY_PENDING_PURCHASE_TOKEN = "pending_subscription_purchase_token"
        private const val KEY_PENDING_VERIFY_SOURCE = "pending_subscription_verify_source"
        private const val KEY_BACKEND_STATUS = "backend_status"
        private const val KEY_BACKEND_EXPIRY = "backend_expiry"
        private const val KEY_BACKEND_VERIFIED_AT = "backend_verified_at"
        
        @Volatile
        private var INSTANCE: SubscriptionSyncManager? = null
        
        fun getInstance(context: Context): SubscriptionSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SubscriptionSyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val authManager: AuthManager = AuthManager.getInstance(context)
    /**
     * Save backend-verified subscription snapshot to Firestore (merge) and local cache
     */
    fun saveBackendVerifiedSubscription(
        status: com.nocturnevpn.data.model.SubscriptionStatus,
        productId: String?,
        purchaseToken: String?,
        verifySource: String
    ) {
        // Always persist locally for immediate UX, even if user not signed in
        updateLocalSubscriptionStatus(status.expiryTimeMillis, status.status == "active", status.autoRenewing)

        // Persist backend verdict snapshot locally for authoritative restore decisions
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_BACKEND_STATUS, status.status)
                .putLong(KEY_BACKEND_EXPIRY, status.expiryTimeMillis)
                .putLong(KEY_BACKEND_VERIFIED_AT, System.currentTimeMillis())
                .apply()
        } catch (_: Exception) { }

        val userId = authManager.getCurrentUserId()
        if (userId == null) {
            Log.d(TAG, "No user ID; saving subscription snapshot as pending (source=$verifySource)")
            savePendingSubscriptionSnapshot(status, productId, purchaseToken, verifySource)
            return
        }

        val data = hashMapOf<String, Any>(
            "status" to status.status,
            "expiryTimeMillis" to status.expiryTimeMillis,
            "autoRenewing" to status.autoRenewing,
            "lastVerifiedAt" to System.currentTimeMillis(),
            "verifySource" to verifySource,
            "packageName" to context.packageName,
            "appVersion" to getAppVersion(context),
            "deviceId" to getUserFriendlyDeviceId(context)
        ).apply {
            if (!productId.isNullOrEmpty()) put("productId", productId)
            if (!purchaseToken.isNullOrEmpty()) put("purchaseToken", purchaseToken)
        }

        db.collection("users").document(userId)
            .collection("subscription").document("current")
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Saved subscription snapshot to Firebase and local cache (source=$verifySource)")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("SubscriptionSyncManager", "Failed to save subscription snapshot: ${e.message}")
            }
    }

    private fun savePendingSubscriptionSnapshot(
        status: com.nocturnevpn.data.model.SubscriptionStatus,
        productId: String?,
        purchaseToken: String?,
        verifySource: String
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PENDING_STATUS, status.status)
            .putLong(KEY_PENDING_EXPIRY, status.expiryTimeMillis)
            .putBoolean(KEY_PENDING_AUTORENEW, status.autoRenewing ?: false)
            .putString(KEY_PENDING_PRODUCT_ID, productId ?: "")
            .putString(KEY_PENDING_PURCHASE_TOKEN, purchaseToken ?: "")
            .putString(KEY_PENDING_VERIFY_SOURCE, verifySource)
            .apply()
        Log.d(TAG, "Pending subscription snapshot saved locally: status=${status.status}, expiry=${status.expiryTimeMillis}")
    }

    fun flushPendingSubscriptionSnapshot() {
        val userId = authManager.getCurrentUserId() ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pendingStatus = prefs.getString(KEY_PENDING_STATUS, null) ?: return
        val pendingExpiry = prefs.getLong(KEY_PENDING_EXPIRY, 0L)
        val pendingAutoRenew = prefs.getBoolean(KEY_PENDING_AUTORENEW, false)
        val pendingProductId = prefs.getString(KEY_PENDING_PRODUCT_ID, null)
        val pendingPurchaseToken = prefs.getString(KEY_PENDING_PURCHASE_TOKEN, null)
        val pendingVerifySource = prefs.getString(KEY_PENDING_VERIFY_SOURCE, "pending") ?: "pending"

        val status = SubscriptionStatus(
            status = pendingStatus,
            expiryTimeMillis = pendingExpiry,
            autoRenewing = pendingAutoRenew
        )

        val data = hashMapOf<String, Any>(
            "status" to status.status,
            "expiryTimeMillis" to status.expiryTimeMillis,
            "autoRenewing" to status.autoRenewing,
            "lastVerifiedAt" to System.currentTimeMillis(),
            "verifySource" to pendingVerifySource,
            "packageName" to context.packageName,
            "appVersion" to getAppVersion(context),
            "deviceId" to getUserFriendlyDeviceId(context)
        ).apply {
            if (!pendingProductId.isNullOrEmpty()) put("productId", pendingProductId)
            if (!pendingPurchaseToken.isNullOrEmpty()) put("purchaseToken", pendingPurchaseToken)
        }

        db.collection("users").document(userId)
            .collection("subscription").document("current")
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Flushed pending subscription snapshot to Firebase")
                // Clear pending keys
                prefs.edit()
                    .remove(KEY_PENDING_STATUS)
                    .remove(KEY_PENDING_EXPIRY)
                    .remove(KEY_PENDING_AUTORENEW)
                    .remove(KEY_PENDING_PRODUCT_ID)
                    .remove(KEY_PENDING_PURCHASE_TOKEN)
                    .remove(KEY_PENDING_VERIFY_SOURCE)
                    .apply()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to flush pending subscription snapshot: ${e.message}")
            }
    }
    
    /**
     * Save backend-verified subscription data to Firebase Firestore
     */
    fun saveBackendVerifiedSubscriptionToFirebase(
        subscriptionStatus: SubscriptionStatus,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) {
        val userId = authManager.getCurrentUserId()
        if (userId == null) {
            Log.w(TAG, "No user ID available for subscription sync")
            onFailure?.invoke("User not authenticated")
            return
        }

        Log.d(TAG, "🔥 Saving backend-verified subscription to Firebase for user: $userId")
        Log.d(TAG, "Status: ${subscriptionStatus.status}, Expiry: ${subscriptionStatus.expiryTimeMillis}")

        val subscriptionData = hashMapOf<String, Any>(
            "status" to subscriptionStatus.status,
            "expiryTimeMillis" to subscriptionStatus.expiryTimeMillis,
            "autoRenewing" to subscriptionStatus.autoRenewing,
            "lastSyncDate" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .collection("subscription")
            .document("current")
            .set(subscriptionData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "✅ Backend-verified subscription data saved to Firebase successfully")
                // Also update local shared preferences for quick access if needed (e.g., for non-critical UI updates)
                updateLocalSubscriptionStatus(subscriptionStatus.expiryTimeMillis, subscriptionStatus.status == "active", subscriptionStatus.autoRenewing)
                onSuccess?.invoke()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "❌ Failed to save backend-verified subscription to Firebase: ${exception.message}")
                onFailure?.invoke(exception.message ?: "Unknown error")
            }
    }

    /**
     * Restore subscription from Firebase Firestore
     */
    fun restoreSubscriptionFromFirebase(
        onSuccess: ((SubscriptionStatus?) -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) {
        val userId = authManager.getCurrentUserId()
        if (userId == null) {
            Log.w(TAG, "No user ID available for subscription restore")
            onFailure?.invoke("User not authenticated")
            return
        }
        
        Log.d(TAG, "🔄 Restoring subscription from Firebase for user: $userId")

        // 1) First, consult last backend-verified local snapshot if present (authoritative)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val backendStatus = prefs.getString(KEY_BACKEND_STATUS, null)
        val backendExpiry = prefs.getLong(KEY_BACKEND_EXPIRY, 0L)
        val backendVerifiedAt = prefs.getLong(KEY_BACKEND_VERIFIED_AT, 0L)
        val nowTs = System.currentTimeMillis()
        if (!backendStatus.isNullOrEmpty() && backendVerifiedAt > 0L) {
            val backendActive = backendStatus == "active" && backendExpiry > nowTs
            if (backendActive) {
                // Force active from backend snapshot
                updateLocalSubscriptionStatus(backendExpiry, true, true)
                Log.d(TAG, "✅ Using backend snapshot: active (expiry=$backendExpiry)")
                onSuccess?.invoke(SubscriptionStatus("active", backendExpiry, true))
                return
            } else {
                // If backend says inactive/expired, force clear regardless of Firebase to avoid false-active
                Log.d(TAG, "🚫 Using backend snapshot: inactive/expired (status=$backendStatus, expiry=$backendExpiry)")
                clearLocalSubscription()
                onSuccess?.invoke(SubscriptionStatus(backendStatus, backendExpiry, false))
                return
            }
        }
        
        db.collection("users").document(userId)
            .collection("subscription")
            .document("current")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val endTime = document.getLong("expiryTimeMillis") ?: 0L
                    val status = document.getString("status") ?: "inactive"
                    val autoRenewing = document.getBoolean("autoRenewing") ?: false
                    val now = System.currentTimeMillis()
                    val lastVerifiedAt = document.getLong("lastVerifiedAt") ?: 0L
                    
                    // Check if Firebase data is recent (within last 5 minutes)
                    val isRecentData = lastVerifiedAt > 0L && (now - lastVerifiedAt) < 5L * 60L * 1000L
                    
                    // Only consider active if status is active, not expired, AND data is recent
                    // Remove grace period to prevent stale data from showing as active
                    val isActive = status == "active" && endTime > now && isRecentData

                    Log.d(TAG, "📋 Subscription data found:")
                    Log.d(TAG, "Status: $status, End time: $endTime (${getReadableDate(endTime)}), Auto-renewing: $autoRenewing")
                    Log.d(TAG, "Last verified: $lastVerifiedAt (${getReadableDate(lastVerifiedAt)}), Recent: $isRecentData")
                    Log.d(TAG, "Is active (strict check): $isActive")

                    if (isActive) {
                        updateLocalSubscriptionStatus(endTime, true, autoRenewing)
                        Log.d(TAG, "✅ Subscription restored to local storage (endTime=$endTime)")
                        onSuccess?.invoke(SubscriptionStatus(status, endTime, autoRenewing))
                    } else {
                        Log.d(TAG, "⚠️ Subscription expired, inactive, or stale data - clearing local data")
                        clearLocalSubscription()
                        onSuccess?.invoke(SubscriptionStatus(status, endTime, autoRenewing))
                    }
                } else {
                    Log.d(TAG, "📭 No subscription data found in Firebase")
                    clearLocalSubscription()
                    onSuccess?.invoke(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "❌ Failed to restore subscription from Firebase: ${exception.message}")
                onFailure?.invoke(exception.message ?: "Unknown error")
            }
    }
    
    /**
     * Sync current subscription status to Firebase (for periodic updates)
     * This method is no longer directly used for paid subscriptions as backend is the source of truth.
     * Keep it for reward-based timer sync if needed, but remove subscription-related logic.
     */
    // Removed syncCurrentSubscriptionStatus as backend is the source of truth
    // fun syncCurrentSubscriptionStatus(
    //     onSuccess: (() -> Unit)? = null,
    //     onFailure: ((String) -> Unit)? = null
    // ) {
    //    // ... old logic ...
    // }
    
    /**
     * Clear subscription data from Firebase
     */
    fun clearSubscriptionFromFirebase(
        onSuccess: (() -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) {
        val userId = authManager.getCurrentUserId()
        if (userId == null) {
            Log.w(TAG, "No user ID available for subscription clear")
            onFailure?.invoke("User not authenticated")
            return
        }
        
        Log.d(TAG, "🗑️ Clearing subscription data from Firebase")
        
        db.collection("users").document(userId)
            .collection("subscription")
            .document("current")
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "✅ Subscription data cleared from Firebase")
                clearLocalSubscription()
                onSuccess?.invoke()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "❌ Failed to clear subscription from Firebase: ${exception.message}")
                onFailure?.invoke(exception.message ?: "Unknown error")
            }
    }
    
    /**
     * Check if user has active subscription
     */
    fun hasActiveSubscription(): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val endTime = prefs.getLong(KEY_PRO_TIMER_END, 0L)
        val subscriptionType = prefs.getString(KEY_PRO_TIMER_TYPE, "")
        // Only consider active if it's a reward-based timer
        return endTime > System.currentTimeMillis() && subscriptionType != "subscription"
    }

    /**
     * Enforce local expiry based on end timestamp and optionally sync status to Firebase.
     * Returns true if subscription remains active after enforcement, false otherwise.
     * This method is primarily for reward-based timers or local temporary states.
     */
    fun enforceLocalExpiryAndSync(syncFirebase: Boolean = true): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val endTime = prefs.getLong(KEY_PRO_TIMER_END, 0L)
        val subscriptionType = prefs.getString(KEY_PRO_TIMER_TYPE, "")
        // For paid subscriptions, clear local state when expired
        if (subscriptionType == "subscription") {
            val isActive = endTime > System.currentTimeMillis()
            if (!isActive) {
                Log.d(TAG, "Paid subscription expired locally, clearing local data")
                clearLocalSubscription()
                return false
            }
            return true
        }
        // Only consider reward-based timer
        val isActive = endTime > System.currentTimeMillis()
        if (!isActive) {
            Log.d(TAG, "⏰ Local reward entitlement expired, clearing local data")
            clearLocalSubscription()
            return false
        }
        return true
    }
    
    /**
     * Get subscription end time (for reward-based timer)
     */
    fun getSubscriptionEndTime(): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_PRO_TIMER_END, 0L)
    }
    
    /**
     * Get subscription type (for reward-based timer)
     */
    fun getSubscriptionType(): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PRO_TIMER_TYPE, "") ?: ""
    }

    fun isLocalPaidSubscriptionActive(): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val end = prefs.getLong(KEY_PRO_TIMER_END, 0L)
        val type = prefs.getString(KEY_PRO_TIMER_TYPE, "") ?: ""
        return type == "subscription" && end > System.currentTimeMillis()
    }

    fun getLocalPaidEndTime(): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val type = prefs.getString(KEY_PRO_TIMER_TYPE, "") ?: ""
        return if (type == "subscription") prefs.getLong(KEY_PRO_TIMER_END, 0L) else 0L
    }
    
    /**
     * Clear local subscription data
     */
    fun clearLocalSubscription() {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_PRO_TIMER_END, 0L)
            .putString(KEY_PRO_TIMER_TYPE, "")
            .apply()
        Log.d(TAG, "🗑️ Local subscription data cleared")
    }
    
    /**
     * Get readable date from timestamp
     */
    private fun getReadableDate(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * Get user-friendly device ID
     */
    private fun getUserFriendlyDeviceId(context: Context): String {
        return try {
            android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            "unknown_device"
        }
    }
    
    /**
     * Get app version
     */
    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun updateLocalSubscriptionStatus(endTime: Long, isActive: Boolean, autoRenewing: Boolean? = null) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_PRO_TIMER_END, endTime)
            .putString(KEY_PRO_TIMER_TYPE, if (isActive) "subscription" else "") // Store 'subscription' type for paid
            .apply()
        Log.d(TAG, "Local shared preference updated: EndTime=$endTime, IsActive=$isActive")
    }
}
