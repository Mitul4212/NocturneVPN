package com.example.nocturnevpn.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nocturnevpn.R
import com.example.nocturnevpn.adapter.HistoryAdapter
import com.example.nocturnevpn.databinding.FragmentHistoryBinding
import com.example.nocturnevpn.db.HistoryHelper
import com.example.nocturnevpn.model.History

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var historyHelper: HistoryHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        loadHistory()
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
        AlertDialog.Builder(requireContext())
            .setTitle("Clear History")
            .setMessage("Are you sure you want to clear all connection history? This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                clearHistory()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearHistory() {
        historyHelper.clearHistory()
        loadHistory()
        Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        com.example.nocturnevpn.utils.RatingDialogManager.maybeShowRatingDialog(requireActivity())
    }
} 