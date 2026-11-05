package com.example.zephyr_bbb

import com.example.zephyr_bbb.entrant.isoDateToMillis
import com.example.zephyr_bbb.entrant.lotteryStatusText
import com.example.zephyr_bbb.entrant.millisToIsoDate
import com.example.zephyr_bbb.entrant.todayUtcMillis
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Unit tests for SearchDates.kt
// Checks the conversion between date picker milliseconds and YYYY-MM-DD strings, the start of
// today used to block past dates, and the lottery closing text on a search result

class SearchDatesTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val edmonton = TimeZone.getTimeZone("America/Edmonton")

    @Test
    fun millisToIsoDate_formatsPickedDay() {
        assertEquals("2026-10-25", millisToIsoDate(1792886400000))
        assertEquals("1970-01-01", millisToIsoDate(0))
    }

    @Test
    fun isoDateToMillis_isMidnightUtc() {
        assertEquals(1792886400000, isoDateToMillis("2026-10-25"))
    }

    @Test
    fun isoDateToMillis_roundTrips() {
        assertEquals("2026-10-27", millisToIsoDate(isoDateToMillis("2026-10-27")!!))
    }

    @Test
    fun isoDateToMillis_rejectsBadDates() {
        assertNull(isoDateToMillis("2026-13-01"))
        assertNull(isoDateToMillis("2026-02-30"))
        assertNull(isoDateToMillis("Oct 25"))
        assertNull(isoDateToMillis(""))
    }

    @Test
    fun todayUtcMillis_usesTheLocalDay() {
        // 23:30 on Oct 25 in Edmonton is already Oct 26 in UTC, but the user's day is Oct 25
        val now = 1792992600000
        assertEquals(1792886400000, todayUtcMillis(now, edmonton))
    }

    @Test
    fun lotteryStatusText_showsClosingDay() {
        // Registration closes Oct 22 at 18:00 UTC
        assertEquals("Lottery open · closes Oct 22", lotteryStatusText(1792692000000, utc))
    }

    @Test
    fun lotteryStatusText_withoutClosingTime() {
        assertEquals("Lottery open", lotteryStatusText(null))
    }
}
