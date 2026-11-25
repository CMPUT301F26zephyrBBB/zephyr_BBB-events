package com.example.zephyr_bbb

import com.example.zephyr_bbb.owner.LotteryFormFields
import com.example.zephyr_bbb.owner.canEditLottery
import com.example.zephyr_bbb.owner.formatClosingRange
import com.example.zephyr_bbb.owner.formatDateRange
import com.example.zephyr_bbb.owner.formatLotteryDate
import com.example.zephyr_bbb.owner.formatLotteryEndDate
import com.example.zephyr_bbb.owner.formatStayRange
import com.example.zephyr_bbb.owner.isLotteryDrawDue
import com.example.zephyr_bbb.owner.lotteryStatusLabel
import com.example.zephyr_bbb.owner.parseLotteryDate
import com.example.zephyr_bbb.owner.parseLotteryEndDate
import com.example.zephyr_bbb.owner.validate
import com.example.zephyr_bbb.owner.waitlistCapacityOrNull
import androidx.compose.runtime.saveable.SaverScope
import com.example.zephyr_bbb.owner.LotteryFormFieldsSaver
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
 * A draw is due only once registration has closed and the lottery is still open.
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
        campsiteIds = setOf("pine_creek_a12", "willow_bend_b04"),
        registrationStart = "2026-10-01",
        registrationEnd = "2026-10-14",
        bookingStart = "2026-10-18",
        bookingEnd = "2026-10-31",
        winnerCount = "8",
        waitlistCapacity = "",
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

    /** Ranges across months or years show both months or years; a missing date shows "Not set". */
    @Test
    fun formatDateRangeAcrossMonthsAndYears() {
        assertEquals(
            "Oct 28 – Nov 3, 2026",
            formatDateRange(date("2026-10-28"), date("2026-11-03"), utc)
        )
        assertEquals(
            "Dec 30, 2026 – Jan 2, 2027",
            formatDateRange(date("2026-12-30"), date("2027-01-02"), utc)
        )
        assertEquals("Not set", formatDateRange(null, date("2026-11-03"), utc))
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

    /** A completely filled-in form, with one or several campsites, has no validation error. */
    @Test
    fun validFormHasNoError() {
        assertNull(validFields.validate(utc))
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
            "Select the registration start date.",
            validFields.copy(registrationStart = "").validate(utc)
        )
        assertEquals(
            "Select the registration end date.",
            validFields.copy(registrationEnd = "Oct 14").validate(utc)
        )
    }

    /** Missing or impossible booking dates are rejected. */
    @Test
    fun formRequiresBookingDates() {
        assertEquals(
            "Select the booking start date.",
            validFields.copy(bookingStart = "").validate(utc)
        )
        assertEquals(
            "Select the booking end date.",
            validFields.copy(bookingEnd = "2026-02-30").validate(utc)
        )
    }

    /** Missing stay start or end dates are rejected. */
    @Test
    fun formRequiresStayDates() {
        assertEquals(
            "Select the stay start date.",
            validFields.copy(stayStart = "").validate(utc)
        )
        assertEquals(
            "Select the stay end date.",
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

    /** Registration can start and end on the same day, since end dates are inclusive. */
    @Test
    fun registrationCanBeOneDay() {
        val fields = validFields.copy(
            registrationStart = "2026-10-14",
            registrationEnd = "2026-10-14"
        )
        assertNull(fields.validate(utc))
    }

    /** Booking that ends before it starts is rejected. */
    @Test
    fun bookingCannotEndBeforeItStarts() {
        val fields = validFields.copy(
            bookingStart = "2026-10-18",
            bookingEnd = "2026-10-10"
        )
        assertEquals(
            "Booking must end on or after the day it starts.",
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
    /** A blank capacity is valid and means an unlimited waitlist; a number is read as that capacity. */
    @Test
    fun blankWaitlistCapacityIsUnlimited() {
        val fields = validFields.copy(waitlistCapacity = "")
        assertNull(fields.validate(utc))
        assertNull(fields.waitlistCapacityOrNull())
        assertEquals(20L, validFields.copy(waitlistCapacity = "20").waitlistCapacityOrNull())
    }

    /** A draw is due once registration has closed, and not before. */
    @Test
    fun drawIsDueOnlyAfterRegistrationCloses() {
        assertFalse(    isLotteryDrawDue("open", registrationCloses, date("2026-10-14")))
        assertTrue(isLotteryDrawDue("open", registrationCloses, registrationCloses))
        assertTrue(isLotteryDrawDue("open", registrationCloses, date("2026-10-20")))
    }
    private val registrationOpens = date("2026-10-01")
    private val registrationCloses = parseLotteryEndDate("2026-10-14", utc)!!

    /** An open lottery is Active while registration is running, including its last day. */
    @Test
    fun openLotteryDuringRegistrationIsActive() {
        assertEquals(
            "Active",
            lotteryStatusLabel("open", registrationOpens, registrationCloses, date("2026-10-14"))
        )
    }

    /** An open lottery is Upcoming before registration, and Drawing winners once it has closed. */
    @Test
    fun openLotteryOutsideRegistrationIsUpcomingOrDrawing() {
        assertEquals(
            "Upcoming",
            lotteryStatusLabel("open", registrationOpens, registrationCloses, date("2026-09-30"))
        )
        assertEquals(
            "Drawing winners",
            lotteryStatusLabel("open", registrationOpens, registrationCloses, registrationCloses)
        )
    }

    /** Drawn and cancelled lotteries are shown as Completed and Cancelled. */
    @Test
    fun drawnLotteryIsCompleted() {
        assertEquals(
            "Completed",
            lotteryStatusLabel("drawn", registrationOpens, registrationCloses, date("2026-10-15"))
        )
        assertEquals(
            "Cancelled",
            lotteryStatusLabel("cancelled", registrationOpens, registrationCloses, date("2026-10-05"))
        )
    }

    /** The saver used to keep the form through screen rotation restores every field exactly. */
    @Test
    fun formFieldsSurviveSaveAndRestore() {
        val fields = validFields.copy(waitlistCapacity = "20")
        val saved = with(LotteryFormFieldsSaver) { SaverScope { true }.save(fields) }!!
        assertEquals(fields, LotteryFormFieldsSaver.restore(saved))
    }

    /** A drawn or cancelled lottery, or one with no closing time, is not due for a draw. */
    @Test
    fun drawIsNotDueOnceDrawnOrCancelled() {
        assertFalse(isLotteryDrawDue("drawn", registrationCloses, date("2026-10-20")))
        assertFalse(isLotteryDrawDue("cancelled", registrationCloses, date("2026-10-20")))
        assertFalse(isLotteryDrawDue("open", null, date("2026-10-20")))
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