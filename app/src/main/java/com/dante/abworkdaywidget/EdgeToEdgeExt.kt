package com.dante.abworkdaywidget

import android.content.res.Configuration
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

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

fun View.applySystemBarInsetsToPadding(
    addTop: Boolean = false,
    addBottom: Boolean = false,
    addLeft: Boolean = false,
    addRight: Boolean = false,
    extraBottom: Int = 0,
    insetTypes: Int = WindowInsetsCompat.Type.systemBars()
) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars: Insets = insets.getInsets(insetTypes)

        view.updatePadding(
            left = initialLeft + if (addLeft) systemBars.left else 0,
            top = initialTop + if (addTop) systemBars.top else 0,
            right = initialRight + if (addRight) systemBars.right else 0,
            bottom = initialBottom + if (addBottom) systemBars.bottom + extraBottom else 0
        )

        insets
    }

    ViewCompat.requestApplyInsets(this)
}