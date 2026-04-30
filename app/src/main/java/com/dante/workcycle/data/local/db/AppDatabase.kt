package com.dante.workcycle.data.local.db

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dante.workcycle.data.local.dao.WorkEventDao
import com.dante.workcycle.data.local.dao.WorkLogDao
import com.dante.workcycle.data.local.entity.WorkEventEntity
import com.dante.workcycle.data.local.entity.WorkLogEntity

/**
 * Room database for Work Log aggregate rows and event timelines.
 *
 * Work Log data must not fall back to destructive migration because it can be
 * used as work time evidence. Schema changes should keep older events readable.
 */
@Database(
    entities = [
        WorkLogEntity::class,
        WorkEventEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(DbConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workLogDao(): WorkLogDao
    abstract fun workEventDao(): WorkEventDao

    companion object {
        const val DATABASE_VERSION = 3

        /**
         * Adds nullable manual-edit audit columns to existing Work Event rows.
         *
         * Existing events without audit metadata remain valid and render as
         * normal, unedited events.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE work_events ADD COLUMN editAuditOldDate TEXT")
                db.execSQL("ALTER TABLE work_events ADD COLUMN editAuditOldTime TEXT")
                db.execSQL("ALTER TABLE work_events ADD COLUMN editAuditNewDate TEXT")
                db.execSQL("ALTER TABLE work_events ADD COLUMN editAuditNewTime TEXT")
                db.execSQL("ALTER TABLE work_events ADD COLUMN editAuditEditedAt INTEGER")
                db.execSQL("ALTER TABLE work_events ADD COLUMN editAuditWasFutureTime INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE work_events ADD COLUMN editAuditSource TEXT")
            }
        }
    }
}
