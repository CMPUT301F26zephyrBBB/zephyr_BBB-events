package com.example.zephyr_bbb.owner

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Holds the text an owner has typed into the Create / Edit Lottery form.
 * Everything is kept as text so the form can show exactly what was typed,
 * then validate() checks it before it is turned into a Lottery.
 * The stay end date is the checkout date, so it is excluded, matching the Lottery model.
 * @property title The lottery name
 * @property stayStart First night winners can stay, YYYY-MM-DD
 * @property stayEnd Last checkout date, YYYY-MM-DD
 * @property campsiteIds The IDs of the campsites the owner has ticked
 * @property bookingStart First day of booking, YYYY-MM-DD
 * @property bookingEnd Last day of booking, YYYY-MM-DD
 * @property winnerCount Number of winners to draw
 * @property waitlistCapacity Maximum waitlist size, blank for unlimited
 **/
data class LotteryFormFields(
    val title: String = "",
    val campsiteIds: Set<String> = emptySet(),
    val registrationStart: String = "",
    val registrationEnd: String = "",
    val bookingStart: String = "",
    val bookingEnd: String = "",
    val stayStart: String = "",
    val stayEnd: String = "",
    val winnerCount: String = "1",
    val waitlistCapacity: String = ""
)

/**
 * Checks the form for missing or badly formatted values, so the owner gets a message before
 *  anything is sent to the database.
 *  Checks that there is a lottery name, and that the stay dates are real dates with the stay
 *  ending after it starts.
 *  Checks that at least one campsite is chosen.
 *  Checks that the registration dates are real dates and registration ends after it starts.
 *  Checks that the booking dates are real dates, booking ends after it starts, and booking
 *  stays open after registration ends.
 *  Checks that there is at least one winner.
 *  Checks that the waitlist capacity is blank (unlimited) or a positive number.
 *  @param timeZone The time zone the dates are read in, normally the phone's
 *  @return An error message to show the owner, or null if the form is ready to publish
 */
fun LotteryFormFields.validate(timeZone: TimeZone = TimeZone.getDefault()): String? {
    if (title.isBlank()) {
        return "Enter a lottery name."
    }
    if (campsiteIds.isEmpty()) {
        return "Choose at least one campsite."
    }
    val registrationOpens = parseLotteryDate(registrationStart, timeZone)
        ?: return "Enter the registration start as YYYY-MM-DD."
    val registrationCloses = parseLotteryEndDate(registrationEnd, timeZone)
        ?: return "Enter the registration end as YYYY-MM-DD."
    val bookingOpens = parseLotteryDate(bookingStart, timeZone)
        ?: return "Enter the booking start as YYYY-MM-DD."
    val bookingCloses = parseLotteryEndDate(bookingEnd, timeZone)
        ?: return "Enter the booking end as YYYY-MM-DD."
    val stayStartDate = parseLotteryDate(stayStart, timeZone)
        ?: return "Enter the stay start as YYYY-MM-DD."
    val stayEndDate = parseLotteryDate(stayEnd, timeZone)
        ?: return "Enter the stay end as YYYY-MM-DD."
    if (stayEndDate <= stayStartDate) {
        return "The stay must end after it starts."
    }
    if (registrationCloses <= registrationOpens) {
        return "Registration must end on or after the day it starts."
    }
    if (bookingCloses <= bookingOpens) {
        return "Booking must end on or after the day it starts."
    }

    if (bookingCloses <= registrationCloses) {
        return "Booking must stay open after registration ends."
    }
    if (stayEndDate <= stayStartDate) {
        return "The stay must end after it starts."
    }
    val winners = winnerCount.toLongOrNull()

    if (winners == null || winners < 1) {
        return "Choose at least one winner."
    }

    if (waitlistCapacity.isNotBlank()) {
        val capacity = waitlistCapacity.toLongOrNull()

        if (capacity == null || capacity < 1) {
            return "Waitlist capacity must be a positive number or left blank."
        }
    }
    return null
}

/**
 * Returns the waitlist capacity the owner entered, or null for an unlimited waitlist.
 */
fun LotteryFormFields.waitlistCapacityOrNull(): Long? {
    return waitlistCapacity.trim().toLongOrNull()
}

/**
 * Turns YYYY-MM-DD into the very start of that day.
 * Returns null for anything that isn't a real calendar date (e.g. 2026-02-30).
 */
