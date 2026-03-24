// GLOBAL EDGE-TO-EDGE STRATEGY
//
// This project uses STRICT inset rules:
//
// 1) Top (status bar):
//    - MainActivity → margin on scroll container
//    - Other activities → padding on content container
//
// 2) Bottom (navigation bar):
//    - ALWAYS apply to BottomNavigationView directly
//    - NEVER apply to parent containers (causes layout inflation)
//
// 3) IME (keyboard):
//    - Apply ONLY to views that must stay above keyboard (e.g. save bar)
//
// Breaking these rules will cause:
// - inconsistent bottom nav sizes
// - content overlapping system bars
// - layout flickering or resizing issues
package com.dante.abworkdaywidget

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlin.math.max

fun ComponentActivity.setupDefaultEdgeToEdge() {
    enableEdgeToEdge()
}

fun ComponentActivity.updateSystemBarIconContrast(anchorView: View) {
    val controller = WindowCompat.getInsetsController(window, anchorView)
    val isDarkMode =
        (anchorView.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

    controller.isAppearanceLightStatusBars = !isDarkMode
    controller.isAppearanceLightNavigationBars = !isDarkMode
}

fun View.applyTopSystemBarInsetAsPadding() {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val statusBars: Insets = insets.getInsets(WindowInsetsCompat.Type.statusBars())

        view.updatePadding(
            left = initialLeft,
            top = initialTop + statusBars.top,
            right = initialRight,
            bottom = initialBottom
        )

        insets
    }

    ViewCompat.requestApplyInsets(this)
}

fun View.applyBottomNavInsetAsPadding() {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val navigationBars: Insets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

        view.updatePadding(
            left = initialLeft,
            top = initialTop,
            right = initialRight,
            bottom = initialBottom + navigationBars.bottom
        )

        insets
    }

    ViewCompat.requestApplyInsets(this)
}

fun View.applyTopStatusBarInsetAsMargin() {
    val lp = layoutParams as? ViewGroup.MarginLayoutParams ?: return
    val initialTopMargin = lp.topMargin

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        val marginLp = view.layoutParams as ViewGroup.MarginLayoutParams

        val targetMargin = initialTopMargin + topInset
        if (marginLp.topMargin != targetMargin) {
            marginLp.topMargin = targetMargin
            view.layoutParams = marginLp
        }

        insets
    }

    ViewCompat.requestApplyInsets(this)
}

fun View.applyBottomSystemInsetWithImeAsPadding() {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
        val bottomInset = max(navBars.bottom, imeInsets.bottom)

        view.updatePadding(
            left = initialLeft,
            top = initialTop,
            right = initialRight,
            bottom = initialBottom + bottomInset
        )

        insets
    }

    ViewCompat.requestApplyInsets(this)
}