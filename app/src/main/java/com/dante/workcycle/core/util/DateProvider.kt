package com.dante.workcycle.core.util

import java.time.LocalDate
import java.time.ZoneId

object DateProvider {
    fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
}