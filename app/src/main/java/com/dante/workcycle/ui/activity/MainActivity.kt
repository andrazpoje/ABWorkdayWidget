package com.dante.workcycle.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.dante.workcycle.AppLanguageManager
import com.dante.workcycle.R
import com.dante.workcycle.core.theme.AppThemeManager
import com.dante.workcycle.core.ui.applyBottomNavInsetAsPadding
import com.dante.workcycle.core.ui.applyTopStatusBarInsetAsMargin
import com.dante.workcycle.core.ui.setupDefaultEdgeToEdge
import com.dante.workcycle.core.ui.updateSystemBarIconContrast
import com.dante.workcycle.databinding.ActivityMainBinding
import com.dante.workcycle.notifications.MidnightAlarmScheduler
import com.dante.workcycle.notifications.NotificationHelper
import com.dante.workcycle.ui.home.HomeFragment
import androidx.core.os.bundleOf
import com.dante.workcycle.data.prefs.LaunchPrefs
import android.widget.ImageView
import androidx.core.view.children
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navHostFragment: NavHostFragment

    private var pendingNotificationPermissionResult: ((Boolean) -> Unit)? = null
    private var isNotificationPermissionRequestInFlight = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            isNotificationPermissionRequestInFlight = false

            pendingNotificationPermissionResult?.invoke(granted)
            pendingNotificationPermissionResult = null

            if (!granted && ::navHostFragment.isInitialized) {
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

        setupDefaultEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.applyTopStatusBarInsetAsMargin()
        binding.bottomNavigation.applyBottomNavInsetAsPadding()
        updateSystemBarIconContrast(binding.toolbar)

        setSupportActionBar(binding.toolbar)

        binding.toolbar.post {
            val size = (38 * resources.displayMetrics.density).toInt()
            val marginEnd = (10 * resources.displayMetrics.density).toInt()

            binding.toolbar.children.forEach { child ->
                if (child is ImageView && child.drawable != null) {
                    val params = child.layoutParams as? Toolbar.LayoutParams
                    if (params != null) {
                        params.width = size
                        params.height = size
                        params.marginEnd = marginEnd
                        child.layoutParams = params
                    } else {
                        child.layoutParams.width = size
                        child.layoutParams.height = size
                    }

                    child.adjustViewBounds = true
                    child.scaleType = ImageView.ScaleType.FIT_CENTER
                    child.requestLayout()
                }
            }
        }

        val host = supportFragmentManager.findFragmentById(R.id.navHostFragment)
        if (host !is NavHostFragment) {
            finish()
            return
        }
        navHostFragment = host

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
        handleLaunchDestination(navController, savedInstanceState)

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
                suffix
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

        if (isNotificationPermissionRequestInFlight) {
            return
        }

        pendingNotificationPermissionResult = onResult
        isNotificationPermissionRequestInFlight = true
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
                safeNavigate(R.id.settingsFragment)
                true
            }

            R.id.action_help -> {
                safeNavigate(R.id.helpFragment)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun handleLaunchDestination(
        navController: NavController,
        savedInstanceState: Bundle?
    ) {
        if (savedInstanceState != null) return

        val launchPrefs = LaunchPrefs(this)

        binding.root.post {
            when {
                !launchPrefs.isOnboardingCompleted() -> {
                    runCatching {
                        navController.navigate(
                            R.id.helpFragment,
                            bundleOf("isOnboarding" to true)
                        )
                    }
                }

                launchPrefs.shouldShowWhatsNew() -> {
                    runCatching {
                        navController.navigate(R.id.whatsNewFragment)
                    }
                }
            }
        }
    }
    private fun safeNavigate(@IdRes destinationId: Int) {
        val navController = navHostFragment.navController
        val currentId = navController.currentDestination?.id
        if (currentId == destinationId) return

        runCatching {
            navController.navigate(destinationId)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
}