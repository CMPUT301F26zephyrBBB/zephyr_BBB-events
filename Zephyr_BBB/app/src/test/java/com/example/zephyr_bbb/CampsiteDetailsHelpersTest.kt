package com.example.zephyr_bbb

import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.database.model.LotteryEntry
import com.example.zephyr_bbb.entrant.JoinAction
import com.example.zephyr_bbb.entrant.chooseLottery
import com.example.zephyr_bbb.entrant.entrantsText
import com.example.zephyr_bbb.entrant.joinAction
import com.example.zephyr_bbb.entrant.priceText
import com.example.zephyr_bbb.entrant.registrationText
import com.example.zephyr_bbb.entrant.stayText
import com.example.zephyr_bbb.entrant.winnersText
import com.google.firebase.Timestamp
import java.util.Date
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Unit tests for CampsiteDetailsHelpers.kt
// Checks which lottery is shown, which button the entrant gets, and the price and lottery text
// on the campsite details screen

class CampsiteDetailsHelpersTest {

    private val utc = TimeZone.getTimeZone("UTC")

    // Oct 22 2026 18:00 UTC and Oct 20 2026 18:00 UTC
    private val oct22 = 1792692000000L
    private val oct20 = oct22 - 2 * 24 * 60 * 60 * 1000L

    private fun lottery(
        id: String,
        closesAt: Long? = oct22,
        entryCount: Long = 0,
        capacity: Long? = null
    ) = Lottery(
        id = id,
        title = "Fall long weekend draw",
        registrationOpensAt = Timestamp(Date(oct20)),
        registrationClosesAt = closesAt?.let { Timestamp(Date(it)) },
        entryCount = entryCount,
        waitlistCapacity = capacity,
        status = "open"
    )

    private val waitingEntry = LotteryEntry(entrantId = "u1", lotteryId = "l1", status = "waiting")

    @Test
    fun chooseLottery_emptyList_isNull() {
        assertNull(chooseLottery(emptyList()))
    }

    @Test
    fun chooseLottery_picksTheOneClosingFirst() {
        val later = lottery("later", closesAt = oct22 + 86_400_000L)
        val sooner = lottery("sooner", closesAt = oct22)
        assertEquals("sooner", chooseLottery(listOf(later, sooner))?.id)
    }

    @Test
    fun joinAction_noLottery_isNone() {
        assertEquals(JoinAction.NONE, joinAction(null, null))
    }

    @Test
    fun joinAction_notJoined_isJoin() {
        assertEquals(JoinAction.JOIN, joinAction(lottery("l1"), null))
    }

    @Test
    fun joinAction_waiting_isLeave() {
        assertEquals(JoinAction.LEAVE, joinAction(lottery("l1"), waitingEntry))
    }

    @Test
    fun joinAction_alreadyHasResult_isNone() {
        val selected = waitingEntry.copy(status = "selected")
        assertEquals(JoinAction.NONE, joinAction(lottery("l1"), selected))
    }

    @Test
    fun joinAction_fullWaitlist_isFull() {
        assertEquals(JoinAction.FULL, joinAction(lottery("l1", entryCount = 10, capacity = 10), null))
    }

    @Test
    fun joinAction_roomLeft_isJoin() {
        assertEquals(JoinAction.JOIN, joinAction(lottery("l1", entryCount = 9, capacity = 10), null))
    }

    @Test
    fun joinAction_fullWaitlistButAlreadyJoined_isLeave() {
        val full = lottery("l1", entryCount = 10, capacity = 10)
        assertEquals(JoinAction.LEAVE, joinAction(full, waitingEntry))
    }

    @Test
    fun priceText_showsDollarsAndCents() {
        assertEquals("$65.00 CAD / night", priceText(6500, "CAD"))
        assertEquals("$65.05 CAD / night", priceText(6505, "CAD"))
        assertEquals("$0.99 CAD / night", priceText(99, "CAD"))
    }

    @Test
    fun registrationText_showsBothDays() {
        assertEquals("Registration: Oct 20 to Oct 22", registrationText(oct20, oct22, utc))
    }

    @Test
    fun registrationText_missingDates() {
        assertEquals("Registration dates not set", registrationText(null, oct22, utc))
    }

    @Test
    fun stayText_showsRangeOrNothing() {
        assertEquals("Stay: 2026-10-25 to 2026-10-27", stayText("2026-10-25", "2026-10-27"))
        assertEquals("", stayText("", "2026-10-27"))
    }

    @Test
    fun entrantsAndWinnersText_handleOne() {
        assertEquals("1 entrant waiting", entrantsText(1))
        assertEquals("12 entrants waiting", entrantsText(12))
        assertEquals("0 entrants waiting", entrantsText(0))
        assertEquals("1 winner will be drawn", winnersText(1))
        assertEquals("3 winners will be drawn", winnersText(3))
    }
}
