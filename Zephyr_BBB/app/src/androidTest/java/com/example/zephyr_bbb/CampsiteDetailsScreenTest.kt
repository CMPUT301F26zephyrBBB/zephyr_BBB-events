package com.example.zephyr_bbb

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
// Checks the campsite information and the lottery cards show up (one per open lottery), the join and leave dialogs ask
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

    private val winterDraw = Lottery(
        id = "winter-draw",
        title = "Winter break draw",
        registrationOpensAt = Timestamp(Date(1792519200000L)),
        registrationClosesAt = Timestamp(Date(1793124000000L)),
        stayStartDate = "2026-12-20",
        stayEndDate = "2026-12-23",
        winnerCount = 1,
        entryCount = 1,
        status = "open"
    )

    private fun show(
        campsite: Campsite? = pineCreek,
        lotteries: List<Lottery> = listOf(fallDraw),
        joinAction: JoinAction = JoinAction.JOIN,
        joinActionFor: (Lottery) -> JoinAction = { joinAction },
        isLoading: Boolean = false,
        isWorking: Boolean = false,
        notFound: Boolean = false,
        loadError: String? = null,
        actionError: String? = null,
        onBackClick: () -> Unit = {},
        onRetryClick: () -> Unit = {},
        onJoinConfirmed: (String) -> Unit = {},
        onLeaveConfirmed: (String) -> Unit = {}
    ) {
        composeTestRule.setContent {
            CampsiteDetailsContent(
                campsite = campsite,
                lotteries = lotteries,
                joinActionFor = joinActionFor,
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
        show(lotteries = emptyList(), joinAction = JoinAction.NONE)
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
        val joined = mutableListOf<String>()
        show(onJoinConfirmed = { joined.add(it) })
        composeTestRule.onNodeWithText("Join Lottery").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Join lottery?").assertIsDisplayed()
        assertEquals(0, joined.size)
        composeTestRule.onNodeWithText("Join").performClick()
        assertEquals(listOf("fall-draw"), joined)
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
        val left = mutableListOf<String>()
        show(joinAction = JoinAction.LEAVE, onLeaveConfirmed = { left.add(it) })
        composeTestRule.onNodeWithText("You are on the waiting list.").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Join Lottery").assertDoesNotExist()
        composeTestRule.onNodeWithText("Leave Waiting List").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Leave waiting list?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leave").performClick()
        assertEquals(listOf("fall-draw"), left)
    }

    @Test
    fun showsEveryOpenLottery() {
        show(lotteries = listOf(fallDraw, winterDraw))
        composeTestRule.onNodeWithText("Open lotteries").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Fall long weekend draw").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Winter break draw").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Stay: 2026-12-20 to 2026-12-23").performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Join Lottery").assertCountEquals(2)
    }

    @Test
    fun singleLotteryIsLabelledOpenLottery() {
        show()
        composeTestRule.onNodeWithText("Open lottery").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun joiningTheSecondLotteryJoinsThatOne() {
        val joined = mutableListOf<String>()
        show(lotteries = listOf(fallDraw, winterDraw), onJoinConfirmed = { joined.add(it) })
        composeTestRule.onAllNodesWithText("Join Lottery")[1].performScrollTo().performClick()
        composeTestRule.onNodeWithText("Join lottery?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Join").performClick()
        assertEquals(listOf("winter-draw"), joined)
    }

    @Test
    fun joinDialogNamesTheLottery() {
        show(lotteries = listOf(fallDraw, winterDraw))
        composeTestRule.onAllNodesWithText("Join Lottery")[1].performScrollTo().performClick()
        composeTestRule.onNodeWithText(
            "You will be added to the waiting list for Winter break draw",
            substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun eachLotteryHasItsOwnButton() {
        show(
            lotteries = listOf(fallDraw, winterDraw),
            joinActionFor = { if (it.id == "fall-draw") JoinAction.LEAVE else JoinAction.JOIN }
        )
        composeTestRule.onNodeWithText("Leave Waiting List").performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Join Lottery").assertCountEquals(1)
    }

    @Test
    fun leavingOneLotteryLeavesThatOne() {
        val left = mutableListOf<String>()
        show(
            lotteries = listOf(fallDraw, winterDraw),
            joinActionFor = { if (it.id == "winter-draw") JoinAction.LEAVE else JoinAction.JOIN },
            onLeaveConfirmed = { left.add(it) }
        )
        composeTestRule.onNodeWithText("Leave Waiting List").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Leave").performClick()
        assertEquals(listOf("winter-draw"), left)
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
        show(campsite = null, lotteries = emptyList(), isLoading = true)
        composeTestRule.onNodeWithText("Loading campsite...").assertIsDisplayed()
    }

    @Test
    fun notFoundShowsMessageAndBackStillWorks() {
        var backClicked = false
        show(campsite = null, lotteries = emptyList(), notFound = true, onBackClick = { backClicked = true })
        composeTestRule.onNodeWithText("This campsite could not be found.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun loadErrorShowsMessageAndRetry() {
        var retries = 0
        show(campsite = null, lotteries = emptyList(), loadError = "Network down", onRetryClick = { retries++ })
        composeTestRule.onNodeWithText("Network down").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }
}
