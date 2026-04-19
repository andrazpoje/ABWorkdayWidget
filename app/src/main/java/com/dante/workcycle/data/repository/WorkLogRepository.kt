package com.dante.workcycle.data.repository

import com.dante.workcycle.data.local.dao.WorkLogDao
import com.dante.workcycle.data.local.entity.toDomain
import com.dante.workcycle.data.local.entity.toEntity
import com.dante.workcycle.domain.model.WorkLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class WorkLogRepository(
    private val dao: WorkLogDao
) {

    suspend fun getByDate(date: LocalDate): WorkLog? {
        return dao.getByDate(date)?.toDomain()
    }

    fun observeByDate(date: LocalDate): Flow<WorkLog?> {
        return dao.observeByDate(date).map { it?.toDomain() }
    }

    fun observeAll(): Flow<List<WorkLog>> {
        return dao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    suspend fun save(log: WorkLog): Long {
        return dao.insert(
            log.copy(updatedAt = System.currentTimeMillis()).toEntity()
        )
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteByDate(date: LocalDate) {
        dao.deleteByDate(date)
    }
}