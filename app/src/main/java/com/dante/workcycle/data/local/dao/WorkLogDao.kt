package com.dante.workcycle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dante.workcycle.data.local.entity.WorkLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Room access for the legacy daily Work Log aggregate table.
 *
 * Keep this separate from [WorkEventDao]: aggregate rows are compatibility data,
 * while the current dashboard state is derived from event timelines.
 */
@Dao
interface WorkLogDao {

    @Query("SELECT * FROM work_logs ORDER BY date ASC, id ASC")
    suspend fun getAllOrdered(): List<WorkLogEntity>

    @Query("SELECT * FROM work_logs WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: LocalDate): WorkLogEntity?

    @Query("SELECT * FROM work_logs WHERE date = :date LIMIT 1")
    fun observeByDate(date: LocalDate): Flow<WorkLogEntity?>

    @Query("SELECT * FROM work_logs ORDER BY date DESC")
    fun observeAll(): Flow<List<WorkLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WorkLogEntity): Long

    @Query("DELETE FROM work_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM work_logs WHERE date = :date")
    suspend fun deleteByDate(date: LocalDate)
}
