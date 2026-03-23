package com.dante.abworkdaywidget

import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun MainActivity.applyEdgeToEdgeInsets() {
    val mainContentContainer = findViewById<View>(R.id.mainContentContainer)

    val initialContentLeft = mainContentContainer.paddingLeft
    val initialContentTop = mainContentContainer.paddingTop
    val initialContentRight = mainContentContainer.paddingRight
    val initialContentBottom = mainContentContainer.paddingBottom

    val initialBottomBarsLeft = bottomBarsContainer.paddingLeft
    val initialBottomBarsTop = bottomBarsContainer.paddingTop
    val initialBottomBarsRight = bottomBarsContainer.paddingRight
    val initialBottomBarsBottom = bottomBarsContainer.paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(mainContentContainer) { view, insets ->
        val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())

        view.updatePadding(
            left = initialContentLeft,
            top = initialContentTop + statusBars.top,
            right = initialContentRight,
            bottom = initialContentBottom
        )

        insets
    }

    ViewCompat.setOnApplyWindowInsetsListener(bottomBarsContainer) { view, insets ->
        val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

        view.updatePadding(
            left = initialBottomBarsLeft,
            top = initialBottomBarsTop,
            right = initialBottomBarsRight,
            bottom = initialBottomBarsBottom + navigationBars.bottom
        )

        insets
    }

    ViewCompat.requestApplyInsets(mainContentContainer)
    ViewCompat.requestApplyInsets(bottomBarsContainer)
}

fun MainActivity.hideAllSections() {
    cycleSection.visibility = View.GONE
    rulesSection.visibility = View.GONE
    displaySection.visibility = View.GONE
}

fun MainActivity.resetArrows() {
    cycleArrow.setImageResource(R.drawable.ic_expand_more_24)
    rulesArrow.setImageResource(R.drawable.ic_expand_more_24)
    displayArrow.setImageResource(R.drawable.ic_expand_more_24)
}

fun MainActivity.setupSection(
    header: View,
    section: View,
    arrow: ImageView,
    sectionKey: String
) {
    header.setOnClickListener {
        val parent = section.parent as ViewGroup
        TransitionManager.beginDelayedTransition(parent, AutoTransition())

        if (section.visibility == View.VISIBLE) {
            section.visibility = View.GONE
            arrow.setImageResource(R.drawable.ic_expand_more_24)
            saveLastOpenSection("")
        } else {
            hideAllSections()
            resetArrows()

            section.visibility = View.VISIBLE
            arrow.setImageResource(R.drawable.ic_expand_less_24)
            saveLastOpenSection(sectionKey)
        }
    }
}

fun MainActivity.saveLastOpenSection(sectionKey: String) {
    getSharedPreferences(MainActivity.PREFS_UI, android.content.Context.MODE_PRIVATE)
        .edit()
        .putString(MainActivity.KEY_LAST_OPEN_SECTION, sectionKey)
        .apply()
}

fun MainActivity.restoreLastOpenSection() {
    val lastSection = getSharedPreferences(
        MainActivity.PREFS_UI,
        android.content.Context.MODE_PRIVATE
    ).getString(MainActivity.KEY_LAST_OPEN_SECTION, MainActivity.SECTION_CYCLE)

    hideAllSections()
    resetArrows()

    when (lastSection) {
        MainActivity.SECTION_RULES -> {
            rulesSection.visibility = View.VISIBLE
            rulesArrow.setImageResource(R.drawable.ic_expand_less_24)
        }

        MainActivity.SECTION_DISPLAY -> {
            displaySection.visibility = View.VISIBLE
            displayArrow.setImageResource(R.drawable.ic_expand_less_24)
        }

        else -> {
            cycleSection.visibility = View.VISIBLE
            cycleArrow.setImageResource(R.drawable.ic_expand_less_24)
        }
    }
}