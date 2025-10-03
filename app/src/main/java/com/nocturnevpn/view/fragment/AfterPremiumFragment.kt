package com.nocturnevpn.view.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.nocturnevpn.data.model.SubscriptionStatus
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.google.android.material.button.MaterialButton
import com.nocturnevpn.R
import com.nocturnevpn.utils.AnimatedBorderManager
import com.nocturnevpn.utils.SubscriptionSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AfterPremiumFragment : Fragment(), PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_after_premium, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("AfterPremiumFragment", "onViewCreated: fragment shown")
        
        // Clean up any remaining loading dialog from PremiumFragment
        PremiumFragment.hideAnyLoadingDialog()
        Log.d("AfterPremiumFragment", "Cleaned up any remaining loading dialogs")
        val premiumMemberText = view.findViewById<TextView>(R.id.tvPremiumMember)
        premiumMemberText?.let { textView ->
            val paint = textView.paint
            val width = paint.measureText(textView.text.toString())
            textView.paint.shader = LinearGradient(
                0f, 0f, width, textView.textSize,
                intArrayOf(Color.parseColor("#6622CC"), Color.parseColor("#22CCC2")),
                null, Shader.TileMode.CLAMP
            )
            textView.invalidate()
        }

        // Close button
        view.findViewById<View>(R.id.close)?.setOnClickListener {
            Log.d("AfterPremiumFragment", "Close clicked → navigating back to Home")
            navigateBackToHome()
        }

        // Trigger animated border when user completes premium purchase
        val animatedBorderManager = AnimatedBorderManager.getInstance(requireContext())
        animatedBorderManager.setShouldShowAfterNavigation(true)
        Log.d("AfterPremiumFragment", "Animated border flag set to show after navigation")

        // Setup Manage Subscription button
        view.findViewById<MaterialButton>(R.id.btnManageSubscription)?.setOnClickListener {
            // Open Play subscription center for this app
            val uri = Uri.parse("https://play.google.com/store/account/subscriptions")
            try {
                Log.d("AfterPremiumFragment", "Opening Play subscription center")
                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
            } catch (_: Exception) {}
        }

        // We no longer verify backend here to avoid repeated calls on revisit.
        // If needed, background worker will sync daily. Display basic info only.

        // Ensure system back goes to Home, not back to Premium
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToHome()
            }
        })

        // Load and display backend-verified subscription state from Firebase with a local fallback
        loadAndDisplaySubscriptionStatus()
    }

    // No backend calls here; worker updates status in background

    override fun onDestroyView() {
        super.onDestroyView()
        billingClient?.endConnection()
    }

    // Removed backend verification here; background worker handles it daily

    private fun updateAfterPremiumUI(status: String, expiryTime: Long) {
        val view = view ?: return
        val planText = view.findViewById<TextView>(R.id.tvCurrentPlan)
        val statusText = view.findViewById<TextView>(R.id.tvStatus)
        val renewText = view.findViewById<TextView>(R.id.tvRenewsOn)

        // Combine rules: treat local paid as active; otherwise require future expiry
        val sync = SubscriptionSyncManager.getInstance(requireContext())
        val localPaid = sync.isLocalPaidSubscriptionActive()
        val now = System.currentTimeMillis()
        val isActive = localPaid || (status == "active" && expiryTime > now)
        val effectiveEnd = if (localPaid) sync.getLocalPaidEndTime() else expiryTime

        if (isActive) {
            planText?.text = "Premium Active"
            statusText?.text = "Active"
            try { statusText?.setTextColor(requireContext().getColor(R.color.success_green_premium)) } catch (_: Exception) {}
            if (effectiveEnd > 0) {
                val fmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                renewText?.text = fmt.format(Date(effectiveEnd))
            }
        } else {
            planText?.text = "No Active Premium"
            statusText?.text = "Inactive"
            try { statusText?.setTextColor(requireContext().getColor(R.color.red)) } catch (_: Exception) {}
            renewText?.text = "N/A"
        }
    }

    private fun loadAndDisplaySubscriptionStatus() {
        val ctx = requireContext()
        val sync = SubscriptionSyncManager.getInstance(ctx)
        // Prefer local paid state for quick UX, then refresh from Firebase
        val localPaid = sync.isLocalPaidSubscriptionActive()
        if (localPaid) {
            val end = sync.getLocalPaidEndTime()
            updateAfterPremiumUI("active", end)
        }
        // Then fetch Firebase (source of truth)
        sync.restoreSubscriptionFromFirebase(
            onSuccess = { sub: SubscriptionStatus? ->
                if (sub != null) {
                    updateAfterPremiumUI(sub.status, sub.expiryTimeMillis)
                } else {
                    // Fallback to local cache for quick UX
                    val prefs = ctx.getSharedPreferences("reward_prefs", Context.MODE_PRIVATE)
                    val end = prefs.getLong("pro_timer_end", 0L)
                    val type = prefs.getString("pro_timer_type", "") ?: ""
                    val status = if (type == "subscription" && end > System.currentTimeMillis()) "active" else "inactive"
                    updateAfterPremiumUI(status, end)
                }
            },
            onFailure = {
                // On failure, fallback to local cache
                val prefs = ctx.getSharedPreferences("reward_prefs", Context.MODE_PRIVATE)
                val end = prefs.getLong("pro_timer_end", 0L)
                val type = prefs.getString("pro_timer_type", "") ?: ""
                val status = if (type == "subscription" && end > System.currentTimeMillis()) "active" else "inactive"
                updateAfterPremiumUI(status, end)
            }
        )
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        // No-op; backend verification runs in background worker now
    }

    private fun navigateBackToHome() {
        Log.d("AfterPremiumFragment", "navigateBackToHome() called")
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.homeFragment, false)
            .build()
        findNavController().navigate(R.id.homeFragment, null, navOptions)
    }
}