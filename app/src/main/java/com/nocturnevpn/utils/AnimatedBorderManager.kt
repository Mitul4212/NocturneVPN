package com.nocturnevpn.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nocturnevpn.widget.AnimatedGradientBorderView

/**
 * AnimatedBorderManager - Global manager for animated border state across the app
 * 
 * This manager handles the animated stroke border for the go_pro_button across fragment navigation.
 * It maintains the animation state in SharedPreferences and restores it when the user returns to the HomeFragment.
 * 
 * Usage:
 * 1. Get instance: AnimatedBorderManager.getInstance(context)
 * 2. Start animation: manager.startAnimatedBorder(borderView, durationMillis)
 * 3. Stop animation: manager.stopAnimatedBorder()
 * 4. Restore state: manager.restoreAnimationState(borderView)
 * 5. Set navigation flag: manager.setShouldShowAfterNavigation(true)
 */

class AnimatedBorderManager private constructor(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "animated_border_prefs"
        private const val KEY_IS_ANIMATING = "is_animating"
        private const val KEY_ANIMATION_END_TIME = "animation_end_time"
        private const val KEY_SHOULD_SHOW_AFTER_NAVIGATION = "should_show_after_navigation"
        const val ANIMATION_INFINITE: Long = Long.MAX_VALUE // Define ANIMATION_INFINITE
        
        @Volatile
        private var INSTANCE: AnimatedBorderManager? = null
        
        fun getInstance(context: Context): AnimatedBorderManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnimatedBorderManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var currentAnimationRunnable: Runnable? = null
    private var currentBorderView: AnimatedGradientBorderView? = null
    private var isStartingAnimation = false // Flag to prevent multiple starts
    
    fun startAnimatedBorder(borderView: AnimatedGradientBorderView, durationMillis: Long = 60000) {
        if (isStartingAnimation) {
            Log.d("AnimatedBorderManager", "Animation already starting, skipping")
            return
        }
        
        Log.d("AnimatedBorderManager", "Starting animated border for ${durationMillis}ms")
        isStartingAnimation = true
        
        // Stop any existing animation without clearing state first
        stopAnimatedBorderWithoutClearingState()
        
        // Update current border view
        currentBorderView = borderView
        
        // Save state to preferences and start the animation
        val infinite = durationMillis == ANIMATION_INFINITE
        val endTime = if (infinite) Long.MAX_VALUE else (System.currentTimeMillis() + durationMillis)
        prefs.edit()
            .putBoolean(KEY_IS_ANIMATING, true)
            .putLong(KEY_ANIMATION_END_TIME, endTime)
            .apply()

        borderView.startBorderAnimation()

        // Schedule stop only for finite durations
        if (!infinite) {
            currentAnimationRunnable = Runnable {
                stopAnimatedBorder()
            }
            handler.postDelayed(currentAnimationRunnable!!, durationMillis)
        } else {
            currentAnimationRunnable = null
        }
        
        // Add a small delay to ensure animation starts properly
        handler.postDelayed({
            Log.d("AnimatedBorderManager", "Animation should now be running")
        }, 100)
        
        Log.d("AnimatedBorderManager", "Animated border started, will stop at ${endTime}")
        isStartingAnimation = false
    }
    
    fun stopAnimatedBorder() {
        Log.d("AnimatedBorderManager", "Stopping animated border")
        
        // Cancel any pending runnable
        currentAnimationRunnable?.let { handler.removeCallbacks(it) }
        currentAnimationRunnable = null
        
        // Stop animation on current view
        currentBorderView?.stopBorderAnimation()
        currentBorderView = null
        
        // Clear state from preferences
        prefs.edit()
            .putBoolean(KEY_IS_ANIMATING, false)
            .putLong(KEY_ANIMATION_END_TIME, 0)
            .putBoolean(KEY_SHOULD_SHOW_AFTER_NAVIGATION, false) // Clear navigation flag too
            .apply()
    }
    
    fun stopAnimatedBorderWithoutClearingState() {
        Log.d("AnimatedBorderManager", "Stopping animated border without clearing state")
        
        // Cancel any pending runnable
        currentAnimationRunnable?.let { handler.removeCallbacks(it) }
        currentAnimationRunnable = null
        
        // Stop animation on current view only
        currentBorderView?.stopBorderAnimation()
        currentBorderView = null
        
        // IMPORTANT: Do NOT clear preferences - keep the state for restoration
        // The preferences should remain intact so we can restore the animation later
        Log.d("AnimatedBorderManager", "State preserved in preferences for later restoration")
    }
    
    fun setShouldShowAfterNavigation(shouldShow: Boolean) {
        prefs.edit().putBoolean(KEY_SHOULD_SHOW_AFTER_NAVIGATION, shouldShow).apply()
        Log.d("AnimatedBorderManager", "Set should show after navigation: $shouldShow")
    }
    
    fun shouldShowAfterNavigation(): Boolean {
        return prefs.getBoolean(KEY_SHOULD_SHOW_AFTER_NAVIGATION, false)
    }
    
    fun isCurrentlyAnimating(): Boolean {
        return prefs.getBoolean(KEY_IS_ANIMATING, false)
    }
    
    fun getAnimationEndTime(): Long {
        return prefs.getLong(KEY_ANIMATION_END_TIME, 0)
    }
    
    fun restoreAnimationState(borderView: AnimatedGradientBorderView) {
        val isAnimating = isCurrentlyAnimating()
        val endTime = getAnimationEndTime()
        val shouldShowAfterNav = shouldShowAfterNavigation()
        
        Log.d("AnimatedBorderManager", "Restoring animation state: isAnimating=$isAnimating, endTime=$endTime, shouldShowAfterNav=$shouldShowAfterNav")
        
        // Check if we already have a current view to prevent multiple restorations
        if (currentBorderView != null) {
            Log.d("AnimatedBorderManager", "Already have current view, skipping restoration")
            return
        }
        
        // Check if the view is already animating
        if (isViewAnimating(borderView)) {
            Log.d("AnimatedBorderManager", "View is already animating, skipping restoration")
            return
        }
        
        if (isAnimating && (endTime == Long.MAX_VALUE || endTime > System.currentTimeMillis())) {
            // Animation should still be running
            val remainingTime = if (endTime == Long.MAX_VALUE) Long.MAX_VALUE else (endTime - System.currentTimeMillis())
            Log.d("AnimatedBorderManager", "Restoring animation with ${if (endTime == Long.MAX_VALUE) "infinite" else "$remainingTime"}ms remaining")
            
            currentBorderView = borderView
            borderView.startBorderAnimation()
            
            if (endTime != Long.MAX_VALUE) {
                currentAnimationRunnable = Runnable {
                    stopAnimatedBorder()
                }
                handler.postDelayed(currentAnimationRunnable!!, remainingTime)
            } else {
                currentAnimationRunnable = null
            }
            
        } else if (shouldShowAfterNav) {
            // Should show animation after navigation
            Log.d("AnimatedBorderManager", "Showing animation after navigation")
            setShouldShowAfterNavigation(false)
            startAnimatedBorder(borderView, 60000) // 1 minute duration
        } else if (isAnimating && endTime <= System.currentTimeMillis()) {
            // Animation has expired, clear the state
            Log.d("AnimatedBorderManager", "Animation has expired, clearing state")
            stopAnimatedBorder()
        } else {
            // No animation should be running
            Log.d("AnimatedBorderManager", "No animation to restore")
            borderView.stopBorderAnimation()
        }
    }
    
    fun checkAndRestoreAnimation(borderView: AnimatedGradientBorderView) {
        // This method is called when the user manually tries to trigger animation
        val isAnimating = isCurrentlyAnimating()
        val endTime = getAnimationEndTime()
        
        Log.d("AnimatedBorderManager", "Checking animation state: isAnimating=$isAnimating, endTime=$endTime")
        
        if (isAnimating && endTime > System.currentTimeMillis()) {
            // Animation is already running, restart it
            Log.d("AnimatedBorderManager", "Animation already running, restarting")
            val remainingTime = endTime - System.currentTimeMillis()
            startAnimatedBorder(borderView, remainingTime)
        } else {
            // Start fresh animation
            Log.d("AnimatedBorderManager", "Starting fresh animation")
            startAnimatedBorder(borderView, 60000)
        }
    }
    
    fun forceStartAnimation(borderView: AnimatedGradientBorderView, durationMillis: Long = 60000) {
        Log.d("AnimatedBorderManager", "Force starting animation for ${durationMillis}ms")
        
        // Reset the starting flag to allow force start
        isStartingAnimation = false
        
        // Stop any existing animation completely
        stopAnimatedBorder()
        
        // Start fresh animation
        startAnimatedBorder(borderView, durationMillis)
    }
    
    fun triggerAnimation(borderView: AnimatedGradientBorderView, durationMillis: Long = 60000) {
        Log.d("AnimatedBorderManager", "Manual trigger animation for ${durationMillis}ms")
        
        // Set navigation flag to preserve animation across navigation
        setShouldShowAfterNavigation(true)
        
        // Force start animation regardless of current state
        forceStartAnimation(borderView, durationMillis)
    }
    
    fun isAnimationActive(): Boolean {
        return currentBorderView != null && isCurrentlyAnimating() && currentBorderView!!.isAnimating()
    }
    
    fun isViewAnimating(borderView: AnimatedGradientBorderView): Boolean {
        return borderView.isAnimating() && borderView.hasGradient()
    }
    
    fun isAnimationRunningForNavigation(): Boolean {
        return shouldShowAfterNavigation() || (isCurrentlyAnimating() && getAnimationEndTime() > System.currentTimeMillis())
    }
    
    fun onFragmentPause() {
        // When fragment is paused, stop the animation but keep the state
        Log.d("AnimatedBorderManager", "Fragment paused - preserving animation state")
        
        // Check if we should preserve the animation state
        if (isAnimationRunningForNavigation()) {
            Log.d("AnimatedBorderManager", "Animation is running for navigation - preserving state")
            stopAnimatedBorderWithoutClearingState()
        } else {
            Log.d("AnimatedBorderManager", "Animation is not for navigation - stopping completely")
            stopAnimatedBorder()
        }
    }
    
    fun onFragmentResume(borderView: AnimatedGradientBorderView) {
        // When fragment resumes, restore the animation state
        Log.d("AnimatedBorderManager", "Fragment resumed - restoring animation state")
        restoreAnimationState(borderView)
    }
    
    fun debugAnimationState() {
        val isAnimating = isCurrentlyAnimating()
        val endTime = getAnimationEndTime()
        val shouldShowAfterNav = shouldShowAfterNavigation()
        val hasCurrentView = currentBorderView != null
        val viewAnimating = currentBorderView?.isAnimating() ?: false
        val viewHasGradient = currentBorderView?.hasGradient() ?: false
        
        Log.d("AnimatedBorderManager", "=== DEBUG STATE ===")
        Log.d("AnimatedBorderManager", "isAnimating: $isAnimating")
        Log.d("AnimatedBorderManager", "endTime: $endTime")
        Log.d("AnimatedBorderManager", "shouldShowAfterNav: $shouldShowAfterNav")
        Log.d("AnimatedBorderManager", "hasCurrentView: $hasCurrentView")
        Log.d("AnimatedBorderManager", "viewAnimating: $viewAnimating")
        Log.d("AnimatedBorderManager", "viewHasGradient: $viewHasGradient")
        Log.d("AnimatedBorderManager", "isStartingAnimation: $isStartingAnimation")
        Log.d("AnimatedBorderManager", "currentTime: ${System.currentTimeMillis()}")
        if (endTime > 0) {
            Log.d("AnimatedBorderManager", "timeRemaining: ${endTime - System.currentTimeMillis()}")
        }
        Log.d("AnimatedBorderManager", "==================")
    }
    
    fun cleanup() {
        stopAnimatedBorder()
        INSTANCE = null
    }
} 