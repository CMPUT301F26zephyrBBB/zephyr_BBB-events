package com.example.zephyr_bbb

import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.entrant.CampsiteSearchFilters
import com.example.zephyr_bbb.entrant.averageRating
import com.example.zephyr_bbb.entrant.matchesAmenities
import com.example.zephyr_bbb.entrant.ratingSummary
import com.example.zephyr_bbb.entrant.resultCountText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit tests for CampsiteSearchFilters.kt
// Checks filter validation, the filter summary text, the amenity match, and the rating and
// result count text shown on the search screen

class CampsiteSearchFiltersTest {

    private val pineCreek = Campsite(
        name = "Pine Creek A12",
        locationName = "Jasper, Alberta",
        maxOccupancy = 6,
        tags = listOf("Fire pit", "Potable water", "Nearby toilets", "Family-friendly"),
        ratingSum = 175,
        ratingCount = 38
    )

    @Test
    fun defaultFilters_areValid() {
        assertNull(CampsiteSearchFilters().validate())
    }

    @Test
    fun zeroGuests_isInvalid() {
        assertNotNull(CampsiteSearchFilters(guests = 0).validate())
    }

    @Test
    fun onlyCheckIn_isInvalid() {
        val filters = CampsiteSearchFilters(checkInDate = "2026-10-25")
        assertEquals("Enter both dates or neither", filters.validate())
    }

    @Test
    fun checkOutBeforeCheckIn_isInvalid() {
        val filters = CampsiteSearchFilters(
            checkInDate = "2026-10-27",
            checkOutDate = "2026-10-25"
        )
        assertEquals("Check-out must be after check-in", filters.validate())
    }

    @Test
    fun sameDayCheckInAndCheckOut_isInvalid() {
        val filters = CampsiteSearchFilters(
            checkInDate = "2026-10-25",
            checkOutDate = "2026-10-25"
        )
        assertEquals("Check-out must be after check-in", filters.validate())
    }

    @Test
    fun notARealDate_isInvalid() {
        val filters = CampsiteSearchFilters(
            checkInDate = "2026-02-30",
            checkOutDate = "2026-03-02"
        )
        assertEquals("Dates must use YYYY-MM-DD", filters.validate())
    }

    @Test
    fun validDateRange_isValid() {
        val filters = CampsiteSearchFilters(
            location = "Jasper, AB",
            guests = 4,
            checkInDate = "2026-10-25",
            checkOutDate = "2026-10-27"
        )
        assertNull(filters.validate())
    }

    @Test
    fun summary_listsActiveFilters() {
        val filters = CampsiteSearchFilters(
            location = "Jasper, AB",
            guests = 4,
            checkInDate = "2026-10-25",
            checkOutDate = "2026-10-27"
        )
        assertEquals("Jasper, AB · 2026-10-25 to 2026-10-27 · 4 guests", filters.summary())
    }

    @Test
    fun summary_isEmptyWithoutFilters() {
        assertEquals("", CampsiteSearchFilters().summary())
    }

    @Test
    fun activeFilterCount_ignoresSearchText() {
        assertEquals(0, CampsiteSearchFilters(searchText = "lake").activeFilterCount())
    }

    @Test
    fun activeFilterCount_countsEachFilter() {
        val filters = CampsiteSearchFilters(
            location = "Canmore",
            amenities = "fire pit",
            guests = 2,
            checkInDate = "2026-10-25",
            checkOutDate = "2026-10-27"
        )
        assertEquals(4, filters.activeFilterCount())
    }

    @Test
    fun averageRating_isNullWithoutRatings() {
        assertNull(Campsite(name = "Pine Creek A12").averageRating())
    }

    @Test
    fun averageRating_dividesSumByCount() {
        // Three ratings of 4, 5 and 3
        val campsite = Campsite(name = "Pine Creek A12", ratingSum = 12, ratingCount = 3)
        assertEquals(4.0, campsite.averageRating()!!, 0.001)
    }

    @Test
    fun ratingSummary_showsAverageAndReviewCount() {
        assertEquals("★ 4.6 (38 reviews)", pineCreek.ratingSummary())
    }

    @Test
    fun ratingSummary_usesSingularForOneReview() {
        val campsite = Campsite(ratingSum = 5, ratingCount = 1)
        assertEquals("★ 5.0 (1 review)", campsite.ratingSummary())
    }

    @Test
    fun ratingSummary_withoutRatings() {
        assertEquals("No ratings yet", Campsite().ratingSummary())
    }

    @Test
    fun matchesAmenities_emptyRequestMatchesEverything() {
        assertTrue(pineCreek.matchesAmenities(""))
        assertTrue(pineCreek.matchesAmenities(" , "))
    }

    @Test
    fun matchesAmenities_ignoresCaseAndMatchesPartOfATag() {
        assertTrue(pineCreek.matchesAmenities("FIRE"))
        assertTrue(pineCreek.matchesAmenities("water"))
    }

    @Test
    fun matchesAmenities_needsEveryAmenity() {
        assertTrue(pineCreek.matchesAmenities("fire pit, potable water"))
        assertFalse(pineCreek.matchesAmenities("fire pit, hot tub"))
    }

    @Test
    fun resultCountText_handlesOneAndMany() {
        assertEquals("1 campsite found", resultCountText(1))
        assertEquals("3 campsites found", resultCountText(3))
        assertEquals("0 campsites found", resultCountText(0))
    }
}
