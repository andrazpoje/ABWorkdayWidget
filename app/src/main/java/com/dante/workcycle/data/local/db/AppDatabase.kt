package com.dante.workcycle.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dante.workcycle.data.local.dao.WorkEventDao
import com.dante.workcycle.data.local.dao.WorkLogDao
import com.dante.workcycle.data.local.entity.WorkEventEntity
import com.dante.workcycle.data.local.entity.WorkLogEntity

@Database(
    entities = [
        WorkLogEntity::class,
        WorkEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DbConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workLogDao(): WorkLogDao
    abstract fun workEventDao(): WorkEventDao
}