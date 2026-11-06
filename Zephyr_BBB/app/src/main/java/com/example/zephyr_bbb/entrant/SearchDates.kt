package com.example.zephyr_bbb.entrant

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Date helpers for the entrant search
// The db uses YYYY-MM-DD strings (checkout excluded) and the Material3 date picker works in
// UTC milliseconds, so these functions convert between them
// Outstanding issues: none known.

private const val ISO_PATTERN = "yyyy-MM-dd"
private const val CLOSES_PATTERN = "MMM d"

private val utc: TimeZone = TimeZone.getTimeZone("UTC")

private fun isoFormat() = SimpleDateFormat(ISO_PATTERN, Locale.US).apply {
    timeZone = utc
    isLenient = false
}

/**
 * Converts the UTC milliseconds given by the date picker to a YYYY-MM-DD string.
 *
 * @param utcMillis milliseconds since midnight UTC of the picked day
 */
fun millisToIsoDate(utcMillis: Long): String = isoFormat().format(Date(utcMillis))

/**
 * Converts a YYYY-MM-DD string to UTC milliseconds at midnight of that day.
 *
 * @return the milliseconds, or null if [date] is not a real date in YYYY-MM-DD format
 */
fun isoDateToMillis(date: String): Long? = try {
    isoFormat().parse(date)?.time
} catch (_: ParseException) {
    null
}

/**
 * Midnight UTC of the current day in [zone], which is the format the date picker uses.
 * Used so that dates before today cant be picked.
 *
 * @param now the current time in milliseconds
 * @param zone the time zone the user is in
 */
fun todayUtcMillis(
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault()
): Long {
    val local = Calendar.getInstance(zone).apply { timeInMillis = now }
    return Calendar.getInstance(utc).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/**
 * Text describing when a lottery stops taking registrations, ex:
 * "Lottery open · closes Oct 22".
 *
 * @param closesAtMillis when registration closes, or null if unknown
 * @param zone the time zone used to pick the day that is shown
 */
fun lotteryStatusText(
    closesAtMillis: Long?,
    zone: TimeZone = TimeZone.getDefault()
): String {
    if (closesAtMillis == null) return "Lottery open"
    val format = SimpleDateFormat(CLOSES_PATTERN, Locale.US).apply { timeZone = zone }
    return "Lottery open · closes ${format.format(Date(closesAtMillis))}"
}
