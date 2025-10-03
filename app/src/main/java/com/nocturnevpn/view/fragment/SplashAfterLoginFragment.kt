package com.nocturnevpn.view.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.nocturnevpn.R
import com.nocturnevpn.utils.SubscriptionSyncManager
import com.nocturnevpn.data.repository.SubscriptionRepository
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.QueryPurchasesParams
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SplashAfterLoginFragment : Fragment() {

    private val timeoutMs = 20000L // 20s
    private var finished = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash_after_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("SplashAfterLogin", "onViewCreated: post-login verification splash shown")
        val btnContinue = view.findViewById<MaterialButton>(R.id.btnContinue)
        val status = view.findViewById<TextView>(R.id.status)

        btnContinue.setOnClickListener { navigateToHome() }

        // Start verification
        startVerification(status, btnContinue)

        // Timeout fallback
        Handler(Looper.getMainLooper()).postDelayed({
            if (!finished) {
                android.util.Log.w("SplashAfterLogin", "Verification timeout reached → showing Continue button")
                btnContinue.visibility = View.VISIBLE
                status.text = "Verification is taking longer than expected. You can continue and it will finish in background."
            }
        }, timeoutMs)
    }

    private fun startVerification(statusView: TextView, btnContinue: MaterialButton) {
        android.util.Log.d("SplashAfterLogin", "startVerification() invoked")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val context = requireContext().applicationContext
                val billing = BillingClient.newBuilder(context).enablePendingPurchases().setListener { _, _ -> }.build()
                var purchases: List<com.android.billingclient.api.Purchase> = emptyList()

                val setup = kotlinx.coroutines.suspendCancellableCoroutine<Int> { cont ->
                    billing.startConnection(object : BillingClientStateListener {
                        override fun onBillingSetupFinished(result: BillingResult) {
                            cont.resume(result.responseCode, onCancellation = null)
                        }
                        override fun onBillingServiceDisconnected() { cont.resume(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, onCancellation = null) }
                    })
                }

                if (setup == BillingClient.BillingResponseCode.OK) {
                    android.util.Log.d("SplashAfterLogin", "Billing setup OK. Querying purchases...")
                    val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
                    val res = kotlinx.coroutines.suspendCancellableCoroutine<Pair<com.android.billingclient.api.BillingResult, List<com.android.billingclient.api.Purchase>>> { cont ->
                        billing.queryPurchasesAsync(params) { br, list -> cont.resume(br to list, onCancellation = null) }
                    }
                    purchases = res.second
                    android.util.Log.d("SplashAfterLogin", "Purchases fetched: count=" + purchases.size)
                }
                billing.endConnection()

                val repo = SubscriptionRepository(SubscriptionRepository.subscriptionService)
                val sync = SubscriptionSyncManager.getInstance(requireContext())
                val pkg = requireContext().packageName
                var anySuccess = false

                // Get current user account information for subscription binding
                val authManager = com.nocturnevpn.utils.AuthManager.getInstance(requireContext())
                val userEmail = authManager.getCurrentUserEmail()
                val userId = authManager.getCurrentUserId()
                
                android.util.Log.d("SplashAfterLogin", "Verifying subscription for user: email=$userEmail, userId=$userId")

                for (p in purchases) {
                    if (p.products.isEmpty()) continue
                    val productId = p.products.first()
                    val token = p.purchaseToken
                    val vr = repo.checkSubscription(pkg, productId, token, userEmail, userId)
                    vr.onSuccess { stat ->
                        android.util.Log.d("SplashAfterLogin", "Verified subscription: product=" + productId + ", expiry=" + stat.expiryTimeMillis + ", status=" + stat.status)
                        
                        // Only count as success if subscription is actually active
                        if (stat.status == "active" && stat.expiryTimeMillis > System.currentTimeMillis()) {
                            anySuccess = true
                            android.util.Log.d("SplashAfterLogin", "✅ Active subscription found for this user")
                        } else {
                            android.util.Log.d("SplashAfterLogin", "❌ Subscription inactive/expired for this user")
                        }
                        
                        sync.saveBackendVerifiedSubscription(stat, productId, token, verifySource = "splash")
                    }
                }

                // If no active purchases found, clear any stale subscription data
                if (!anySuccess && purchases.isEmpty()) {
                    android.util.Log.d("SplashAfterLogin", "No active purchases found, clearing stale subscription data")
                    sync.clearSubscriptionFromFirebase(
                        onSuccess = { android.util.Log.d("SplashAfterLogin", "Stale subscription data cleared from Firebase") },
                        onFailure = { error -> android.util.Log.w("SplashAfterLogin", "Failed to clear stale data: $error") }
                    )
                    sync.clearLocalSubscription()
                }

                requireActivity().runOnUiThread {
                    android.util.Log.d("SplashAfterLogin", "Verification completed. Navigating to Home. anySuccess=" + anySuccess)
                    finished = true
                    
                    // Force refresh of UI components by sending broadcast or calling activity method
                    if (activity is com.nocturnevpn.view.activitys.HomeActivity) {
                        (activity as com.nocturnevpn.view.activitys.HomeActivity).checkAuthState()
                    }
                    
                    navigateToHome()
                }
            } catch (_: Exception) {
                requireActivity().runOnUiThread {
                    android.util.Log.e("SplashAfterLogin", "Verification failed due to exception; continuing to Home")
                    finished = true
                    navigateToHome()
                }
            }
        }
    }

    private fun navigateToHome() {
        android.util.Log.d("SplashAfterLogin", "navigateToHome() called")
        if (findNavController().currentDestination?.id == R.id.splashAfterLoginFragment) {
            val navOptions = NavOptions.Builder().setPopUpTo(R.id.homeFragment, false).build()
            findNavController().navigate(R.id.homeFragment, null, navOptions)
        }
    }
}


