package com.nocturnevpn.data.repository

import com.nocturnevpn.data.api.SubscriptionService
import com.nocturnevpn.data.model.SubscriptionStatus
import com.nocturnevpn.data.model.SubscriptionVerificationRequest
import com.nocturnevpn.data.model.SubscriptionVerificationResponse
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class SubscriptionRepository(private val subscriptionService: SubscriptionService) {

    companion object {
        private const val TAG = "SubscriptionRepository"
        private const val BASE_URL = "https://google-play-server.onrender.com/"

        // Retrofit client setup
        private val retrofitClient: Retrofit by lazy {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val httpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build()
        }

        val subscriptionService: SubscriptionService by lazy {
            retrofitClient.create(SubscriptionService::class.java)
        }
    }

    suspend fun checkSubscription(
        packageName: String,
        subscriptionId: String,
        purchaseToken: String,
        userEmail: String? = null,
        userId: String? = null
    ): Result<SubscriptionStatus> {
        return try {
            val request = SubscriptionVerificationRequest(
                packageName = packageName,
                subscriptionId = subscriptionId,
                purchaseToken = purchaseToken,
                userEmail = userEmail,
                userId = userId
            )
            val response = subscriptionService.verifySubscription(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d(TAG, "Backend response: $body")
                val expiryTime = body.expiryTimeMillis.toLongOrNull() ?: 0L

                Result.success(SubscriptionStatus(
                    status = body.status,
                    expiryTimeMillis = expiryTime,
                    autoRenewing = body.autoRenewing
                ))
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Backend verification failed: ${response.code()} - $errorBody")
                Result.failure(Exception(errorBody ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking subscription with backend: ${e.message}", e)
            Result.failure(e)
        }
    }
}
