package com.dante.abworkdaywidget

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.core.content.edit
import androidx.core.view.isVisible
import com.dante.abworkdaywidget.MainActivity.Companion.KEY_LAST_OPEN_SECTION
import com.dante.abworkdaywidget.MainActivity.Companion.PREFS_UI
import com.dante.abworkdaywidget.MainActivity.Companion.SECTION_CYCLE
import com.dante.abworkdaywidget.MainActivity.Companion.SECTION_DISPLAY
import com.dante.abworkdaywidget.MainActivity.Companion.SECTION_RULES

fun MainActivity.setupSection(
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

fun MainActivity.hideAllSections() {
    cycleSection.isVisible = false
    rulesSection.isVisible = false
    displaySection.isVisible = false
}

fun MainActivity.resetArrows() {
    cycleArrow.setImageResource(R.drawable.ic_expand_more_24)
    rulesArrow.setImageResource(R.drawable.ic_expand_more_24)
    displayArrow.setImageResource(R.drawable.ic_expand_more_24)
}

fun MainActivity.saveLastOpenSection(sectionKey: String) {
    getSharedPreferences(PREFS_UI, Context.MODE_PRIVATE).edit {
        putString(KEY_LAST_OPEN_SECTION, sectionKey)
    }
}

fun MainActivity.restoreLastOpenSection() {
    val lastSection = getSharedPreferences(
        PREFS_UI,
        Context.MODE_PRIVATE
    ).getString(KEY_LAST_OPEN_SECTION, SECTION_CYCLE)

    hideAllSections()
    resetArrows()

    when (lastSection) {
        SECTION_RULES -> {
            rulesSection.isVisible = true
            rulesArrow.setImageResource(R.drawable.ic_expand_less_24)
        }

        SECTION_DISPLAY -> {
            displaySection.isVisible = true
            displayArrow.setImageResource(R.drawable.ic_expand_less_24)
        }

        else -> {
            cycleSection.isVisible = true
            cycleArrow.setImageResource(R.drawable.ic_expand_less_24)
        }
    }
}