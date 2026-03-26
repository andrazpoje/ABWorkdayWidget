package com.dante.abworkdaywidget

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.dante.abworkdaywidget.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navHostFragment: NavHostFragment

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

            invalidateOptionsMenu()
        }
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

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIFICATION_PERMISSION
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != REQUEST_NOTIFICATION_PERMISSION) return

        val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED

        if (granted) return

        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment

        if (currentFragment is HomeFragment) {
            currentFragment.onNotificationPermissionDenied()
            return
        }

        if (currentFragment is SettingsFragment) {
            recreate()
            return
        }
    }
}