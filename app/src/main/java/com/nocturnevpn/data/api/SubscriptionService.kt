package com.nocturnevpn.data.api

import com.nocturnevpn.data.model.SubscriptionVerificationRequest
import com.nocturnevpn.data.model.SubscriptionVerificationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SubscriptionService {
    @POST("validate-subscription")
    suspend fun verifySubscription(
        @Body request: SubscriptionVerificationRequest
    ): Response<SubscriptionVerificationResponse>
}
