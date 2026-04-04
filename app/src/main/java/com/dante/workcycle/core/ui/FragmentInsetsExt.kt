package com.dante.workcycle.core.ui

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.applySystemBarsBottomInsetAsPadding() {
    val initialPaddingLeft = paddingLeft
    val initialPaddingTop = paddingTop
    val initialPaddingRight = paddingRight
    val initialPaddingBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

        val bottomInset = maxOf(systemBars.bottom, ime.bottom)

        v.setPadding(
            initialPaddingLeft,
            initialPaddingTop,
            initialPaddingRight,
            initialPaddingBottom + bottomInset
        )

        insets
    }

    ViewCompat.requestApplyInsets(this)
}

fun View.applyImeInsetAsPadding() {
    val initialPaddingLeft = paddingLeft
    val initialPaddingTop = paddingTop
    val initialPaddingRight = paddingRight
    val initialPaddingBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

        v.setPadding(
            initialPaddingLeft,
            initialPaddingTop,
            initialPaddingRight,
            initialPaddingBottom + ime.bottom
        )

        insets
    }

    ViewCompat.requestApplyInsets(this)
}
fun View.applySystemBarsHorizontalInsetAsPadding() {
    val initialPaddingLeft = paddingLeft
    val initialPaddingTop = paddingTop
    val initialPaddingRight = paddingRight
    val initialPaddingBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        v.setPadding(
            initialPaddingLeft + systemBars.left,
            initialPaddingTop,
            initialPaddingRight + systemBars.right,
            initialPaddingBottom
        )

        insets
    }

    ViewCompat.requestApplyInsets(this)
}