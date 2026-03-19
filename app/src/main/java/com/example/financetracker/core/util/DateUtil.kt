package com.example.financetracker.core.util

import android.content.Context
import android.text.format.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtil {
    private val dateOnlyFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    /**
     * Formats timestamp with date and time: "14 Mar 2026, 02:47 PM" or "14 Mar 2026, 14:47"
     * Respects device's 24-hour format setting.
     */
    fun formatDateWithTime(timestamp: Long, context: Context): String {
        return try {
            val instant = Instant.ofEpochMilli(timestamp)
            val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
            val is24Hour = DateFormat.is24HourFormat(context)
            val pattern = if (is24Hour) "dd MMM yyyy, HH:mm" else "dd MMM yyyy, hh:mm a"
            localDateTime.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Formats timestamp with date only: "14 Mar 2026"
     * Used for grouping headers in Ledger screen.
     */
    fun formatDateOnly(timestamp: Long): String {
        return try {
            val instant = Instant.ofEpochMilli(timestamp)
            val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
            localDate.format(dateOnlyFormatter)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Legacy method for backward compatibility.
     * @deprecated Use formatDateWithTime() or formatDateOnly() instead.
     */
    fun formatTimestamp(timestamp: Long): String {
        return formatDateOnly(timestamp)
    }
}
