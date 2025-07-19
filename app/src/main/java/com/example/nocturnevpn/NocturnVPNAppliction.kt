package com.example.nocturnevpn

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.nocturnevpn.workers.ServerFetchWorker
import java.util.concurrent.TimeUnit

class NocturnVPNAppliction : Application() {

    override fun onCreate() {
        super.onCreate()
        setupPeriodicServerFetch()
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