package com.nocturnevpn.view.activitys

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.nocturnevpn.R
import com.nocturnevpn.databinding.ActivityHomeBinding
import com.nocturnevpn.utils.AuthManager
import com.nocturnevpn.utils.AuthFlowManager
import me.ibrahimsn.lib.SmoothBottomBar
import com.nocturnevpn.utils.RatingDialogManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.view.updateLayoutParams
import androidx.constraintlayout.widget.ConstraintLayout


class HomeActivity : AppCompatActivity() {

    // View binding for accessing XML views
    private lateinit var binding: ActivityHomeBinding

    // SmoothBottomBar instance for bottom navigation
    private lateinit var bottomNavigation: SmoothBottomBar

    // Navigation Controller to handle fragment transitions
    private lateinit var navController: NavController

    // SharedPreferences to store Dark Mode settings
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {

        // Load Dark Mode preference before setting content view
//        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
//        val isDarkMode = sharedPreferences.getBoolean("DarkMode", false)
//        AppCompatDelegate.setDefaultNightMode(
//            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
//        )

        super.onCreate(savedInstanceState)

        // Draw edge-to-edge; we'll handle insets in layouts
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Inflate the layout using View Binding
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply insets to avoid overlapping content while keeping transparent bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top)
            // Lift bottom bar above navigation bar by adding a bottom margin
            binding.bottomBar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomMargin = systemBars.bottom
            }
            insets
        }

        askNotificationPermission()

        // Show rating dialog if needed (after first VPN connect/disconnect and once per day)
        RatingDialogManager.maybeShowRatingDialog(this)

        // Initialize bottom navigation bar
        bottomNavigation = binding.bottomBar

        // Get the NavController from the fragment container
        navController = supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
            ?.findNavController() ?: throw IllegalStateException("NavController not found")

        // Ensure Home is selected as the default tab
        bottomNavigation.itemActiveIndex = 1

        // Set up navigation logic for bottom navigation bar
        setupNavigation()
        

    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    /**
     * Sign out the current user and navigate to auth screen
     */
    fun signOut() {
        val authManager = AuthManager.getInstance(this)
        authManager.signOut()
        
        // Navigate back to auth screen
        val intent = Intent(this, AppAuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    /**
     * Check if user is signed in and show appropriate UI
     */
    fun checkAuthState() {
        val authManager = AuthManager.getInstance(this)
        val isSignedIn = authManager.isUserSignedIn()
        

        Log.d("HomeActivity", "User signed in: $isSignedIn")
        
        // You can add UI logic here to show/hide elements based on auth state
        // For example, show login button in profile fragment if not signed in
    }
    



    private fun setupNavigation() {
        // Define the fragments where the bottom navigation should be visible
        val visibleFragments = setOf(R.id.homeFragment, R.id.profileFragment, R.id.settingFragment3)

        // Handle bottom navigation item clicks
        bottomNavigation.onItemSelected = { position ->
            val currentDestination = navController.currentDestination?.id
            when (position) {
                1 -> if (currentDestination != R.id.homeFragment) {
                    navController.navigate(R.id.homeFragment) // Navigate to Home
                }
                0 -> if (currentDestination != R.id.profileFragment) {
                    navController.navigate(R.id.profileFragment) // Navigate to Profile
                }
                2 -> if (currentDestination != R.id.settingFragment3) {
                    navController.navigate(R.id.settingFragment3) // Navigate to Settings
                }
            }
        }

        // Listen for fragment changes and update the UI accordingly
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Show bottom navigation bar only on defined fragments
            if (destination.id in visibleFragments) {
                bottomNavigation.visibility = View.VISIBLE
            } else {
                bottomNavigation.visibility = View.GONE
            }

            // Ensure the correct bottom navigation item is highlighted
            val newIndex = when (destination.id) {
                R.id.homeFragment -> 1
                R.id.profileFragment -> 0
                R.id.settingFragment3 -> 2
                else -> -1 // Hide selection for non-navigation fragments
            }

            // Update the selected tab only if it's different
            if (newIndex != -1 && bottomNavigation.itemActiveIndex != newIndex) {
                bottomNavigation.itemActiveIndex = newIndex
            }
        }
    }


    // Notification permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

}
