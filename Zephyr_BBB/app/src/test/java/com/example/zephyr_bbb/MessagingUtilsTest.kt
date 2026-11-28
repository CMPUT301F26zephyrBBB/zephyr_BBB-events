package com.example.zephyr_bbb

import com.example.zephyr_bbb.database.model.OwnerMessage
import com.example.zephyr_bbb.entrant.MessagingUtils
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

// unit tests for the entrant messaging helpers (US 01.14.01)
class MessagingUtilsTest {

    private val utc = TimeZone.getTimeZone("UTC")

    // 2026-10-17 09:00:00 UTC
    private val nineAm = Timestamp(1_792_227_600L, 0)

    private fun message(id: String, sender: String, seconds: Long?) = OwnerMessage(
        id = id,
        senderId = sender,
        body = "body $id",
        createdAt = seconds?.let { Timestamp(it, 0) }
    )

    // send button

    @Test
    fun cannotSendEmptyMessage() {
        assertFalse(MessagingUtils.canSend("", isSending = false))
    }

    @Test
    fun cannotSendBlankMessage() {
        assertFalse(MessagingUtils.canSend("   ", isSending = false))
    }

    @Test
    fun cannotSendWhileSending() {
        assertFalse(MessagingUtils.canSend("hi", isSending = true))
    }

    @Test
    fun canSendText() {
        assertTrue(MessagingUtils.canSend("hi", isSending = false))
    }

    // ordering

    @Test
    fun sortsOldestFirst() {
        val sorted = MessagingUtils.sortChronologically(
            listOf(message("b", "x", 200), message("a", "x", 100), message("c", "x", 300))
        )
        assertEquals(listOf("a", "b", "c"), sorted.map { it.id })
    }

    // a message still being saved has no time yet, and was just sent, so it goes last
    @Test
    fun messageWithoutTimeGoesLast() {
        val sorted = MessagingUtils.sortChronologically(
            listOf(message("pending", "x", null), message("a", "x", 100))
        )
        assertEquals(listOf("a", "pending"), sorted.map { it.id })
    }

    // who sent it

    @Test
    fun isFromMeWhenISentIt() {
        assertTrue(MessagingUtils.isFromMe(message("a", "me", 1), "me"))
    }

    @Test
    fun isNotFromMeWhenOwnerSentIt() {
        assertFalse(MessagingUtils.isFromMe(message("a", "owner", 1), "me"))
    }

    // time under each bubble

    @Test
    fun formatTimeBlankForMissingTime() {
        assertEquals("", MessagingUtils.formatTime(null))
    }

    @Test
    fun formatTimeShowsMonthDayAndTime() {
        assertEquals("Oct 17, 9:00 AM", MessagingUtils.formatTime(nineAm, Locale.US, utc))
    }

    // header

    @Test
    fun headerShowsCampsiteAndDates() {
        assertEquals(
            "Pine Creek A12 · 2026-10-25 to 2026-10-27",
            MessagingUtils.header("Pine Creek A12", "2026-10-25", "2026-10-27")
        )
    }

    @Test
    fun headerFallsBackWhileCampsiteLoads() {
        assertEquals(
            "Campsite · 2026-10-25 to 2026-10-27",
            MessagingUtils.header(null, "2026-10-25", "2026-10-27")
        )
    }

    // bubbles

    @Test
    fun toBubblesSortsAndMarksMyMessages() {
        val bubbles = MessagingUtils.toBubbles(
            listOf(message("reply", "owner", 200), message("question", "me", 100)),
            myUid = "me",
            locale = Locale.US,
            zone = utc
        )
        assertEquals(listOf("question", "reply"), bubbles.map { it.id })
        assertTrue(bubbles[0].fromMe)
        assertFalse(bubbles[1].fromMe)
        assertEquals("body question", bubbles[0].body)
    }
}
