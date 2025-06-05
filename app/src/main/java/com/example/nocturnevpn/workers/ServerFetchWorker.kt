package com.example.nocturnevpn.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nocturnevpn.BuildConfig
import com.example.nocturnevpn.db.DbHelper
import com.example.nocturnevpn.utils.CsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ServerFetchWorker (
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val okHttpClient = OkHttpClient()
    private val dbHelper = DbHelper.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        // Check if this is a manual trigger
        val isManualTrigger = inputData.getBoolean("is_manual_trigger", false)
        val triggerType = if (isManualTrigger) "MANUAL" else "AUTOMATIC"

        Log.d("ServerFetchWorker", "🔄 [$triggerType] Starting server fetch at $currentTime")

        try {
            val request = Request.Builder()
                .url(BuildConfig.VPN_GATE_API)
                .build()

            Log.d("ServerFetchWorker", "📡 [$triggerType] Making API request to ${BuildConfig.VPN_GATE_API}")
            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val servers = CsvParser.parse(response)
                Log.d("ServerFetchWorker", "✅ [$triggerType] Parsed ${servers.size} servers from response")

                dbHelper.save(servers, context)
                Log.d("ServerFetchWorker", "💾 [$triggerType] Successfully saved servers to database")

                Result.success()
            } else {
                Log.e("ServerFetchWorker", "❌ [$triggerType] Failed to fetch server data: ${response.code}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("ServerFetchWorker", "❌ [$triggerType] Error fetching server data", e)
            Result.retry()
        }

    }


}