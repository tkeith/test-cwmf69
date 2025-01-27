package com.delayedmessaging.android.ui.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity // version: 1.6.1
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI // version: 2.7.1
import androidx.navigation.ui.setupWithNavController
import com.delayedmessaging.android.R
import com.delayedmessaging.android.databinding.ActivityMainBinding
import com.delayedmessaging.android.util.ThemeManager
import com.delayedmessaging.android.util.PreferenceManager
import com.delayedmessaging.android.util.Logger
import com.google.android.material.bottomnavigation.BottomNavigationView // version: 1.9.0
import com.google.android.material.navigation.NavigationBarView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main activity serving as the primary container for app navigation and fragment hosting.
 * Implements Material Design 3 components and handles deep linking with enhanced state management.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    
    @Inject
    lateinit var themeManager: ThemeManager
    
    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.debug(TAG, "Creating MainActivity")

        // Apply edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar with Material Design 3
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        setupNavigation(savedInstanceState)
        setupTheme()
        handleDeepLink(intent)
    }

    private fun setupNavigation(savedInstanceState: Bundle?) {
        // Find nav host fragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Configure app bar with navigation
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.messageListFragment,
                R.id.composeMessageFragment,
                R.id.settingsFragment
            )
        )

        // Setup bottom navigation with Material Design 3
        binding.bottomNavigation.apply {
            setupWithNavController(navController)
            labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
            setOnItemReselectedListener { /* Prevent fragment recreation */ }
        }

        // Setup toolbar with navigation
        NavigationUI.setupWithNavController(
            binding.toolbar,
            navController,
            appBarConfiguration
        )

        // Restore navigation state if available
        savedInstanceState?.getInt("nav_state")?.let { destinationId ->
            navController.navigate(destinationId)
        }

        // Handle navigation changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.messageListFragment -> {
                    binding.toolbar.title = getString(R.string.title_messages)
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                R.id.composeMessageFragment -> {
                    binding.toolbar.title = getString(R.string.title_compose)
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                R.id.settingsFragment -> {
                    binding.toolbar.title = getString(R.string.title_settings)
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
            }
        }
    }

    private fun setupTheme() {
        lifecycleScope.launch {
            // Initialize theme based on saved preference
            val isDarkMode = preferenceManager.getThemeMode()
            val nightMode = if (isDarkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)

            // Listen for theme changes
            themeManager.setThemeChangeListener { isDark ->
                AppCompatDelegate.setDefaultNightMode(
                    if (isDark) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }
    }

    private fun handleDeepLink(intent: android.content.Intent) {
        navController.handleDeepLink(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save navigation state
        navController.currentDestination?.id?.let { destinationId ->
            outState.putInt("nav_state", destinationId)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
    }

    override fun onDestroy() {
        super.onDestroy()
        themeManager.setThemeChangeListener(null)
    }

    companion object {
        private const val ANIMATION_DURATION = 300L
    }
}