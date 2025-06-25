package com.example.nocturnevpn.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nocturnevpn.databinding.FragmentReportFormBinding
import android.os.Build
import android.content.pm.PackageManager

class ReportFormFragment : Fragment() {
    private var _binding: FragmentReportFormBinding? = null
    private val binding get() = _binding!!
    private var issueCategory: String? = null

    companion object {
        private const val ARG_ISSUE_CATEGORY = "issue_category"
        fun newInstance(issueCategory: String?): ReportFormFragment {
            val fragment = ReportFormFragment()
            val args = Bundle()
            args.putString(ARG_ISSUE_CATEGORY, issueCategory)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        issueCategory = arguments?.getString(ARG_ISSUE_CATEGORY)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentReportFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set issue category
        binding.etIssueCategory.setText(issueCategory ?: "")
        // Set device info
        val deviceInfo = getDeviceInfo()
        binding.etDeviceInfo.setText(deviceInfo)
        // Attachment button logic (placeholder)
        binding.btnAddAttachment.setOnClickListener {
            Toast.makeText(requireContext(), "Attachment feature coming soon!", Toast.LENGTH_SHORT).show()
            Log.d("ReportForm", "Attachment button clicked")
        }
        binding.btnSendReport.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val subject = binding.etSubject.text?.toString()?.trim() ?: ""
            val description = binding.etDescription.text?.toString()?.trim() ?: ""
            val steps = binding.etSteps.text?.toString()?.trim() ?: ""
            val expected = binding.etExpected.text?.toString()?.trim() ?: ""
            val quickFixesTried = binding.etQuickFixesTried.text?.toString()?.trim() ?: ""
            val deviceInfoText = binding.etDeviceInfo.text?.toString()?.trim() ?: ""

            val reportData = """
                {
                  "issueCategory": "$issueCategory",
                  "email": "$email",
                  "subject": "$subject",
                  "description": "$description",
                  "steps": "$steps",
                  "expected": "$expected",
                  "quickFixesTried": "$quickFixesTried",
                  "deviceInfo": "$deviceInfoText"
                }
            """.trimIndent()

            Log.d("ReportForm", reportData)
            Toast.makeText(requireContext(), "Report sent! (see log)", Toast.LENGTH_SHORT).show()
            requireActivity().finish()
        }
    }

    private fun getDeviceInfo(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val version = Build.VERSION.RELEASE
        val sdkInt = Build.VERSION.SDK_INT
        val appVersion = try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
        return "Device: $manufacturer $model\nAndroid: $version (SDK $sdkInt)\nApp Version: $appVersion"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 