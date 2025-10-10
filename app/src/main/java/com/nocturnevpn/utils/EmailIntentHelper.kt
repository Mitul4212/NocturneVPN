package com.nocturnevpn.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.nocturnevpn.BuildConfig

object EmailIntentHelper {
    
    private const val TAG = "EmailIntentHelper"
    
    /**
     * Opens email app with pre-filled data for issue reporting
     */
    fun openEmailApp(
        context: Context,
        toEmail: String,
        subject: String,
        body: String,
        attachmentUris: List<android.net.Uri> = emptyList()
    ): Boolean {
        try {
            // Try multiple email intent approaches
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$toEmail")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            
            // For no attachments, use ACTION_SEND with message/rfc822 (most reliable)
            if (attachmentUris.isEmpty()) {
                val emailIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(toEmail))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                
                if (emailIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(Intent.createChooser(emailIntent, "Send email via..."))
                    Log.d(TAG, "Email intent launched successfully with ACTION_SEND (no attachments)")
                    return true
                }
            }
            
            // Second try: ACTION_SEND with single attachment
            if (attachmentUris.size == 1) {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(toEmail))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    putExtra(Intent.EXTRA_STREAM, attachmentUris[0])
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    Log.d(TAG, "Added single attachment: ${attachmentUris[0]}")
                }
                
                if (sendIntent.resolveActivity(context.packageManager) != null) {
                    // Create chooser with specific email apps
                    val chooser = Intent.createChooser(sendIntent, "Send email via...")
                    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(chooser)
                    Log.d(TAG, "Email intent launched successfully with chooser and 1 attachment")
                    return true
                }
            }
            


            
            // Third try: ACTION_SEND_MULTIPLE for multiple attachments
            if (attachmentUris.size > 1) {
                val multipleIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(toEmail))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachmentUris))
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                if (multipleIntent.resolveActivity(context.packageManager) != null) {
                    val chooser = Intent.createChooser(multipleIntent, "Send email via...")
                    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(chooser)
                    Log.d(TAG, "Multiple email intent launched successfully with ${attachmentUris.size} attachments")
                    return true
                }
            }
            
            // Fourth try: Just open any app that can handle text (final fallback)
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, """
                    To: $toEmail
                    Subject: $subject
                    
                    $body
                """.trimIndent())
            }
            
            if (textIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(Intent.createChooser(textIntent, "Share via..."))
                Log.d(TAG, "Text sharing intent launched as fallback")
                return true
            }
            
            // No email app found
            Log.w(TAG, "No email app found on device")
            showNoEmailAppDialog(context, toEmail, subject, body, attachmentUris)
            return false
            
        } catch (e: Exception) {
            Log.e(TAG, "Error launching email intent", e)
            Toast.makeText(context, "Error opening email app: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }
    
    /**
     * Shows dialog when no email app is found
     */
    private fun showNoEmailAppDialog(context: Context, toEmail: String, subject: String, body: String, attachmentUris: List<android.net.Uri> = emptyList()) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("No Email App Found")
            .setMessage("No email app is installed on your device. Would you like to copy the report to clipboard?")
            .setPositiveButton("Copy to Clipboard") { _, _ ->
                copyToClipboard(context, toEmail, subject, body, attachmentUris)
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
    }
    
    /**
     * Copies email content to clipboard
     */
    private fun copyToClipboard(context: Context, toEmail: String, subject: String, body: String, attachmentUris: List<android.net.Uri> = emptyList()) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val attachmentInfo = if (attachmentUris.isNotEmpty()) "\n\nAttachments: ${attachmentUris.size} file(s)" else ""
        val clip = android.content.ClipData.newPlainText("Email Report", """
            To: $toEmail
            Subject: $subject
            
            $body$attachmentInfo
        """.trimIndent())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Report copied to clipboard!", Toast.LENGTH_LONG).show()
    }
    
    /**
     * Creates formatted email body for issue reports
     */
    fun createIssueReportBody(
        context: Context,
        issueCategory: String,
        userEmail: String,
        subject: String,
        description: String,
        steps: String = "",
        expected: String = "",
        quickFixesTried: String = "",
        deviceInfo: String = ""
    ): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val firebaseEmail = currentUser?.email ?: "Not logged in"
        
        return buildString {
            appendLine("=== NOCTURNE VPN ISSUE REPORT ===")
            appendLine()
            appendLine("Issue Category: $issueCategory")
            appendLine("User Email: $userEmail")
            appendLine("Firebase User: $firebaseEmail")
            appendLine("Subject: $subject")
            appendLine()
            appendLine("=== DESCRIPTION ===")
            appendLine(description)
            appendLine()
            
            if (steps.isNotEmpty()) {
                appendLine("=== STEPS TO REPRODUCE ===")
                appendLine(steps)
                appendLine()
            }
            
            if (expected.isNotEmpty()) {
                appendLine("=== EXPECTED BEHAVIOR ===")
                appendLine(expected)
                appendLine()
            }
            
            if (quickFixesTried.isNotEmpty()) {
                appendLine("=== QUICK FIXES TRIED ===")
                appendLine(quickFixesTried)
                appendLine()
            }
            
            if (deviceInfo.isNotEmpty()) {
                appendLine("=== DEVICE INFORMATION ===")
                appendLine(deviceInfo)
                appendLine()
            }
            
            appendLine("=== REPORT GENERATED ===")
            appendLine("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
            appendLine("App Version: ${getAppVersion(context)}")
        }
    }
    
    /**
     * Gets device information for reports
     */
    fun getDeviceInfo(context: Context): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val version = Build.VERSION.RELEASE
        val sdkInt = Build.VERSION.SDK_INT
        val appVersion = getAppVersion(context)
        
        return buildString {
            appendLine("Device: $manufacturer $model")
            appendLine("Android: $version (SDK $sdkInt)")
            appendLine("App Version: $appVersion")
            appendLine("Build Type: ${if (BuildConfig.DEBUG) "Debug" else "Release"}")
        }
    }
    
    /**
     * Gets app version name
     */
    private fun getAppVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Auto-fills user email from Firebase Auth
     */
    fun getCurrentUserEmail(): String? {
        return FirebaseAuth.getInstance().currentUser?.email
    }
} 