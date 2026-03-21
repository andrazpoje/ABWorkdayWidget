package com.dante.abworkdaywidget

import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun MainActivity.applyEdgeToEdgeInsets() {
    val extraSpacing = resources.getDimensionPixelSize(R.dimen.save_bar_extra_bottom_padding)

    ViewCompat.setOnApplyWindowInsetsListener(activityRoot) { _, insets ->
        val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
        val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

        val bottomInset = maxOf(navBars.bottom, imeInsets.bottom)

        activityRoot.setPadding(
            activityRoot.paddingLeft,
            statusBars.top,
            activityRoot.paddingRight,
            activityRoot.paddingBottom
        )

        saveBarContainer.post {
            val saveBarHeight = saveBarContainer.height

            mainScrollView.setPadding(
                0,
                0,
                0,
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
    val lastSection = getSharedPreferences(MainActivity.PREFS_UI, android.content.Context.MODE_PRIVATE)
        .getString(MainActivity.KEY_LAST_OPEN_SECTION, MainActivity.SECTION_CYCLE)

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