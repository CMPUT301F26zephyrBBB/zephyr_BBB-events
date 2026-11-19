package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.entrant.CampsiteFilterScreen
import com.example.zephyr_bbb.entrant.CampsiteSearchScreen
import com.example.zephyr_bbb.entrant.CampsiteSearchViewModel
import com.example.zephyr_bbb.entrant.CampsiteSummaryCard
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Tests for CampsiteSearchScreen.kt, CampsiteFilterScreen.kt and CampsiteSummaryCard.kt
// Checks the search bar, filter fields and summary card show up, and that the buttons work.
// The search and filter screens are created with a real view model, so they use Firestore.

@RunWith(AndroidJUnit4::class)
class CampsiteSearchScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val pineCreek = Campsite(
        id = "pine-creek-a12",
        name = "Pine Creek A12",
        locationName = "Jasper, Alberta",
        maxOccupancy = 6,
        ratingSum = 175,
        ratingCount = 38
    )

    @Test
    fun searchScreenShowsSearchBarAndButtons() {
        composeTestRule.setContent {
            CampsiteSearchScreen(
                viewModel = CampsiteSearchViewModel(),
                onFilterClick = {},
                onCampsiteClick = {}
            )
        }
        composeTestRule.onNodeWithText("Search Campsites").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search campsites").assertIsDisplayed()
        composeTestRule.onNodeWithText("Filters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search").assertIsDisplayed()
    }

    @Test
    fun searchScreenFilterButtonWorks() {
        var filterClicked = false
        composeTestRule.setContent {
            CampsiteSearchScreen(
                viewModel = CampsiteSearchViewModel(),
                onFilterClick = { filterClicked = true },
                onCampsiteClick = {}
            )
        }
        composeTestRule.onNodeWithText("Filters").performClick()
        assertTrue(filterClicked)
    }

    @Test
    fun summaryCardShowsCampsiteInformation() {
        composeTestRule.setContent {
            CampsiteSummaryCard(
                campsite = pineCreek,
                lotteryText = "Lottery open · closes Oct 22",
                onViewDetailsClick = {}
            )
        }
        composeTestRule.onNodeWithText("Pine Creek A12").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jasper, Alberta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Max 6 guests · ★ 4.6 (38 reviews)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lottery open · closes Oct 22").assertIsDisplayed()
    }

    @Test
    fun summaryCardViewDetailsWorks() {
        var detailsClicked = false
        composeTestRule.setContent {
            CampsiteSummaryCard(
                campsite = pineCreek,
                lotteryText = "No active lottery",
                onViewDetailsClick = { detailsClicked = true }
            )
        }
        composeTestRule.onNodeWithText("View Details").performClick()
        assertTrue(detailsClicked)
    }

    @Test
    fun summaryCardWithoutRatingsSaysSo() {
        composeTestRule.setContent {
            CampsiteSummaryCard(
                campsite = Campsite(name = "Willow Bend B04", maxOccupancy = 4),
                lotteryText = "No active lottery",
                onViewDetailsClick = {}
            )
        }
        composeTestRule.onNodeWithText("Max 4 guests · No ratings yet").assertIsDisplayed()
    }

    @Test
    fun filterScreenShowsAllFilters() {
        composeTestRule.setContent {
            CampsiteFilterScreen(
                viewModel = CampsiteSearchViewModel(),
                onBackClick = {},
                onApplyClick = {}
            )
        }
        composeTestRule.onNodeWithText("Location").assertIsDisplayed()
        composeTestRule.onNodeWithText("Amenities (comma separated)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Select dates").assertIsDisplayed()
        composeTestRule.onNodeWithText("Number of guests").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apply Filters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear Filters").assertIsDisplayed()
    }

    @Test
    fun filterScreenBackWorks() {
        var backClicked = false
        composeTestRule.setContent {
            CampsiteFilterScreen(
                viewModel = CampsiteSearchViewModel(),
                onBackClick = { backClicked = true },
                onApplyClick = {}
            )
        }
        composeTestRule.onNodeWithText("Back").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun filterScreenGuestButtonsChangeTheCount() {
        composeTestRule.setContent {
            CampsiteFilterScreen(
                viewModel = CampsiteSearchViewModel(),
                onBackClick = {},
                onApplyClick = {}
            )
        }
        // Starts at one guest, which cannot go lower
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Fewer guests").assertIsNotEnabled()

        composeTestRule.onNodeWithContentDescription("More guests").performClick()
        composeTestRule.onNodeWithContentDescription("More guests").performClick()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Fewer guests").assertIsEnabled()
    }

    @Test
    fun filterScreenClearFiltersResetsGuests() {
        composeTestRule.setContent {
            CampsiteFilterScreen(
                viewModel = CampsiteSearchViewModel(),
                onBackClick = {},
                onApplyClick = {}
            )
        }
        composeTestRule.onNodeWithContentDescription("More guests").performClick()
        composeTestRule.onNodeWithText("2").assertIsDisplayed()

        composeTestRule.onNodeWithText("Clear Filters").performClick()
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun filterScreenApplyFiltersWorks() {
        var applied = false
        val viewModel = CampsiteSearchViewModel()
        composeTestRule.setContent {
            CampsiteFilterScreen(
                viewModel = viewModel,
                onBackClick = {},
                onApplyClick = { applied = true }
            )
        }
        composeTestRule.onNodeWithContentDescription("More guests").performClick()
        composeTestRule.onNodeWithText("Apply Filters").performClick()

        assertTrue(applied)
        assertEquals(2L, viewModel.filters.guests)
    }

    @Test
    fun filterScreenOpensDateRangePicker() {
        var applied = false
        composeTestRule.setContent {
            CampsiteFilterScreen(
                viewModel = CampsiteSearchViewModel(),
                onBackClick = {},
                onApplyClick = { applied = true }
            )
        }
        composeTestRule.onNodeWithText("Select dates").performClick()
        // OK stays disabled until both a check-in and a check-out day are picked
        composeTestRule.onNodeWithText("OK").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Select dates").assertIsDisplayed()
        assertFalse(applied)
    }
}
