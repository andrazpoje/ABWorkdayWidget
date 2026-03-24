// NOTE:
// This MUST be used instead of applying inset to parent container.
// Applying inset to parent will visually increase BottomNavigationView height.
package com.dante.abworkdaywidget

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

fun AppCompatActivity.setupBottomNavigation(selectedItemId: Int) {
    val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation) ?: return

    bottomNav.selectedItemId = selectedItemId

    bottomNav.setOnItemSelectedListener { item ->
        if (item.itemId == selectedItemId) {
            return@setOnItemSelectedListener true
        }

        when (item.itemId) {
            R.id.nav_home -> {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                true
            }

            R.id.nav_calendar -> {
                startActivity(
                    Intent(this, CalendarActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                true
            }

            R.id.nav_more -> {
                startActivity(
                    Intent(this, MoreActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                true
            }

            else -> false
        }
    }
}