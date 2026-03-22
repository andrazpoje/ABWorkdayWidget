package com.dante.abworkdaywidget

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

fun Context.startActivityNoAnimation(intent: Intent) {
    startActivity(
        intent,
        ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()
    )
}

fun MainActivity.setupBottomNavigation() {
    val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
    bottomNav.selectedItemId = R.id.nav_home

    bottomNav.setOnItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_home -> true

            R.id.nav_calendar -> {
                startActivityNoAnimation(
                    Intent(this, CalendarActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                )
                true
            }

            R.id.nav_more -> {
                startActivityNoAnimation(
                    Intent(this, MoreActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                )
                true
            }

            else -> false
        }
    }
}

fun CalendarActivity.setupBottomNavigation() {
    val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
    bottomNav.selectedItemId = R.id.nav_calendar

    bottomNav.setOnItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_home -> {
                startActivityNoAnimation(
                    Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                )
                true
            }

            R.id.nav_calendar -> true

            R.id.nav_more -> {
                startActivityNoAnimation(
                    Intent(this, MoreActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                )
                true
            }

            else -> false
        }
    }
}

fun MoreActivity.setupBottomNavigation() {
    val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
    bottomNav.selectedItemId = R.id.nav_more

    bottomNav.setOnItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_home -> {
                startActivityNoAnimation(
                    Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                )
                true
            }

            R.id.nav_calendar -> {
                startActivityNoAnimation(
                    Intent(this, CalendarActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                )
                true
            }

            R.id.nav_more -> true

            else -> false
        }
    }
}