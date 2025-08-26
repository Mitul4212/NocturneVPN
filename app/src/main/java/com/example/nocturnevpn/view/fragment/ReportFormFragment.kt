package com.example.nocturnevpn.view.fragment

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.nocturnevpn.R
import com.example.nocturnevpn.databinding.FragmentReportFormBinding
import com.example.nocturnevpn.utils.EmailIntentHelper
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class ReportFormFragment : Fragment() {
    private var _binding: FragmentReportFormBinding? = null
    private val binding get() = _binding!!
    private var issueCategory: String? = null
    private lateinit var auth: FirebaseAuth
    private val attachmentUris = mutableListOf<Uri>()

    companion object {
        private const val ARG_ISSUE_CATEGORY = "issue_category"
        private const val SUPPORT_EMAIL = "paypal4212a@gmail.com" // TODO: Replace with your actual support email
        
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
        auth = FirebaseAuth.getInstance()
    }

    // Activity result launcher for gallery
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // Try to copy the image to our cache directory for better compatibility
            try {
                val copiedUri = copyImageToCache(it)
                attachmentUris.add(copiedUri)
                updateAttachmentUI()
                Log.d("ReportForm", "Added attachment: $copiedUri, authority: ${copiedUri.authority}, scheme: ${copiedUri.scheme}")
                Toast.makeText(requireContext(), "Image added!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.w("ReportForm", "Failed to copy image, using original URI", e)
                attachmentUris.add(it)
                updateAttachmentUI()
                Log.d("ReportForm", "Added original attachment: $it, authority: ${it.authority}, scheme: ${it.scheme}")
                Toast.makeText(requireContext(), "Image added!", Toast.LENGTH_SHORT).show()
            }
        }
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
        val deviceInfo = EmailIntentHelper.getDeviceInfo(requireContext())
        binding.etDeviceInfo.setText(deviceInfo)
        
        // Auto-fill user email from Firebase Auth
        val currentUser = auth.currentUser
        if (currentUser?.email != null) {
            binding.etEmail.setText(currentUser.email)
        }
        
        // Attachment button logic
        binding.btnAddAttachment.setOnClickListener {
            showAttachmentOptions()
        }
        
        // Send report button - now opens email intent
        binding.btnSendReport.setOnClickListener {
            sendReportViaEmail()
        }
    }

    private fun sendReportViaEmail() {
        // Validate required fields
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val subject = binding.etSubject.text?.toString()?.trim() ?: ""
        val description = binding.etDescription.text?.toString()?.trim() ?: ""
        
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return
        }
        
        if (subject.isEmpty()) {
            binding.etSubject.error = "Subject is required"
            return
        }
        
        if (description.isEmpty()) {
            binding.etDescription.error = "Description is required"
            return
        }
        
        // Collect all form data
        val steps = binding.etSteps.text?.toString()?.trim() ?: ""
        val expected = binding.etExpected.text?.toString()?.trim() ?: ""
        val quickFixesTried = binding.etQuickFixesTried.text?.toString()?.trim() ?: ""
        val deviceInfoText = binding.etDeviceInfo.text?.toString()?.trim() ?: ""
        
        // Create email body using utility class
        val emailBody = EmailIntentHelper.createIssueReportBody(
            context = requireContext(),
            issueCategory = issueCategory ?: "Unknown",
            userEmail = email,
            subject = subject,
            description = description,
            steps = steps,
            expected = expected,
            quickFixesTried = quickFixesTried,
            deviceInfo = deviceInfoText
        )
        
        // Open email app using utility class
        Log.d("ReportForm", "Sending email with ${attachmentUris.size} attachments")
        val success = EmailIntentHelper.openEmailApp(
            context = requireContext(),
            toEmail = SUPPORT_EMAIL,
            subject = "NocturneVPN Issue Report: $subject",
            body = emailBody,
            attachmentUris = attachmentUris
        )
        
        if (success) {
            Log.d("ReportForm", "Email intent launched successfully")
            Toast.makeText(requireContext(), "Opening email app...", Toast.LENGTH_SHORT).show()
            // Close the activity after successful email intent launch
            requireActivity().finish()
        } else {
            Log.w("ReportForm", "Failed to launch email intent")
            // Don't close activity if email intent failed - user might want to try again
        }
    }

    /**
     * Shows attachment options dialog
     */
    private fun showAttachmentOptions() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.attachment_dialog)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Update title based on current attachments
        val currentCount = attachmentUris.size
        val titleTextView = dialog.findViewById<android.widget.TextView>(R.id.attachment_dialog_title)
        titleTextView.text = if (currentCount > 0) "Attachments ($currentCount)" else "Add Attachment"

        // Update subtitle based on current attachments
        val subtitleTextView = dialog.findViewById<android.widget.TextView>(R.id.attachment_dialog_subtitle)
        subtitleTextView.text = if (currentCount > 0) {
            "You have $currentCount attachment(s). Add more or remove all."
        } else {
            "Choose an option to add attachments to your report"
        }

        // Choose from Gallery button
        val btnChooseGallery = dialog.findViewById<com.google.android.material.button.MaterialButton>(
            R.id.btn_choose_gallery)
        btnChooseGallery.setOnClickListener {
            chooseFromGallery()
            dialog.dismiss()
        }

        // Remove All Attachments button
        val btnRemoveAttachments = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_remove_attachments)
        btnRemoveAttachments.setOnClickListener {
            removeAllAttachments()
            dialog.dismiss()
        }

        // Cancel button
        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Chooses image from gallery
     */
    private fun chooseFromGallery() {
        if (checkStoragePermission()) {
            galleryLauncher.launch("image/*")
        } else {
            requestStoragePermission()
        }
    }

    /**
     * Removes all attachments
     */
    private fun removeAllAttachments() {
        attachmentUris.clear()
        updateAttachmentUI()
        Toast.makeText(requireContext(), "All attachments removed", Toast.LENGTH_SHORT).show()
    }

    /**
     * Updates the attachment UI
     */
    private fun updateAttachmentUI() {
        val attachmentCount = attachmentUris.size
        binding.btnAddAttachment.text = if (attachmentCount > 0) {
            "Attachments ($attachmentCount) - Tap to add more"
        } else {
            "Add Attachment"
        }
    }

    /**
     * Checks storage permission
     */
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Requests storage permission
     */
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Permission launcher
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, retry the action
            chooseFromGallery()
        } else {
            Toast.makeText(requireContext(), "Permission required to add attachments", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies image to cache directory for better email compatibility
     */
    private fun copyImageToCache(originalUri: Uri): Uri {
        val inputStream = requireContext().contentResolver.openInputStream(originalUri)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "ATTACHMENT_$timeStamp.jpg"
        val cacheDir = File(requireContext().cacheDir, "attachments")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val imageFile = File(cacheDir, imageFileName)
        
        inputStream?.use { input ->
            FileOutputStream(imageFile).use { output ->
                input.copyTo(output)
            }
        }
        
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 