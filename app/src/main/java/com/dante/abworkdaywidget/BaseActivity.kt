package com.dante.abworkdaywidget

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity : AppCompatActivity() {

    /**
     * Root view of the activity layout.
     */
    protected abstract val activityRootView: View

    /**
     * Main content view that should be pushed below the status bar.
     */
    protected abstract val topInsetTargetView: View

    /**
     * Optional bottom navigation view.
     */
    protected open val bottomNavigationView: BottomNavigationView? = null

    /**
     * Optional save/action bar above the bottom navigation.
     */
    protected open val imeInsetTargetView: View? = null

    /**
     * Currently selected bottom nav item, if this screen uses bottom nav.
     */
    protected open val selectedBottomNavItemId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemeManager.applyFromPreferences(this)
        super.onCreate(savedInstanceState)
    }

    protected fun setupBaseUi() {
        setupDefaultEdgeToEdge()

        topInsetTargetView.applyTopStatusBarInsetAsMargin()
        bottomNavigationView?.applyBottomNavInsetAsPadding()
        imeInsetTargetView?.applyBottomSystemInsetWithImeAsPadding()

        updateSystemBarIconContrast(activityRootView)

        val bottomNav = bottomNavigationView
        val selectedItemId = selectedBottomNavItemId

        if (bottomNav != null && selectedItemId != null) {
            setupBottomNavigation(selectedItemId)
            syncBottomNavigationSelection(selectedItemId)
        }
    }

    override fun onResume() {
        super.onResume()

        val bottomNav = bottomNavigationView
        val selectedItemId = selectedBottomNavItemId

        if (bottomNav != null && selectedItemId != null) {
            syncBottomNavigationSelection(selectedItemId)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val bottomNav = bottomNavigationView
        val selectedItemId = selectedBottomNavItemId

        if (bottomNav != null && selectedItemId != null) {
            syncBottomNavigationSelection(selectedItemId)
        }
    }

    private fun syncBottomNavigationSelection(itemId: Int) {
        val bottomNav = bottomNavigationView ?: return
        if (bottomNav.selectedItemId != itemId) {
            bottomNav.selectedItemId = itemId
        }
    }
}