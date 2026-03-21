package com.dante.abworkdaywidget

import android.content.res.Resources
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.FrameLayout

fun MainActivity.applyEdgeToEdgeInsets() {
    val extraSpacing = resources.getDimensionPixelSize(R.dimen.save_bar_extra_bottom_padding)

    ViewCompat.setOnApplyWindowInsetsListener(activityRoot) { _, insets ->
        val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
        val bottomInset = maxOf(navBars.bottom, imeInsets.bottom)

        saveBarContainer.post {
            val saveBarHeight = saveBarContainer.height

            mainScrollView.setPadding(
                mainScrollView.paddingLeft,
                mainScrollView.paddingTop,
                mainScrollView.paddingRight,
                saveBarHeight + bottomInset + extraSpacing
            )

            val lp = saveBarContainer.layoutParams as FrameLayout.LayoutParams
            lp.bottomMargin = bottomInset
            saveBarContainer.layoutParams = lp
        }

        insets
    }

    ViewCompat.requestApplyInsets(activityRoot)
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
    getSharedPreferences(MainActivity.PREFS_UI, android.content.Context.MODE_PRIVATE)
        .edit()
        .putString(MainActivity.KEY_LAST_OPEN_SECTION, sectionKey)
        .apply()
}

fun MainActivity.restoreLastOpenSection() {
    val lastSection = getSharedPreferences(MainActivity.PREFS_UI, android.content.Context.MODE_PRIVATE)
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