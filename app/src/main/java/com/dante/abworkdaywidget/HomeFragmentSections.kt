package com.dante.abworkdaywidget

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.core.content.edit
import androidx.core.view.isVisible

fun HomeFragment.setupSection(
    header: View,
    section: View,
    arrow: ImageView,
    sectionKey: String
) {
    header.setOnClickListener {
        if (section.isVisible) {
            section.isVisible = false
            arrow.setImageResource(R.drawable.ic_expand_more_24)
            saveLastOpenSection("")
        } else {
            hideAllSections()
            resetArrows()

            section.isVisible = true
            arrow.setImageResource(R.drawable.ic_expand_less_24)
            saveLastOpenSection(sectionKey)
        }
    }
}

fun HomeFragment.hideAllSections() {
    cycleSection.isVisible = false
    rulesSection.isVisible = false
    displaySection.isVisible = false
}

fun HomeFragment.resetArrows() {
    cycleArrow.setImageResource(R.drawable.ic_expand_more_24)
    rulesArrow.setImageResource(R.drawable.ic_expand_more_24)
    displayArrow.setImageResource(R.drawable.ic_expand_more_24)
}

fun HomeFragment.saveLastOpenSection(sectionKey: String) {
    requireContext().getSharedPreferences(HomeFragment.PREFS_UI, Context.MODE_PRIVATE).edit {
        putString(HomeFragment.KEY_LAST_OPEN_SECTION, sectionKey)
    }
}

fun HomeFragment.restoreLastOpenSection() {
    val lastSection = requireContext()
        .getSharedPreferences(HomeFragment.PREFS_UI, Context.MODE_PRIVATE)
        .getString(HomeFragment.KEY_LAST_OPEN_SECTION, HomeFragment.SECTION_CYCLE)

    hideAllSections()
    resetArrows()

    when (lastSection) {
        HomeFragment.SECTION_RULES -> {
            rulesSection.isVisible = true
            rulesArrow.setImageResource(R.drawable.ic_expand_less_24)
        }

        HomeFragment.SECTION_DISPLAY -> {
            displaySection.isVisible = true
            displayArrow.setImageResource(R.drawable.ic_expand_less_24)
        }

        else -> {
            cycleSection.isVisible = true
            cycleArrow.setImageResource(R.drawable.ic_expand_less_24)
        }
    }
}