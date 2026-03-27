package com.dante.abworkdaywidget

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.dante.abworkdaywidget.databinding.ActivityMainBinding
import com.dante.abworkdaywidget.notifications.MidnightAlarmScheduler
import com.dante.abworkdaywidget.notifications.NotificationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navHostFragment: NavHostFragment

    private var pendingNotificationPermissionResult: ((Boolean) -> Unit)? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pendingNotificationPermissionResult?.invoke(granted)
            pendingNotificationPermissionResult = null

            if (!granted) {
                val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
                if (currentFragment is HomeFragment) {
                    currentFragment.onNotificationPermissionDenied()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLanguageManager.applySavedLanguage(this)
        AppThemeManager.applyFromPreferences(this)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDefaultEdgeToEdge()

        binding.toolbar.applyTopStatusBarInsetAsMargin()
        binding.bottomNavigation.applyBottomNavInsetAsPadding()
        updateSystemBarIconContrast(binding.root)

        setSupportActionBar(binding.toolbar)

        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.calendarFragment,
                R.id.moreFragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)

        ensureNotificationFallbackScheduled()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val appName = getString(R.string.app_name)

            val suffix = when (destination.id) {
                R.id.homeFragment -> ""
                R.id.calendarFragment -> getString(R.string.nav_calendar)
                R.id.moreFragment -> getString(R.string.nav_more)
                R.id.statisticsFragment -> getString(R.string.statistics_title)
                R.id.whatsNewFragment -> getString(R.string.whats_new_title)
                R.id.settingsFragment -> getString(R.string.settings_title)
                R.id.helpFragment -> getString(R.string.help_title)
                else -> ""
            }

            binding.toolbar.title = if (suffix.isEmpty()) {
                appName
            } else {
                "$appName - $suffix"
            }

            val showBottomNav = destination.id == R.id.homeFragment ||
                destination.id == R.id.calendarFragment ||
                destination.id == R.id.moreFragment

            binding.bottomNavigation.visibility = if (showBottomNav) View.VISIBLE else View.GONE

            invalidateOptionsMenu()
        }
    }

    private fun ensureNotificationFallbackScheduled() {
        if (NotificationHelper.areNotificationsEnabled(this)) {
            MidnightAlarmScheduler.scheduleNext(this)
        } else {
            MidnightAlarmScheduler.cancel(this)
        }
    }

    fun isNotificationPermissionGranted(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestNotificationPermissionIfNeeded(onResult: (Boolean) -> Unit) {
        if (isNotificationPermissionGranted()) {
            onResult(true)
            return
        }

        pendingNotificationPermissionResult = onResult
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val destinationId = navHostFragment.navController.currentDestination?.id

        val showActions = destinationId == R.id.homeFragment ||
            destinationId == R.id.calendarFragment ||
            destinationId == R.id.moreFragment

        menu.findItem(R.id.action_settings)?.isVisible = showActions
        menu.findItem(R.id.action_help)?.isVisible = showActions

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                navHostFragment.navController.navigate(R.id.settingsFragment)
                true
            }

            R.id.action_help -> {
                navHostFragment.navController.navigate(R.id.helpFragment)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}
