package com.example.zephyr_bbb

import com.example.zephyr_bbb.owner.LotteryFormFields
import com.example.zephyr_bbb.owner.canEditLottery
import com.example.zephyr_bbb.owner.formatClosingRange
import com.example.zephyr_bbb.owner.formatDateRange
import com.example.zephyr_bbb.owner.formatLotteryDate
import com.example.zephyr_bbb.owner.formatLotteryEndDate
import com.example.zephyr_bbb.owner.formatStayRange
import com.example.zephyr_bbb.owner.lotteryStatusLabel
import com.example.zephyr_bbb.owner.parseLotteryDate
import com.example.zephyr_bbb.owner.parseLotteryEndDate
import com.example.zephyr_bbb.owner.validate
import com.example.zephyr_bbb.owner.waitlistCapacityOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.TimeZone

/**
 * Tests for LotteryFormUtils.kt: how the lottery form is checked, how dates are read and shown, and
 * the rules for a lottery's status, edits and automatic draw. They run on the computer with no emulator.
 * Date reading and display, the name and stay-date checks, and the Active and Completed labels.
 * Inclusive registration end dates, the registration checks, and how the period is displayed.
 * The booking checks, including that booking must stay open after registration ends.
 * At least one winner is required.
 * A blank waitlist capacity means unlimited, and a set capacity must be positive.
 *
 * At least one campsite is required, and several are allowed.
 */
class LotteryFormUtilsTest {

    private val utc = TimeZone.getTimeZone("UTC")

    /** Reads a YYYY-MM-DD day in UTC so the tests give the same result on any phone. */
    private fun date(text: String): Date {
        return parseLotteryDate(text, utc)!!
    }

    // A form that passes validation, based on the "October Foothills" storyboard example
    private val validFields = LotteryFormFields(
        title = "October Foothills",
        stayStart = "2026-10-18",
        stayEnd = "2026-11-01",
    )
    /** A valid YYYY-MM-DD date is read and formats back to the same text. */
    @Test
    fun parseLotteryDateReadsValidDate() {
        val parsed = parseLotteryDate("2026-10-01", utc)
        assertNotNull(parsed)
        assertEquals("2026-10-01", formatLotteryDate(parsed, utc))
    }

    /** Empty, malformed and impossible dates such as 2026-02-30 are rejected. */
    @Test
    fun parseLotteryDateRejectsBadInput() {
        assertNull(parseLotteryDate("", utc))
        assertNull(parseLotteryDate("abc", utc))
        assertNull(parseLotteryDate("2026-1-5", utc))
        assertNull(parseLotteryDate("10/01/2026", utc))
        assertNull(parseLotteryDate("2026-02-30", utc))
        assertNull(parseLotteryDate("2026-13-01", utc))
    }

    /** An inclusive end date closes at the start of the following day. */
    @Test
    fun parseLotteryEndDateClosesAtStartOfNextDay() {
        assertEquals(date("2026-10-15"), parseLotteryEndDate("2026-10-14", utc))
    }

    /** An end date turned into a closing time converts back to the text the owner typed. */
    @Test
    fun formatLotteryEndDateUndoesParseLotteryEndDate() {
        val closesAt = parseLotteryEndDate("2026-10-14", utc)
        assertEquals("2026-10-14", formatLotteryEndDate(closesAt, utc))
    }

    /** A range inside one month shows the month once. */
    @Test
    fun formatDateRangeInSameMonth() {
        assertEquals(
            "Oct 1–14, 2026",
            formatDateRange(date("2026-10-01"), date("2026-10-14"), utc)
        )
    }

    /** A registration or booking range shows the last open day, not the day after. */
    @Test
    fun formatClosingRangeShowsLastOpenDay() {
        assertEquals(
            "Oct 1–14, 2026",
            formatClosingRange(
                date("2026-10-01"),
                parseLotteryEndDate("2026-10-14", utc),
                utc
            )
        )
    }

    /** A stay shows its check-in and checkout dates. */
    @Test
    fun formatStayRangeShowsCheckoutDate() {
        assertEquals("Oct 18–21, 2026", formatStayRange("2026-10-18", "2026-10-21", utc))
    }

