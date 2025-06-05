package com.example.nocturnevpn.view.activitys

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.nocturnevpn.R
import com.example.nocturnevpn.databinding.ActivityHomeBinding
import me.ibrahimsn.lib.SmoothBottomBar

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

        // Inflate the layout using View Binding
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
}
