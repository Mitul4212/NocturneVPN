package com.nocturnevpn.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.app.Dialog
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nocturnevpn.R
import com.nocturnevpn.adapter.HistoryAdapter
import com.nocturnevpn.databinding.FragmentHistoryBinding
import com.nocturnevpn.db.HistoryHelper
import com.nocturnevpn.model.History
import com.nocturnevpn.view.managers.BannerAdManager
import android.util.Log

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var historyHelper: HistoryHelper
    private lateinit var bannerAdManager: BannerAdManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        bannerAdManager = BannerAdManager.getInstance(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        loadHistory()
        initializeBannerAd()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }

        historyAdapter.setOnHistoryItemClickListener { history ->
            // Handle history item click if needed
            Toast.makeText(context, "Selected: ${history.serverName}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.clearHistoryFab.setOnClickListener {
            showClearHistoryDialog()
        }
    }

    private fun loadHistory() {
        historyHelper = HistoryHelper.getInstance(requireContext())
        val historyList = historyHelper.getAllHistory()
        
        if (historyList.isEmpty()) {
            binding.noHistoryLayout.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
        } else {
            binding.noHistoryLayout.visibility = View.GONE
            binding.historyRecyclerView.visibility = View.VISIBLE
            historyAdapter.setHistoryList(historyList)
        }
    }

    private fun showClearHistoryDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.clear_history_dialog)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Clear button
        val btnClear = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_clear)
        btnClear.setOnClickListener {
            clearHistory()
            dialog.dismiss()
        }

        // Cancel button
        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun clearHistory() {
        historyHelper.clearHistory()
        loadHistory()
        Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
    }

    /**
     * Initialize banner ad
     */
    private fun initializeBannerAd() {
        try {
            Log.d("HistoryFragment", "🚀 Initializing banner ad...")
            bannerAdManager.initializeBannerAd(
                binding.bannerAdView,
                onAdLoaded = {
                    Log.d("HistoryFragment", "✅ Banner ad loaded successfully")
                },
                onAdFailed = { error ->
                    Log.e("HistoryFragment", "❌ Banner ad failed to load: $error")
                }
            )
        } catch (e: Exception) {
            Log.e("HistoryFragment", "❌ Error initializing banner ad: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume banner ad
        try {
            bannerAdManager.resumeBannerAd(binding.bannerAdView)
        } catch (e: Exception) {
            Log.e("HistoryFragment", "❌ Error resuming banner ad: ${e.message}")
        }
        com.nocturnevpn.utils.RatingDialogManager.maybeShowRatingDialog(requireActivity())
    }

    override fun onPause() {
        super.onPause()
        // Pause banner ad
        try {
            bannerAdManager.pauseBannerAd(binding.bannerAdView)
        } catch (e: Exception) {
            Log.e("HistoryFragment", "❌ Error pausing banner ad: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Destroy banner ad
        try {
            bannerAdManager.destroyBannerAd(binding.bannerAdView)
        } catch (e: Exception) {
            Log.e("HistoryFragment", "❌ Error destroying banner ad: ${e.message}")
        }
        _binding = null
    }
} 