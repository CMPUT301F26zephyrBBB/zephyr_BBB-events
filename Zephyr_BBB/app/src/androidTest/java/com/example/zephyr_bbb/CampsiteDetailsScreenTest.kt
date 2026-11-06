package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.entrant.CampsiteDetailsContent
import com.example.zephyr_bbb.entrant.JoinAction
import com.google.firebase.Timestamp
import java.util.Date
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Tests for CampsiteDetailsScreen.kt
// Checks the campsite information and the lottery card show up, the join and leave dialogs ask
// for confirmation, and the loading, error and not found states.
// Uses CampsiteDetailsContent with fake data, so these tests dont use Firestore

@RunWith(AndroidJUnit4::class)
class CampsiteDetailsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val pineCreek = Campsite(
        id = "pine-creek-a12",
        name = "Pine Creek A12",
        description = "Quiet site beside the creek.",
        locationName = "Jasper, Alberta",
        maxOccupancy = 6,
        pricePerNightCents = 6500,
        tags = listOf("Fire pit", "Potable water"),
        rules = "Quiet hours after 10 pm.",
        ratingSum = 175,
        ratingCount = 38
    )

    private val fallDraw = Lottery(
        id = "fall-draw",
        title = "Fall long weekend draw",
        registrationOpensAt = Timestamp(Date(1792519200000L)),
        registrationClosesAt = Timestamp(Date(1792692000000L)),
        stayStartDate = "2026-10-25",
        stayEndDate = "2026-10-27",
        winnerCount = 2,
        entryCount = 12,
        status = "open"
    )

    private fun show(
        campsite: Campsite? = pineCreek,
        lottery: Lottery? = fallDraw,
        joinAction: JoinAction = JoinAction.JOIN,
        isLoading: Boolean = false,
        isWorking: Boolean = false,
        notFound: Boolean = false,
        loadError: String? = null,
        actionError: String? = null,
        onBackClick: () -> Unit = {},
        onRetryClick: () -> Unit = {},
        onJoinConfirmed: () -> Unit = {},
        onLeaveConfirmed: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            CampsiteDetailsContent(
                campsite = campsite,
                lottery = lottery,
                joinAction = joinAction,
                isLoading = isLoading,
                isWorking = isWorking,
                notFound = notFound,
                loadError = loadError,
                actionError = actionError,
                onBackClick = onBackClick,
                onRetryClick = onRetryClick,
                onJoinConfirmed = onJoinConfirmed,
                onLeaveConfirmed = onLeaveConfirmed
            )
        }
    }

    @Test
    fun showsCampsiteInformation() {
        show()
        composeTestRule.onNodeWithText("Pine Creek A12").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jasper, Alberta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Max 6 guests · ★ 4.6 (38 reviews)").assertIsDisplayed()
        composeTestRule.onNodeWithText("$65.00 CAD / night").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quiet site beside the creek.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fire pit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Potable water").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quiet hours after 10 pm.").assertIsDisplayed()
    }

    @Test
    fun showsLotteryCard() {
        show()
        composeTestRule.onNodeWithText("Fall long weekend draw").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Stay: 2026-10-25 to 2026-10-27").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 winners will be drawn").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 entrants waiting").assertIsDisplayed()
        composeTestRule.onNodeWithText("Join Lottery").assertIsDisplayed()
    }

    @Test
    fun noLotteryShowsMessageAndNoJoinButton() {
        show(lottery = null, joinAction = JoinAction.NONE)
        composeTestRule.onNodeWithText("No active lottery for this campsite right now.")
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Join Lottery").assertDoesNotExist()
    }

    @Test
    fun backButtonWorks() {
        var backClicked = false
        show(onBackClick = { backClicked = true })
        composeTestRule.onNodeWithText("Back").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun joinAsksForConfirmationThenJoins() {
        var joins = 0
        show(onJoinConfirmed = { joins++ })
        composeTestRule.onNodeWithText("Join Lottery").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Join lottery?").assertIsDisplayed()
        assertEquals(0, joins)
        composeTestRule.onNodeWithText("Join").performClick()
        assertEquals(1, joins)
    }

    @Test
    fun cancellingTheJoinDialogDoesNotJoin() {
        var joins = 0
        show(onJoinConfirmed = { joins++ })
        composeTestRule.onNodeWithText("Join Lottery").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Join lottery?").assertDoesNotExist()
        assertEquals(0, joins)
    }

    @Test
    fun joinedEntrantSeesLeaveButtonAndCanLeave() {
        var leaves = 0
        show(joinAction = JoinAction.LEAVE, onLeaveConfirmed = { leaves++ })
        composeTestRule.onNodeWithText("You are on the waiting list.").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Join Lottery").assertDoesNotExist()
        composeTestRule.onNodeWithText("Leave Waiting List").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Leave waiting list?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leave").performClick()
        assertEquals(1, leaves)
    }

    @Test
    fun fullWaitlistDisablesJoin() {
        show(joinAction = JoinAction.FULL)
        composeTestRule.onNodeWithText("Waiting list full").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun joinButtonDisabledWhileWorking() {
        show(isWorking = true)
        composeTestRule.onNodeWithText("Join Lottery").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun actionErrorIsShown() {
        show(actionError = "The waitlist is full.")
        composeTestRule.onNodeWithText("The waitlist is full.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadingShowsMessage() {
        show(campsite = null, lottery = null, isLoading = true)
        composeTestRule.onNodeWithText("Loading campsite...").assertIsDisplayed()
    }

    @Test
    fun notFoundShowsMessageAndBackStillWorks() {
        var backClicked = false
        show(campsite = null, lottery = null, notFound = true, onBackClick = { backClicked = true })
        composeTestRule.onNodeWithText("This campsite could not be found.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun loadErrorShowsMessageAndRetry() {
        var retries = 0
        show(campsite = null, lottery = null, loadError = "Network down", onRetryClick = { retries++ })
        composeTestRule.onNodeWithText("Network down").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }
}
