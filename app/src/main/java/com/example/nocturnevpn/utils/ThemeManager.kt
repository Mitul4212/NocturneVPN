package com.example.nocturnevpn.utils

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.nocturnevpn.SharedPreference

/**
 * ThemeManager handles dark mode theme switching throughout the app
 */
object ThemeManager {
    
    /**
     * Apply the saved theme preference
     */
    fun applyTheme(context: Context) {
        val sharedPref = SharedPreference(context)
        val isDarkMode = sharedPref.isDarkModeEnabled()
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    
    /**
     * Toggle between light and dark mode
     */
    fun toggleTheme(context: Context) {
        val sharedPref = SharedPreference(context)
        val currentMode = sharedPref.isDarkModeEnabled()
        val newMode = !currentMode
        
        sharedPref.setDarkModeEnabled(newMode)
        
        if (newMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    
    /**
     * Set dark mode explicitly
     */
    fun setDarkMode(context: Context, enabled: Boolean) {
        val sharedPref = SharedPreference(context)
        sharedPref.setDarkModeEnabled(enabled)
        
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    
    /**
     * Check if dark mode is currently enabled
     */
    fun isDarkModeEnabled(context: Context): Boolean {
        val sharedPref = SharedPreference(context)
        return sharedPref.isDarkModeEnabled()
    }
    
    /**
     * Recreate activity to apply theme changes
     */
    fun recreateActivity(activity: Activity) {
        activity.recreate()
    }
}
