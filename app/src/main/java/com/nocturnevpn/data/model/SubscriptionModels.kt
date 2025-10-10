package com.nocturnevpn.data.model

// Request body for the subscription verification API
data class SubscriptionVerificationRequest(
    val packageName: String,
    val subscriptionId: String,
    val purchaseToken: String,
    val userEmail: String? = null, // App account email for subscription binding
    val userId: String? = null     // App account ID for subscription binding
)

// Response body from the subscription verification API
data class SubscriptionVerificationResponse(
    val status: String,
    val expiryTimeMillis: String, // Keep as String as per backend response example
    val autoRenewing: Boolean,
    val startTimeMillis: String,
    val cancelReason: Int?
)

// Simplified data class for app-level subscription status
data class SubscriptionStatus(
    val status: String, // e.g., "active", "inactive"
    val expiryTimeMillis: Long,
    val autoRenewing: Boolean
)
