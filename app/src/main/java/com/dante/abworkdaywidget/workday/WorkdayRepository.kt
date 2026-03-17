package com.dante.abworkdaywidget.workday

import android.content.Context
import com.dante.abworkdaywidget.ABLogic

object WorkdayRepository {

    fun getTodayLabel(context: Context): String {
        return ABLogic.getTodayLetter(context)
    }
}