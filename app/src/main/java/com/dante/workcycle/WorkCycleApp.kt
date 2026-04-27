package com.dante.workcycle

import android.app.Application
import com.dante.workcycle.core.theme.AppThemeManager
import androidx.room.Room
import com.dante.workcycle.data.local.db.AppDatabase
class WorkCycleApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        AppThemeManager.applyFromPreferences(this)

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "workcycle.db"
        )
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }
}