fun parseLotteryDate(
    text: String,
    timeZone: TimeZone = TimeZone.getDefault()
): Date? {
    val trimmed = text.trim()

    if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(trimmed)) {
        return null
    }

    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    format.timeZone = timeZone
    format.isLenient = false

    return try {
        format.parse(trimmed)
    } catch (e: ParseException) {
        null
    }
}

/**
 * Turns an inclusive end date (YYYY-MM-DD) into the moment it closes,
 * which is the start of the following day.
 * Example: a registration end of 2026-10-14 closes at midnight starting 2026-10-15.
 */
fun parseLotteryEndDate(
    text: String,
    timeZone: TimeZone = TimeZone.getDefault()
): Date? {
    val date = parseLotteryDate(text, timeZone) ?: return null

    val calendar = Calendar.getInstance(timeZone)
    calendar.time = date
    calendar.add(Calendar.DAY_OF_MONTH, 1)

    return calendar.time
}

/**
 * Formats a date as YYYY-MM-DD so it can be put back into a form field.
 */
fun formatLotteryDate(
    date: Date?,
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    if (date == null) {
        return ""
    }

    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    format.timeZone = timeZone

    return format.format(date)
}

/**
 * Formats a closing moment back into the inclusive end date the owner typed.
 * This undoes parseLotteryEndDate.
 */
fun formatLotteryEndDate(
    closesAt: Date?,
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    if (closesAt == null) {
        return ""
    }

    return formatLotteryDate(Date(closesAt.time - 1), timeZone)
}

/**
 * Formats a period for display, e.g. "Oct 1–14, 2026" or "Oct 28 – Nov 3, 2026".
 *
 * @param start The first day of the period
 * @param end The last day of the period
 */
fun formatDateRange(
    start: Date?,
    end: Date?,
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    if (start == null || end == null) {
        return "Not set"
    }

    val startCalendar = Calendar.getInstance(timeZone)
    startCalendar.time = start
    val endCalendar = Calendar.getInstance(timeZone)
    endCalendar.time = end

    fun format(pattern: String, date: Date): String {
        val format = SimpleDateFormat(pattern, Locale.US)
        format.timeZone = timeZone
        return format.format(date)
    }

    val sameYear = startCalendar.get(Calendar.YEAR) == endCalendar.get(Calendar.YEAR)
    val sameMonth = sameYear &&
            startCalendar.get(Calendar.MONTH) == endCalendar.get(Calendar.MONTH)

    return when {
        sameMonth -> "${format("MMM d", start)}–${format("d, yyyy", end)}"
        sameYear -> "${format("MMM d", start)} – ${format("MMM d, yyyy", end)}"
        else -> "${format("MMM d, yyyy", start)} – ${format("MMM d, yyyy", end)}"
    }
}

/**
 * Formats a period whose end is a closing moment (registration or booking),
 * showing the last open day rather than the day after.
 */
fun formatClosingRange(
    opensAt: Date?,
    closesAt: Date?,
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    val lastDay = closesAt?.let { Date(it.time - 1) }
    return formatDateRange(opensAt, lastDay, timeZone)
}

/**
 * Formats a stay period stored as YYYY-MM-DD strings, e.g. "Oct 18–21, 2026".
 * The end is the checkout date.
 */
fun formatStayRange(
    stayStartDate: String,
    stayEndDate: String,
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    return formatDateRange(
        parseLotteryDate(stayStartDate, timeZone),
        parseLotteryDate(stayEndDate, timeZone),
        timeZone
    )
}

/**
 * Turns the stored lottery status and its registration window into the label owners see.
 * "open" is split into Upcoming / Active / Drawing winners depending on the date.
 * The registration and booking end dates are inclusive (the last day people can register or book).
 *
 * @param status The status saved in the database ("open", "drawn", "cancelled")
 * @param registrationOpensAt When registration starts
 * @param registrationClosesAt When registration ends
 * @param now The current time
 */
fun lotteryStatusLabel(
    status: String,
    registrationOpensAt: Date?,
    registrationClosesAt: Date?,
    now: Date
): String {
    return when (status) {
        "drawn" -> "Completed"
        "cancelled" -> "Cancelled"
        "open" -> when {
            registrationOpensAt != null && now < registrationOpensAt -> "Upcoming"
            registrationClosesAt != null && now < registrationClosesAt -> "Active"
            else -> "Drawing winners"
        }
        else -> status.replaceFirstChar { it.uppercase() }
    }
}

fun canEditLottery(status: String): Boolean {
    return status == "open"
}