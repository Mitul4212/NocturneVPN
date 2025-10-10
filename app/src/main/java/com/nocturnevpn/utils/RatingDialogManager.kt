package com.nocturnevpn.utils

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RatingBar
import androidx.fragment.app.FragmentActivity
import com.nocturnevpn.R
import java.util.concurrent.TimeUnit

object RatingDialogManager {
    fun maybeShowRatingDialog(activity: FragmentActivity) {
        val prefs = activity.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasRated = prefs.getBoolean("has_rated", false)
        val vpnConnectedOnce = prefs.getBoolean("vpn_connected_once", false)
        val vpnPrompted = prefs.getBoolean("vpn_prompted", false)
        val lastPromptTime = prefs.getLong("last_rating_prompt", 0L)
        val now = System.currentTimeMillis()
        val oneDayMillis = TimeUnit.DAYS.toMillis(1)

        if (!hasRated && vpnConnectedOnce) {
            if (!vpnPrompted || now - lastPromptTime > oneDayMillis) {
                showRatingDialog(activity, prefs)
                prefs.edit().putBoolean("vpn_prompted", true).putLong("last_rating_prompt", now).apply()
            }
        }
    }

    private fun showRatingDialog(activity: FragmentActivity, prefs: android.content.SharedPreferences) {
        val dialog = Dialog(activity)
        dialog.setContentView(R.layout.rating_dialog)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ratingBar = dialog.findViewById<RatingBar>(R.id.ratingBar)
        val btnSubmit = dialog.findViewById<Button>(R.id.btnSubmit)
        val tvNoThanks = dialog.findViewById<Button>(R.id.tvNoThanks)
        val ivClose = dialog.findViewById<LinearLayout>(R.id.ivClose)

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating
            if (rating >= 4) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = android.net.Uri.parse("market://details?id=" + activity.packageName)
                activity.startActivity(intent)
            }
            prefs.edit().putBoolean("has_rated", true).apply()
            dialog.dismiss()
        }

        tvNoThanks.setOnClickListener {
            dialog.dismiss()
        }

        ivClose?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
} 