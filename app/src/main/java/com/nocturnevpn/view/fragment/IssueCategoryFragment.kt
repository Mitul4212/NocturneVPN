package com.nocturnevpn.view.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nocturnevpn.R
import com.nocturnevpn.databinding.FragmentIssueCategoryBinding

class IssueCategoryFragment : Fragment() {
    private var _binding: FragmentIssueCategoryBinding? = null
    private val binding get() = _binding!!
    private var listener: OnIssueCategorySelectedListener? = null

    interface OnIssueCategorySelectedListener {
        fun onIssueCategorySelected(category: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnIssueCategorySelectedListener) {
            listener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentIssueCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val categories = listOf(
            "Connection Problem",
            "Slow Speed",
            "App Crash",
            "Payment Issue",
            "Login/Signup Issue",
            "Server Unavailable",
            "Streaming Not Working",
            "App UI/UX Issue",
            "Other"
        )
        val adapter = CategoryAdapter(categories) { category ->
            listener?.onIssueCategorySelected(category)
        }
        binding.rvIssueCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvIssueCategories.adapter = adapter
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    class CategoryAdapter(
        private val items: List<String>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text: TextView = view.findViewById(R.id.issue_catagory_text)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_issue_category, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.text.text = items[position]
            holder.itemView.setOnClickListener { onClick(items[position]) }
        }
        override fun getItemCount() = items.size
    }
} 