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
