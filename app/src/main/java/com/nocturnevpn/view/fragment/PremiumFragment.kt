package com.nocturnevpn.view.fragment

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.nocturnevpn.R
import com.nocturnevpn.data.repository.SubscriptionRepository
import com.nocturnevpn.databinding.FragmentPremiumBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PremiumFragment : Fragment(), PurchasesUpdatedListener {

    private var _binding: FragmentPremiumBinding? = null
    private val binding get() = _binding

    // Billing
    private var billingClient: BillingClient? = null
    private var monthlyProduct: ProductDetails? = null
    private var yearlyProduct: ProductDetails? = null

    // Subscription sync
    private lateinit var subscriptionRepository: SubscriptionRepository
    
    // Loading dialog
    private var loadingDialog: AlertDialog? = null
    
    companion object {
        // Static reference to current loading dialog for cleanup from other fragments
        @Volatile
        private var currentLoadingDialog: AlertDialog? = null
        
        fun hideAnyLoadingDialog() {
            currentLoadingDialog?.let { dialog ->
                try {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    // Ignore errors during cleanup
                }
                currentLoadingDialog = null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPremiumBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.close?.setOnClickListener{
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Initialize subscription repository
        subscriptionRepository = SubscriptionRepository(SubscriptionRepository.subscriptionService)

        // Initialize billing and query products
        initBillingClient()

        // Update UI initially (e.g., if already subscribed)
        updateSubscriptionUI()

        // Wire buttons
        view.findViewById<View>(R.id.cardView4)?.setOnClickListener {
            launchSubscription(monthly = true)
        }
        view.findViewById<View>(R.id.cardView5)?.setOnClickListener {
            launchSubscription(monthly = false)
        }
        view.findViewById<View>(R.id.cardView7)?.setOnClickListener {
            // TRY PREMIUM FREE -> monthly product (free trial configured in Play Console)
            launchSubscription(monthly = true)
        }
    }

    override fun onResume() {
        super.onResume()
        // Restore entitlement if user already has an active subscription
        queryActiveSubscriptionsAndPersist()
        // Also restore from Firebase to sync UI for other parts of app
        val sync = com.nocturnevpn.utils.SubscriptionSyncManager.getInstance(requireContext())
        if (com.nocturnevpn.utils.AuthManager.getInstance(requireContext()).isUserSignedIn()) {
            sync.restoreSubscriptionFromFirebase(
                onSuccess = { status -> Log.d("PremiumFragment", "Subscription restored onResume: $status") },
                onFailure = { e -> Log.w("PremiumFragment", "Failed to restore subscription onResume: $e") }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        billingClient?.endConnection()
        _binding = null
    }

    // === Billing setup ===
    private fun initBillingClient() {
        val context = requireContext().applicationContext
        billingClient = BillingClient.newBuilder(context)
            .enablePendingPurchases()
            .setListener(this)
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    // also check existing purchases
                    queryActiveSubscriptionsAndPersist()
                } else {
                    Log.w("PremiumFragment", "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to reconnect automatically on next action
                Log.w("PremiumFragment", "Billing service disconnected")
            }
        })
    }

    private fun queryProductDetails() {
        val products = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("sub_monthly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("sub_yearly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                monthlyProduct = productDetailsList.firstOrNull { it.productId == "sub_monthly" }
                yearlyProduct = productDetailsList.firstOrNull { it.productId == "sub_yearly" }
                Log.d("PremiumFragment", "Products loaded: monthly=${monthlyProduct != null}, yearly=${yearlyProduct != null}")
            } else {
                Log.w("PremiumFragment", "Product query failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun launchSubscription(monthly: Boolean) {
        val activity = activity as? Activity ?: return
        val product = if (monthly) monthlyProduct else yearlyProduct
        if (billingClient?.isReady != true || product == null) {
            Log.w("PremiumFragment", "Billing not ready or product null; retrying query")
            queryProductDetails()
            return
        }

        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            Log.w("PremiumFragment", "No offerToken for ${product.productId}")
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        billingClient?.launchBillingFlow(activity, flowParams)
    }

    // === Purchases callback ===
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d("PremiumFragment", "Purchase canceled by user")
        } else {
            Log.w("PremiumFragment", "Purchase failed: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.products.isEmpty()) return
        val isAcknowledged = purchase.isAcknowledged
        val productId = purchase.products.first()
        val purchaseToken = purchase.purchaseToken

        // Log purchase token for debugging
        Log.d("PremiumFragment", "🛒 Purchase received - Product: $productId")
        Log.d("PremiumFragment", "🔑 Purchase Token: $purchaseToken")
        Log.d("PremiumFragment", "📱 Package Name: ${purchase.packageName}")
        Log.d("PremiumFragment", "✅ Acknowledged: $isAcknowledged")

        // Acknowledge if needed
        if (!isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
            billingClient?.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("PremiumFragment", "✅ Purchase acknowledged: $productId")
                    grantEntitlement(productId, purchaseToken)
                } else {
                    Log.w("PremiumFragment", "❌ Acknowledge failed: ${result.debugMessage}")
                }
            }
        } else {
            grantEntitlement(productId, purchaseToken)
        }
    }

    private fun grantEntitlement(productId: String, purchaseToken: String) {
        Log.d("PremiumFragment", "🔐 Starting entitlement process for product: $productId")
        
        // Show loading dialog on main thread
        requireActivity().runOnUiThread {
            if (isAdded && activity != null) {
                showLoadingDialog()
            }
        }
        
        // Launch a coroutine to handle the backend call
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val packageName = requireContext().packageName
                
                // Get current user account information for subscription binding
                val authManager = com.nocturnevpn.utils.AuthManager.getInstance(requireContext())
                val userEmail = authManager.getCurrentUserEmail()
                val userId = authManager.getCurrentUserId()
                
                Log.d("PremiumFragment", "🌐 Making backend verification call for $productId")
                Log.d("PremiumFragment", "🔐 Binding subscription to user:")
                Log.d("PremiumFragment", "   📧 userEmail: $userEmail")
                Log.d("PremiumFragment", "   🆔 userId: $userId")
                Log.d("PremiumFragment", "   📦 packageName: $packageName")
                Log.d("PremiumFragment", "   🎫 purchaseToken: ${purchaseToken.take(20)}...")
                
                val result = subscriptionRepository.checkSubscription(packageName, productId, purchaseToken, userEmail, userId)
                Log.d("PremiumFragment", "🌐 Backend call completed, processing result...")

            result.onSuccess { subscriptionStatus ->
                // Handle successful verification
                Log.d("PremiumFragment", "✅ Subscription verified in grantEntitlement: $subscriptionStatus")
                
                // Check if fragment is still attached before proceeding
                if (!isAdded || activity == null) {
                    Log.w("PremiumFragment", "⚠️ Fragment not attached, skipping UI updates")
                    return@onSuccess
                }
                
                // Persist backend verdict so restore uses latest
                val sync = com.nocturnevpn.utils.SubscriptionSyncManager.getInstance(requireContext())
                sync.saveBackendVerifiedSubscription(subscriptionStatus, productId, purchaseToken, verifySource = "premium-verify")

                requireActivity().runOnUiThread {
                    // Always try to hide loading dialog first, even if fragment is detaching
                    hideLoadingDialog()
                    
                    // Then check fragment attachment for other UI updates
                    if (!isAdded || activity == null) {
                        Log.w("PremiumFragment", "⚠️ Fragment not attached in UI thread, skipping navigation")
                        return@runOnUiThread
                    }
                    if (subscriptionStatus.status == "active" && subscriptionStatus.expiryTimeMillis > System.currentTimeMillis()) {
                        // Already persisted to authoritative snapshot above and to Firebase below
                        // Navigate to AfterPremium regardless of current dest if we're still in the flow
                        try {
                            val navOptions = NavOptions.Builder()
                                .setPopUpTo(R.id.premiumFragment, true)
                                .build()
                            findNavController().navigate(R.id.afterPremiumFragment, null, navOptions)
                        } catch (e: Exception) {
                            Log.e("PremiumFragment", "❌ Navigation error: ${e.message}", e)
                        }
                    } else {
                        Toast.makeText(requireContext(), "Subscription is not active or has expired", Toast.LENGTH_LONG).show()
                    }
                }
            }.onFailure { e ->
                // Handle verification failure
                Log.e("PremiumFragment", "❌ Subscription verification failed in grantEntitlement: ${e.message}")
                
                // Check if fragment is still attached before UI updates
                if (!isAdded || activity == null) {
                    Log.w("PremiumFragment", "⚠️ Fragment not attached, skipping error UI updates")
                    return@onFailure
                }
                
                requireActivity().runOnUiThread { // Update UI on main thread
                    // Always try to hide loading dialog first, even if fragment is detaching
                    hideLoadingDialog()
                    
                    // Then check fragment attachment for other UI updates
                    if (!isAdded || activity == null) {
                        Log.w("PremiumFragment", "⚠️ Fragment not attached in error UI thread, skipping toast")
                        return@runOnUiThread
                    }
                    Toast.makeText(requireContext(), "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            } catch (e: Exception) {
                Log.e("PremiumFragment", "❌ Exception in grantEntitlement coroutine: ${e.message}", e)
                requireActivity().runOnUiThread {
                    if (isAdded && activity != null) {
                        hideLoadingDialog()
                        Toast.makeText(requireContext(), "Verification error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    
    private fun showLoadingDialog() {
        if (loadingDialog?.isShowing == true) {
            Log.d("PremiumFragment", "🔄 Loading dialog already showing, skipping")
            return
        }
        
        Log.d("PremiumFragment", "🔄 Showing loading dialog for subscription verification")
        
        try {
            // Check if fragment is still attached
            if (!isAdded || activity == null) {
                Log.w("PremiumFragment", "⚠️ Fragment not attached, cannot show loading dialog")
                return
            }
            
            val builder = AlertDialog.Builder(requireContext())
            builder.setView(R.layout.dialog_loading)
            builder.setCancelable(false)
            loadingDialog = builder.create()
            
            Log.d("PremiumFragment", "🔄 About to show dialog...")
            loadingDialog?.show()
            
            // Store static reference for cleanup from other fragments
            currentLoadingDialog = loadingDialog
            
            Log.d("PremiumFragment", "✅ Loading dialog displayed successfully")
            
        } catch (e: Exception) {
            Log.e("PremiumFragment", "❌ Failed to show loading dialog: ${e.message}", e)
        }
    }
    
    private fun hideLoadingDialog() {
        Log.d("PremiumFragment", "🔄 Hiding loading dialog")
        try {
            loadingDialog?.dismiss()
            loadingDialog = null
            
            // Clear static reference
            currentLoadingDialog = null
            
            Log.d("PremiumFragment", "✅ Loading dialog hidden")
        } catch (e: Exception) {
            Log.w("PremiumFragment", "⚠️ Error hiding loading dialog: ${e.message}")
            loadingDialog = null
            currentLoadingDialog = null
        }
    }
    
    private fun saveVerifiedSubscriptionToFirebase(subscriptionStatus: com.nocturnevpn.data.model.SubscriptionStatus) {
        // Save to local SharedPreferences for immediate access
        val prefs = requireContext().getSharedPreferences("reward_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("pro_timer_end", subscriptionStatus.expiryTimeMillis)
            .putString("pro_timer_type", "subscription")
            .apply()
            
        Log.d("PremiumFragment", "✅ Verified subscription saved locally: expiry=${subscriptionStatus.expiryTimeMillis}")
    }

    // The updateSubscriptionUI method should only reflect temporary UI state in PremiumFragment, not persistent subscription status.
    private fun updateSubscriptionUI() {
        // The UI in PremiumFragment should reflect the initial state or loading, not verified subscription status.
        // It should not read from local preferences related to KEY_PRO_TIMER_END for paid subscriptions.
        // This method can be simplified or removed if not needed for displaying temporary states.
//        binding?.tvSubscriptionStatus?.text = ""
//        binding?.tvSubscriptionExpiry?.text = ""
    }

    private fun queryActiveSubscriptionsAndPersist() {
        val client = billingClient ?: return
        if (!client.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        client.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w("PremiumFragment", "Query purchases failed: ${billingResult.debugMessage}")
                return@queryPurchasesAsync
            }

            val hasActiveSub = purchasesList.any { purchase ->
                purchase.products.any { it == "sub_monthly" || it == "sub_yearly" }
            }

            // Iterate through purchases and verify each with the backend if there are any active subscriptions
            if (hasActiveSub) {
                CoroutineScope(Dispatchers.IO).launch {
                    for (purchase in purchasesList) {
                        if (purchase.products.isNotEmpty()) {
                            val productId = purchase.products.first()
                            val purchaseToken = purchase.purchaseToken
                            
                            // Check if fragment is still attached before using requireContext()
                            if (!isAdded || activity == null) {
                                Log.w("PremiumFragment", "⚠️ Fragment not attached, skipping re-verification for $productId")
                                continue
                            }
                            
                            val packageName = requireContext().packageName
                            
                            // Get current user account information for subscription binding
                            val authManager = com.nocturnevpn.utils.AuthManager.getInstance(requireContext())
                            val userEmail = authManager.getCurrentUserEmail()
                            val userId = authManager.getCurrentUserId()
                            
                            Log.d("PremiumFragment", "🔄 Re-verifying subscription for user:")
                            Log.d("PremiumFragment", "   📧 userEmail: $userEmail")
                            Log.d("PremiumFragment", "   🆔 userId: $userId")
                            Log.d("PremiumFragment", "   🎯 productId: $productId")
                            Log.d("PremiumFragment", "   🎫 purchaseToken: ${purchaseToken.take(20)}...")

                            val result = subscriptionRepository.checkSubscription(packageName, productId, purchaseToken, userEmail, userId)
                            result.onSuccess { subscriptionStatus ->
                                Log.d("PremiumFragment", "Re-verification success for $productId: $subscriptionStatus")
                                
                                // Check if fragment is still attached before proceeding
                                if (!isAdded || activity == null) {
                                    Log.w("PremiumFragment", "⚠️ Fragment not attached, skipping re-verification persistence for $productId")
                                    return@onSuccess
                                }
                                
                                // Persist backend snapshot so UI elsewhere updates
                                val sync = com.nocturnevpn.utils.SubscriptionSyncManager.getInstance(requireContext())
                                sync.saveBackendVerifiedSubscription(subscriptionStatus, productId, purchaseToken, verifySource = "requery")
                                // If still on Premium page and status active, navigate user forward
                                if (subscriptionStatus.status == "active" && subscriptionStatus.expiryTimeMillis > System.currentTimeMillis()) {
                                    // Check if fragment is still attached before UI operations
                                    if (!isAdded || activity == null) {
                                        Log.w("PremiumFragment", "⚠️ Fragment not attached, skipping auto-navigation")
                                        return@onSuccess
                                    }
                                    
                                    requireActivity().runOnUiThread {
                                        // Double-check fragment attachment in UI thread
                                        if (!isAdded || activity == null) {
                                            Log.w("PremiumFragment", "⚠️ Fragment not attached in auto-nav UI thread, skipping")
                                            return@runOnUiThread
                                        }
                                        
                                        try {
                                            if (findNavController().currentDestination?.id == R.id.premiumFragment) {
                                                val navOptions = NavOptions.Builder()
                                                    .setPopUpTo(R.id.premiumFragment, true)
                                                    .build()
                                                findNavController().navigate(R.id.afterPremiumFragment, null, navOptions)
                                            }
                                        } catch (e: Exception) {
                                            Log.e("PremiumFragment", "❌ Auto-navigation error: ${e.message}", e)
                                        }
                                    }
                                }
                            }.onFailure { e ->
                                Log.e("PremiumFragment", "Re-verification failed for $productId: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }
}