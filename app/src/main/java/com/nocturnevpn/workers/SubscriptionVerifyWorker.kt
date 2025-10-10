package com.nocturnevpn.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.QueryPurchasesParams
import com.nocturnevpn.data.repository.SubscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs daily in background to verify active subscriptions with backend
 */
class SubscriptionVerifyWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "SubscriptionVerifyWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val context = applicationContext
            Log.d(TAG, "Worker started: verifying subscriptions in background")

            // Query Google Play for active subscriptions
            val billingClient = BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener { _, _ -> }
                .build()

            var purchases: List<Purchase> = emptyList()

            val setupResult = kotlinx.coroutines.suspendCancellableCoroutine<Int> { cont ->
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        cont.resume(billingResult.responseCode, onCancellation = null)
                    }
                    override fun onBillingServiceDisconnected() {
                        cont.resume(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, onCancellation = null)
                    }
                })
            }

            if (setupResult == BillingClient.BillingResponseCode.OK) {
                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
                val result = kotlinx.coroutines.suspendCancellableCoroutine<Pair<com.android.billingclient.api.BillingResult, List<Purchase>>> { cont ->
                    billingClient.queryPurchasesAsync(params) { br, list ->
                        cont.resume(br to list, onCancellation = null)
                    }
                }
                Log.d(TAG, "Purchases fetched in worker: count=" + result.second.size)
                purchases = result.second
            } else {
                Log.w(TAG, "Billing setup failed: $setupResult")
            }

            billingClient.endConnection()

            if (purchases.isEmpty()) {
                Log.d(TAG, "No active Play purchases found to verify - clearing stale subscription data")
                val sync = com.nocturnevpn.utils.SubscriptionSyncManager.getInstance(context)
                sync.clearSubscriptionFromFirebase(
                    onSuccess = { Log.d(TAG, "Stale subscription data cleared from Firebase") },
                    onFailure = { error -> Log.w(TAG, "Failed to clear stale data: $error") }
                )
                sync.clearLocalSubscription()
                return@withContext Result.success()
            }

            val repo = SubscriptionRepository(SubscriptionRepository.subscriptionService)
            val packageName = context.packageName
            val sync = com.nocturnevpn.utils.SubscriptionSyncManager.getInstance(context)
            
            // Get current user account information for subscription binding
            val authManager = com.nocturnevpn.utils.AuthManager.getInstance(context)
            val userEmail = authManager.getCurrentUserEmail()
            val userId = authManager.getCurrentUserId()
            
            Log.d(TAG, "Worker verifying subscription for user: email=$userEmail, userId=$userId")
            
            for (purchase in purchases) {
                if (purchase.products.isEmpty()) continue
                val productId = purchase.products.first()
                val token = purchase.purchaseToken
                try {
                    val res = repo.checkSubscription(packageName, productId, token, userEmail, userId)
                    res.onSuccess { status ->
                        Log.d(TAG, "Verified $productId: $status")
                        
                        // Only persist if subscription is actually active
                        if (status.status == "active" && status.expiryTimeMillis > System.currentTimeMillis()) {
                            Log.d(TAG, "✅ Active subscription found for current user - persisting")
                            sync.saveBackendVerifiedSubscription(status, productId, token, verifySource = "worker")
                        } else {
                            Log.d(TAG, "❌ Subscription inactive/expired for current user - clearing local data")
                            // Clear local subscription data if backend says inactive
                            sync.clearLocalSubscription()
                        }
                    }.onFailure { e ->
                        Log.w(TAG, "Verification failed for $productId: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error verifying $productId: ${e.message}")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker error: ${e.message}")
            Result.retry()
        }
    }
}