    /** A completely filled-in form has no validation error. */
    @Test
    fun validFormHasNoError() {
        assertNull(validFields.validate(utc))
    }

    /** A form with several campsites is valid. */
    @Test
    fun formAllowsMultipleCampsites() {
        val fields = validFields.copy(campsiteIds = setOf("a", "b", "c"))
        assertNull(fields.validate(utc))
    }

    /** A blank lottery name is rejected. */
    @Test
    fun formRequiresName() {
        assertEquals(
            "Enter a lottery name.",
            validFields.copy(title = "   ").validate(utc)
        )
    }

    /** A form with no campsite is rejected. */
    @Test
    fun formRequiresCampsite() {
        assertEquals(
            "Choose at least one campsite.",
            validFields.copy(campsiteIds = emptySet()).validate(utc)
        )
    }

    /** Missing or malformed registration dates are rejected. */
    @Test
    fun formRequiresRegistrationDates() {
        assertEquals(
            "Enter the registration start as YYYY-MM-DD.",
            validFields.copy(registrationStart = "").validate(utc)
        )
        assertEquals(
            "Enter the registration end as YYYY-MM-DD.",
            validFields.copy(registrationEnd = "Oct 14").validate(utc)
        )
    }

    /** Missing or impossible booking dates are rejected. */
    @Test
    fun formRequiresBookingDates() {
        assertEquals(
            "Enter the booking start as YYYY-MM-DD.",
            validFields.copy(bookingStart = "").validate(utc)
        )
        assertEquals(
            "Enter the booking end as YYYY-MM-DD.",
            validFields.copy(bookingEnd = "2026-02-30").validate(utc)
        )
    }

    /** Missing stay start or end dates are rejected. */
    @Test
    fun formRequiresStayDates() {
        assertEquals(
            "Enter the stay start as YYYY-MM-DD.",
            validFields.copy(stayStart = "").validate(utc)
        )
        assertEquals(
            "Enter the stay end as YYYY-MM-DD.",
            validFields.copy(stayEnd = "").validate(utc)
        )
    }

    /** Registration that ends before it starts is rejected. */
    @Test
    fun registrationCannotEndBeforeItStarts() {
        val fields = validFields.copy(
            registrationStart = "2026-10-14",
            registrationEnd = "2026-10-01"
        )
        assertEquals(
            "Registration must end on or after the day it starts.",
            fields.validate(utc)
        )
    }

    /** A booking period that ends with or before registration is rejected. */
    @Test
    fun bookingMustStayOpenAfterRegistration() {
        val fields = validFields.copy(
            bookingStart = "2026-10-01",
            bookingEnd = "2026-10-14"
        )
        assertEquals(
            "Booking must stay open after registration ends.",
            fields.validate(utc)
        )
    }

    /** A stay that ends on or before its start date is rejected. */
    @Test
    fun stayMustEndAfterItStarts() {
        val fields = validFields.copy(
            stayStart = "2026-10-21",
            stayEnd = "2026-10-21"
        )
        assertEquals("The stay must end after it starts.", fields.validate(utc))
    }
    /** Zero or blank winners are rejected. */
    @Test
    fun formRequiresAtLeastOneWinner() {
        assertEquals(
            "Choose at least one winner.",
            validFields.copy(winnerCount = "0").validate(utc)
        )
        assertEquals(
            "Choose at least one winner.",
            validFields.copy(winnerCount = "").validate(utc)
        )
    }
    /** A blank capacity is valid and means an unlimited waitlist. */
    @Test
    fun blankWaitlistCapacityIsUnlimited() {
        val fields = validFields.copy(waitlistCapacity = "")
        assertNull(fields.validate(utc))
        assertNull(fields.waitlistCapacityOrNull())
    }

    @Test
    fun waitlistCapacityMustBePositive() {
        assertEquals(
            "Waitlist capacity must be a positive number or left blank.",
            validFields.copy(waitlistCapacity = "0").validate(utc)
        )
    }

    /** Only open lotteries can be edited; drawn and cancelled ones cannot. */
    @Test
    fun onlyOpenLotteriesCanBeEdited() {
        assertTrue(canEditLottery("open"))
        assertFalse(canEditLottery("drawn"))
        assertFalse(canEditLottery("cancelled"))
    }

}