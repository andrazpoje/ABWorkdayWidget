package com.dante.workcycle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dante.workcycle.data.local.entity.WorkEventEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WorkEventDao {

    @Query("SELECT * FROM work_events WHERE date = :date ORDER BY time ASC, id ASC")
    suspend fun getByDate(date: LocalDate): List<WorkEventEntity>

    @Query("SELECT * FROM work_events WHERE date = :date ORDER BY time ASC, id ASC")
    fun observeByDate(date: LocalDate): Flow<List<WorkEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: WorkEventEntity): Long

    @Update
    suspend fun update(event: WorkEventEntity)

    @Query("DELETE FROM work_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM work_events WHERE date = :date")
    suspend fun deleteByDate(date: LocalDate)
}
