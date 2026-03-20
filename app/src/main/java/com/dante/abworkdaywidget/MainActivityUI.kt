package com.dante.abworkdaywidget

import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Context

fun MainActivity.applyEdgeToEdgeInsets() {
    val scrollBottomPadding = resources.getDimensionPixelSize(R.dimen.main_scroll_bottom_padding)
    val saveBarExtraBottom = resources.getDimensionPixelSize(R.dimen.save_bar_extra_bottom_padding)

    ViewCompat.setOnApplyWindowInsetsListener(activityRoot) { _, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        mainScrollView.setPadding(
            mainScrollView.paddingLeft,
            mainScrollView.paddingTop,
            mainScrollView.paddingRight,
            scrollBottomPadding
        )

        saveBarContainer.setPadding(
            saveBarContainer.paddingLeft,
            saveBarContainer.paddingTop,
            saveBarContainer.paddingRight,
            systemBars.bottom + saveBarExtraBottom
        )

        insets
    }
}

fun MainActivity.hideAllSections() {
    cycleSection.visibility = View.GONE
    rulesSection.visibility = View.GONE
    displaySection.visibility = View.GONE
}

fun MainActivity.resetArrows() {
    cycleArrow.text = "▼"
    rulesArrow.text = "▼"
    displayArrow.text = "▼"
}

fun MainActivity.setupSection(
    header: View,
    section: View,
    arrow: TextView,
    sectionKey: String
) {
    header.setOnClickListener {
        val parent = section.parent as ViewGroup
        TransitionManager.beginDelayedTransition(parent, AutoTransition())

        if (section.visibility == View.VISIBLE) {
            section.visibility = View.GONE
            arrow.text = "▼"
            saveLastOpenSection("")
        } else {
            hideAllSections()
            resetArrows()

            section.visibility = View.VISIBLE
            arrow.text = "▲"
            saveLastOpenSection(sectionKey)
        }
    }
}

fun MainActivity.saveLastOpenSection(sectionKey: String) {
    getSharedPreferences(MainActivity.PREFS_UI, Context.MODE_PRIVATE)
        .edit()
        .putString(MainActivity.KEY_LAST_OPEN_SECTION, sectionKey)
        .apply()
}

fun MainActivity.restoreLastOpenSection() {
    val lastSection = getSharedPreferences(MainActivity.PREFS_UI, Context.MODE_PRIVATE)
        .getString(MainActivity.KEY_LAST_OPEN_SECTION, MainActivity.SECTION_CYCLE)

    hideAllSections()
    resetArrows()

    when (lastSection) {
        MainActivity.SECTION_RULES -> {
            rulesSection.visibility = View.VISIBLE
            rulesArrow.text = "▲"
        }

        MainActivity.SECTION_DISPLAY -> {
            displaySection.visibility = View.VISIBLE
            displayArrow.text = "▲"
        }

        else -> {
            cycleSection.visibility = View.VISIBLE
            cycleArrow.text = "▲"
        }
    }
}