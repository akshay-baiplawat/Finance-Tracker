package com.example.financetracker.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object DateUtils {

    fun getCurrentMonthStartAndEnd(): Pair<Long, Long> {
        val now = LocalDate.now()
        val startOfMonth = YearMonth.from(now).atDay(1).atStartOfDay(ZoneId.systemDefault())
        val endOfMonth = YearMonth.from(now).atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault())

        return Pair(startOfMonth.toInstant().toEpochMilli(), endOfMonth.toInstant().toEpochMilli())
    }
}
