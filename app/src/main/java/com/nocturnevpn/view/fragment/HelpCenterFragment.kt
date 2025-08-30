package com.nocturnevpn.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nocturnevpn.R
import com.nocturnevpn.adapter.HelpCenterAdapter
import com.nocturnevpn.adapter.HelpCenterTopicAdapter
import com.nocturnevpn.databinding.FragmentHelpCenterBinding
import android.os.Build
import androidx.core.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import androidx.recyclerview.widget.RecyclerView
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.content.Context


class HelpCenterFragment : Fragment() {

    private var _binding : FragmentHelpCenterBinding? = null
    private val binding get() = _binding!!

    private lateinit var questions: Array<String>
    private lateinit var answers: Array<String>
    private lateinit var topics: Array<String>
    private lateinit var topicToFaqIndexes: Map<Int, List<Int>>
    private var selectedTopicIndex: Int? = null
    private lateinit var helpCenterAdapter: HelpCenterAdapter
    private lateinit var topicAdapter: HelpCenterTopicAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHelpCenterBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set status bar color to match header (if API 21+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity?.window?.statusBarColor = ContextCompat.getColor(requireContext(), R.color.strong_violet)
        }

        questions = resources.getStringArray(R.array.faq_questions)
        answers = resources.getStringArray(R.array.faq_answers)
        topics = resources.getStringArray(R.array.faq_topics)

        // Map topics to FAQ indexes (customize as needed)
        topicToFaqIndexes = mapOf(
            1 to listOf(0), // General
            2 to listOf(1, 13), // Safety & Privacy
            3 to listOf(2, 3), // How To
            4 to listOf(4, 7, 8), // Connection Issues
            5 to listOf(5, 6), // Servers
            6 to listOf(9, 10, 11, 12), // Subscription & Payment
            7 to listOf(14) // Streaming
        )

        // Setup topics RecyclerView
        topicAdapter = HelpCenterTopicAdapter(topics.toList()) { topicIndex ->
            selectedTopicIndex = if (topicIndex == 0) null else topicIndex
            filterFaqs()
        }
        binding.topicsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding.topicsRecyclerView.adapter = topicAdapter

        // Setup FAQ RecyclerView
        helpCenterAdapter = HelpCenterAdapter(questions, answers)
        binding.helpCenterRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.helpCenterRecyclerView.adapter = helpCenterAdapter
        binding.helpCenterRecyclerView.setPadding(0, 0, 0, 24)

        // Setup search bar
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterFaqs()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        // Clear search when clear button pressed
        binding.searchInputLayout.setEndIconOnClickListener {
            binding.searchEditText.setText("")
            binding.searchEditText.clearFocus()
            filterFaqs()
        }

        // Hide cursor when touch outside search bar
        binding.root.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val outRect = android.graphics.Rect()
                binding.searchEditText.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    binding.searchEditText.clearFocus()
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
                }
            }
            false
        }
        // Show cursor only when search bar is focused
        binding.searchEditText.setOnFocusChangeListener { v, hasFocus ->
            binding.searchEditText.isCursorVisible = hasFocus
        }
        binding.searchEditText.isCursorVisible = false

        binding.backArrow.setOnClickListener{
            findNavController().navigateUp()
        }

    }

    private fun filterFaqs() {
        val query = binding.searchEditText.text?.toString()
        val topicIndexes = selectedTopicIndex?.let { topicToFaqIndexes[it] } ?: questions.indices.toList()
        helpCenterAdapter.filterByTopicAndQuery(topicIndexes, query)
    }

    override fun onResume() {
        super.onResume()
        com.nocturnevpn.utils.RatingDialogManager.maybeShowRatingDialog(requireActivity())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}