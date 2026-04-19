package com.dante.workcycle.data.local.db

import androidx.room.TypeConverter
import com.dante.workcycle.domain.model.WorkEventType
import java.time.LocalDate
import java.time.LocalTime

class DbConverters {

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? =
        value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? =
        value?.let { LocalTime.parse(it) }

    @TypeConverter
    fun fromWorkEventType(value: WorkEventType?): String? = value?.name

    @TypeConverter
    fun toWorkEventType(value: String?): WorkEventType? =
        value?.let { WorkEventType.valueOf(it) }
}