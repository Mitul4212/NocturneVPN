package com.nocturnevpn.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nocturnevpn.utils.SubscriptionSyncManager

class SubscriptionExpiryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val manager = SubscriptionSyncManager.getInstance(applicationContext)
            val active = manager.enforceLocalExpiryAndSync(syncFirebase = true)
            Log.d("SubscriptionExpiryWorker", "Enforced expiry. Active=$active")
            Result.success()
        } catch (e: Exception) {
            Log.e("SubscriptionExpiryWorker", "Error enforcing expiry: ${e.message}")
            Result.retry()
        }
    }
}


