package com.dante.workcycle.data.repository

import android.content.Context
import com.dante.workcycle.WorkCycleApp

object RepositoryProvider {

    fun workLogRepository(context: Context): WorkLogRepository {
        val app = context.applicationContext as WorkCycleApp
        return WorkLogRepository(app.database.workLogDao())
    }

    fun workEventRepository(context: Context): WorkEventRepository {
        val app = context.applicationContext as WorkCycleApp
        return WorkEventRepository(app.database.workEventDao())
    }
}