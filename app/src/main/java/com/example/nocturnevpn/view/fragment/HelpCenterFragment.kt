package com.example.nocturnevpn.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nocturnevpn.R
import com.example.nocturnevpn.adapter.HelpCenterAdapter
import com.example.nocturnevpn.databinding.FragmentHelpCenterBinding
import android.os.Build
import androidx.core.content.ContextCompat


class HelpCenterFragment : Fragment() {

    private var _binding : FragmentHelpCenterBinding? = null
    private val binding get() = _binding!!

    private lateinit var questions: Array<String>
    private lateinit var answers: Array<String>

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

        binding.helpCenterRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.helpCenterRecyclerView.adapter = HelpCenterAdapter(questions, answers)
        binding.helpCenterRecyclerView.setPadding(0, 0, 0, 24) // Extra bottom padding for last item

        binding.backArrow.setOnClickListener{
            findNavController().navigateUp()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}