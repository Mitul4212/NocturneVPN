package com.example.nocturnevpn.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nocturnevpn.R
import com.example.nocturnevpn.adapter.SimpleHistoryAdapter
import com.example.nocturnevpn.databinding.FragmentProfileBinding
import com.example.nocturnevpn.db.HistoryHelper
import com.example.nocturnevpn.utils.SampleDataGenerator
import com.example.nocturnevpn.view.activitys.AppAuthActivity


class profileFragment : Fragment() {

    private var _binding: FragmentProfileBinding?= null
    private val binding get() = _binding!!
    
    private lateinit var simpleHistoryAdapter: SimpleHistoryAdapter
    private lateinit var historyHelper: HistoryHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSimpleHistoryRecyclerView()
        setupClickListeners()
        loadRecentHistory()
        
        // Temporary: Add sample data generation for testing
        addSampleDataButton()
    }

    private fun setupSimpleHistoryRecyclerView() {
        simpleHistoryAdapter = SimpleHistoryAdapter()
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = simpleHistoryAdapter
        }
    }

    private fun setupClickListeners() {
        binding.upgradeButton.setOnClickListener {
            this.findNavController().navigate(R.id.action_profileFragment_to_premiumFragment)
        }

        binding.signInButton.setOnClickListener {
            val intent = Intent(requireContext(), AppAuthActivity::class.java)
            startActivity(intent)
        }

        binding.moreButton.setOnClickListener {
            // Navigate to HistoryFragment
            this.findNavController().navigate(R.id.action_profileFragment_to_historyFragment)
        }

        binding.goToPremiumButton.setOnClickListener {
            this.findNavController().navigate(R.id.action_profileFragment_to_premiumFragment)
        }
    }

    private fun loadRecentHistory() {
        historyHelper = HistoryHelper.getInstance(requireContext())
        val recentHistory = historyHelper.getRecentHistory(3)
        
        if (recentHistory.isEmpty()) {
            binding.noHistoryText.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
            binding.moreButton.visibility = View.GONE
        } else {
            binding.noHistoryText.visibility = View.GONE
            binding.historyRecyclerView.visibility = View.VISIBLE
            binding.moreButton.visibility = View.VISIBLE
            simpleHistoryAdapter.setHistoryList(recentHistory)
        }
    }

    // Temporary method for testing - remove this in production
    private fun addSampleDataButton() {
        binding.moreButton.setOnLongClickListener {
            SampleDataGenerator.generateSampleHistory(requireContext())
            loadRecentHistory()
            Toast.makeText(context, "Sample history data generated!", Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh history when returning from HistoryFragment
        loadRecentHistory()
    }

}