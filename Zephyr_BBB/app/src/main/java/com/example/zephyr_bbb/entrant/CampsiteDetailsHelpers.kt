package com.example.zephyr_bbb.entrant

import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.database.model.LotteryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Small helpers for the campsite details screen including the order the lotteries are shown in, what
// the join button should do and the text shown for the price and the lotteries
// Outstanding issues: none known.

private const val DAY_PATTERN = "MMM d"

/** What the main lottery button on the details screen should do */
enum class JoinAction {
    /** The entrant is not in the lottery and can join it */
    JOIN,

    /** The entrant is on the waiting list and can leave it. */
    LEAVE,

    /** The entrant is not on the waiting list and it has no room left */
    FULL,

    /** There is nothing to join, or the entrant already has a result in the lottery. */
    NONE
}

/**
 * Orders the open lotteries of a campsite for the details screen
 *
 * @param lotteries the lotteries currently accepting registrations for the campsite
 * @return the same lotteries, the one that closes registration first at the top
 */
fun sortLotteries(lotteries: List<Lottery>): List<Lottery> =
    lotteries.sortedBy { it.registrationClosesAt?.toDate()?.time ?: Long.MAX_VALUE }

/**
 * Decides which button the entrant sees for a lottery.
 *
 * @param lottery the lottery being shown, or null if the campsite has no open lottery
 * @param entry the entrant's own entry in that lottery, or null if they haven't joined
 */
fun joinAction(lottery: Lottery?, entry: LotteryEntry?): JoinAction {
    if (lottery == null) return JoinAction.NONE
    if (entry != null) {
        return if (entry.status == "waiting") JoinAction.LEAVE else JoinAction.NONE
    }
    val capacity = lottery.waitlistCapacity
    if (capacity != null && lottery.entryCount >= capacity) return JoinAction.FULL
    return JoinAction.JOIN
}

/**
 * Formats a price stored in cents, ex 6500 and "CAD" become "$65.00 CAD / night".
 *
 * @param cents the price per night in cents
 * @param currency the currency code stored with the campsite
 */
fun priceText(cents: Long, currency: String): String =
    "$${cents / 100}.${String.format(Locale.US, "%02d", cents % 100)} $currency / night"

/**
 * Text for the registration window of a lottery, ex "Registration: Oct 20 to Oct 22".
 *
 * @param opensAtMillis when registration opens, or null if unknown
 * @param closesAtMillis when registration closes, or null if unknown
 * @param zone the time zone used to pick the days that are shown
 */
fun registrationText(
    opensAtMillis: Long?,
    closesAtMillis: Long?,
    zone: TimeZone = TimeZone.getDefault()
): String {
    if (opensAtMillis == null || closesAtMillis == null) return "Registration dates not set"
    val format = SimpleDateFormat(DAY_PATTERN, Locale.US).apply { timeZone = zone }
    return "Registration: ${format.format(Date(opensAtMillis))} to ${format.format(Date(closesAtMillis))}"
}

/**
 * Text for the days the lottery is offering, ex "Stay: 2026-10-25 to 2026-10-27".
 * Returns an empty string if the lottery has no stay dates.
 */
fun stayText(startDate: String, endDate: String): String =
    if (startDate.isBlank() || endDate.isBlank()) "" else "Stay: $startDate to $endDate"

/** Text for how many entrants are on the waiting list, ex "12 entrants waiting". */
fun entrantsText(count: Long): String =
    if (count == 1L) "1 entrant waiting" else "$count entrants waiting"

/** Text for how many winners the draw picks, ex "2 winners will be drawn". */
fun winnersText(count: Long): String =
    if (count == 1L) "1 winner will be drawn" else "$count winners will be drawn"
