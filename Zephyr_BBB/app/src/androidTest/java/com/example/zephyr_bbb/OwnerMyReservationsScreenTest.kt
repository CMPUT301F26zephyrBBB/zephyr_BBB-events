package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.owner.OwnerMyReservationsScreen
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for OwnerMyReservationsScreen.kt
 * Tests Buttons, displayed on screen, checks to see if the correct text also appears, also checks to
 * see if the booked dates button works, as well as checks to see if the reservation type dropdown works
 */

@RunWith(AndroidJUnit4::class)
class OwnerMyReservationsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ownerMyReservationsScreenButtons(){
        composeTestRule.setContent{
            OwnerMyReservationsScreen(
                onBackClick = {},
                onMessageClick = {}
            )
        }
        composeTestRule.onNodeWithText("Reservations").assertIsDisplayed()
        composeTestRule.onNodeWithText("<").assertIsDisplayed()
        composeTestRule.onNodeWithText("Type of Reservation").assertIsDisplayed()
        composeTestRule.onNodeWithText("View Booked Dates").assertIsDisplayed()
    }

    @Test
    fun ownerMyReservationsScreenBackWorks() {
        var backClicked = false
        composeTestRule.setContent {
            OwnerMyReservationsScreen(
                onBackClick = { backClicked = true },
                onMessageClick = {}
            )
        }
        composeTestRule.onNodeWithText("<").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun ownerMyReservationsScreenViewDatesWork(){
        composeTestRule.setContent {
            OwnerMyReservationsScreen(
                onBackClick = {},
                onMessageClick = {}
            )
        }
        composeTestRule.onNodeWithText("View Booked Dates").performClick()
        composeTestRule.onNodeWithText("Booked Dates").assertIsDisplayed()
    }

    @Test
    fun ownerMyReservationsScreenDropdownWorks(){
        composeTestRule.setContent {
            OwnerMyReservationsScreen(
                onBackClick = {},
                onMessageClick = {}
            )
        }
        composeTestRule.onNodeWithText("Type of Reservation").performClick()
        composeTestRule.onNodeWithText("All Reservations").assertIsDisplayed()
        composeTestRule.onNodeWithText("Active Reservations").assertIsDisplayed()
        composeTestRule.onNodeWithText("Past Reservations").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancelled Reservations").assertIsDisplayed()
    }
}