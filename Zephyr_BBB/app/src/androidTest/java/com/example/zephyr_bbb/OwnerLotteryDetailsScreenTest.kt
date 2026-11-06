package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.owner.LotteryDetailsContent
import com.example.zephyr_bbb.owner.OwnerLotteryDetailsScreen
import com.example.zephyr_bbb.owner.parseLotteryDate
import com.example.zephyr_bbb.owner.parseLotteryEndDate
import com.google.firebase.Timestamp
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for OwnerLotteryDetailsScreen.kt. They run on the emulator with a fake lottery, so they do not
 * use Firebase.
 * The details show the periods and settings, and Edit Lottery only appears while the lottery is open.
 */
@RunWith(AndroidJUnit4::class)
class OwnerLotteryDetailsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Turns a YYYY-MM-DD day into the Timestamp for the start of that day.
     */
    private fun start(text: String) = Timestamp(parseLotteryDate(text)!!)

    /**
     * Turns an inclusive YYYY-MM-DD end day into the Timestamp it closes at.
     */
    private fun end(text: String) = Timestamp(parseLotteryEndDate(text)!!)

    private val campsiteNames = mapOf(
        "c1" to "Pine Creek A12",
        "c2" to "Willow Bend B04"
    )

    private val openLottery = Lottery(
        id = "l1",
        title = "October Foothills",
        campsiteIds = listOf("c1", "c2"),
        registrationOpensAt = start("2026-10-01"),
        registrationClosesAt = end("2026-10-14"),
        bookingOpensAt = start("2026-10-18"),
        bookingClosesAt = end("2026-10-31"),
        stayStartDate = "2026-10-18",
        stayEndDate = "2026-11-01",
        winnerCount = 8,
        waitlistCapacity = null,
        entryCount = 20,
        status = "open"
    )

    private val drawnLottery = openLottery.copy(
        status = "drawn",
        drawnAt = start("2026-10-15")
    )

    private var backClicked = false
    private var editClicked = false

    /**
     * Shows the details of a lottery at a chosen "now" and records the button clicks.
     */
    private fun showDetails(lottery: Lottery?, nowText: String = "2026-10-05") {
        composeTestRule.setContent {
            LotteryDetailsContent(
                lottery = lottery,
                campsiteNames = campsiteNames,
                now = parseLotteryDate(nowText)!!,
                isLoading = false,
                errorMessage = null,
                onBackClick = { backClicked = true },
                onEditLotteryClick = { editClicked = true }
            )
        }
    }

    /** The details show the campsites, registration, booking and stay periods, winners and waitlist size. */
    @Test
    fun lotteryDetailsShowsPeriodsAndSettings() {
        showDetails(openLottery)

        composeTestRule.onNodeWithText("Lottery Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("October Foothills").assertIsDisplayed()
        composeTestRule.onNodeWithText("Status: Active").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pine Creek A12 · Willow Bend B04").performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Registration period").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Oct 1–14, 2026").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Booking period").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Oct 18–31, 2026").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Stay dates").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Oct 18 – Nov 1, 2026").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Winner count: 8").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Waitlist size: 20").performScrollTo().assertIsDisplayed()
    }

    /** Edit Lottery reports a click while the lottery is open. */
    @Test
    fun lotteryDetailsEditWorksWhileOpen() {
        showDetails(openLottery)
        composeTestRule.onNodeWithText("Edit Lottery").performScrollTo().performClick()
        assertTrue(editClicked)
    }

    /** A cancelled lottery shows its status and has no Edit Lottery button. */
    @Test
    fun cancelledLotteryHidesEdit() {
        showDetails(openLottery.copy(status = "cancelled"))
        composeTestRule.onNodeWithText("Status: Cancelled").assertIsDisplayed()
        composeTestRule.onNodeWithText("Edit Lottery").assertDoesNotExist()
    }
}