package com.example.zephyr_bbb.entrant

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Small date and money helpers for the entrant screens.
 * Dates use the same "yyyy-MM-dd" UTC format as the repositories.
 */
object EntrantUtils {

    private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /** Returns the day after [date], e.g. "2026-07-01" -> "2026-07-02". */
    fun nextDay(date: String): String {
        val format = dateFormat()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = format.parse(date)!!
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        return format.format(calendar.time)
    }

    /** Every night from [first] to [last], both included. */
    fun nightsBetween(first: String, last: String): List<String> {
        val nights = mutableListOf<String>()
        var night = first
        while (night <= last) {
            nights.add(night)
            night = nextDay(night)
        }
        return nights
    }

    /** 12345 cents in CAD -> "$123.45 CAD". */
    fun formatPrice(cents: Long, currency: String): String =
        String.format(Locale.ROOT, "$%.2f %s", cents / 100.0, currency)
}

