package com.example.zephyr_bbb

import com.example.zephyr_bbb.owner.LotteryFormFields
import com.example.zephyr_bbb.owner.formatDateRange
import com.example.zephyr_bbb.owner.formatLotteryDate
import com.example.zephyr_bbb.owner.formatStayRange
import com.example.zephyr_bbb.owner.lotteryStatusLabel
import com.example.zephyr_bbb.owner.parseLotteryDate
import com.example.zephyr_bbb.owner.validate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.TimeZone

class LotteryFormUtilsTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun date(text: String): Date {
        return parseLotteryDate(text, utc)!!
    }

    private val validFields = LotteryFormFields(
        title = "October Foothills",
        stayStart = "2026-10-18",
        stayEnd = "2026-11-01",
    )

    @Test
    fun parseLotteryDateReadsValidDate() {
        val parsed = parseLotteryDate("2026-10-01", utc)
        assertNotNull(parsed)
        assertEquals("2026-10-01", formatLotteryDate(parsed, utc))
    }

    @Test
    fun parseLotteryDateRejectsBadInput() {
        assertNull(parseLotteryDate("", utc))
        assertNull(parseLotteryDate("abc", utc))
        assertNull(parseLotteryDate("2026-1-5", utc))
        assertNull(parseLotteryDate("10/01/2026", utc))
        assertNull(parseLotteryDate("2026-02-30", utc))
        assertNull(parseLotteryDate("2026-13-01", utc))
    }

    @Test
    fun formatDateRangeInSameMonth() {
        assertEquals(
            "Oct 1–14, 2026",
            formatDateRange(date("2026-10-01"), date("2026-10-14"), utc)
        )
    }

    @Test
    fun formatStayRangeShowsCheckoutDate() {
        assertEquals("Oct 18–21, 2026", formatStayRange("2026-10-18", "2026-10-21", utc))
    }

    @Test
    fun validFormHasNoError() {
        assertNull(validFields.validate(utc))
    }

    @Test
    fun formRequiresName() {
        assertEquals(
            "Enter a lottery name.",
            validFields.copy(title = "   ").validate(utc)
        )
    }

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

    @Test
    fun stayMustEndAfterItStarts() {
        val fields = validFields.copy(
            stayStart = "2026-10-21",
            stayEnd = "2026-10-21"
        )
        assertEquals("The stay must end after it starts.", fields.validate(utc))
    }
}