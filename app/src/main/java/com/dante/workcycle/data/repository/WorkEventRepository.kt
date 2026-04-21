package com.dante.workcycle.data.repository

import com.dante.workcycle.data.local.dao.WorkEventDao
import com.dante.workcycle.data.local.entity.toDomain
import com.dante.workcycle.data.local.entity.toEntity
import com.dante.workcycle.domain.model.WorkEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class WorkEventRepository(
    private val dao: WorkEventDao
) {

    suspend fun getByDate(date: LocalDate): List<WorkEvent> {
        return dao.getByDate(date).map { it.toDomain() }
    }

    fun observeByDate(date: LocalDate): Flow<List<WorkEvent>> {
        return dao.observeByDate(date).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun insert(event: WorkEvent): Long {
        return dao.insert(event.toEntity())
    }

    suspend fun update(event: WorkEvent) {
        dao.update(event.toEntity())
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteByDate(date: LocalDate) {
        dao.deleteByDate(date)
    }
}
