package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.owner.OwnerMyLotteriesContent
import com.example.zephyr_bbb.owner.OwnerMyLotteriesScreen
import com.example.zephyr_bbb.owner.parseLotteryDate

import com.google.firebase.Timestamp
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OwnerMyLotteriesScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun start(text: String) = Timestamp(parseLotteryDate(text)!!)


    private val campsiteNames = mapOf(
        "c1" to "Pine Creek A12",
        "c2" to "Willow Bend B04",
        "c3" to "Elk Ridge C07"
    )

    // The three lotteries from the My Lotteries storyboard, as of October 17, 2026
    private val now = parseLotteryDate("2026-10-17")!!

    private val lotteries = listOf(
        Lottery(
            id = "l1",
            title = "October Foothills",
            campsiteIds = listOf("c1", "c2"),
            registrationOpensAt = start("2026-10-01"),
            status = "drawn"
        ),
        Lottery(
            id = "l2",
            title = "Autumn Campout",
            campsiteIds = listOf("c3"),
            registrationOpensAt = start("2026-10-15"),
            status = "open"
        ),
        Lottery(
            id = "l3",
            title = "Late October Weekends",
            campsiteIds = listOf("c1"),
            registrationOpensAt = start("2026-10-20"),
            status = "open"
        )
    )

    private var backClicked = false
    private var createClicked = false
    private var clickedLottery: Lottery? = null

    private fun showList(list: List<Lottery> = lotteries) {
        composeTestRule.setContent {
            OwnerMyLotteriesContent(
                lotteries = list,
                campsiteNames = campsiteNames,
                now = now,
                isLoading = false,
                errorMessage = null,
                onBackClick = { backClicked = true },
                onCreateLotteryClick = { createClicked = true },
                onLotteryClick = { clickedLottery = it }
            )
        }
    }

    @Test
    fun myLotteriesListsEachLottery() {
        showList()
        composeTestRule.onNodeWithText("October Foothills").assertExists()
        composeTestRule.onNodeWithText("Pine Creek A12, Willow Bend B04").assertExists()
        composeTestRule.onNodeWithText("Registration: Oct 1–14, 2026").assertExists()
        composeTestRule.onNodeWithText("Autumn Campout").assertExists()
        composeTestRule.onNodeWithText("Elk Ridge C07").assertExists()
        composeTestRule.onNodeWithText("Registration: Oct 15–19, 2026").assertExists()
        composeTestRule.onNodeWithText("Late October Weekends").assertExists()
        composeTestRule.onNodeWithText("Registration: Oct 20–23, 2026").assertExists()
    }

    @Test
    fun myLotteriesShowsStatuses() {
        showList()
        composeTestRule.onNodeWithText("Status: Completed").assertExists()
        composeTestRule.onNodeWithText("Status: Active").assertExists()
        composeTestRule.onNodeWithText("Status: Upcoming").assertExists()
    }

    @Test
    fun myLotteriesCreateWorks() {
        showList()
        composeTestRule.onNodeWithText("Create Lottery").performClick()
        assertTrue(createClicked)
    }
}