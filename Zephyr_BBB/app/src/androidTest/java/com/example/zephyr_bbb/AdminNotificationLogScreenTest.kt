package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.admin.AdminNotificationLogScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// ui tests for the admin notification log screen
@RunWith(AndroidJUnit4::class)
class AdminNotificationLogScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // notification screen opens with its title and back button
    @Test
    fun notificationScreenShowsTitleAndBack() {
        composeTestRule.setContent {
            AdminNotificationLogScreen(
                onBackClick = {}
            )
        }

        composeTestRule.onNodeWithText("All Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
    }

    // back button leaves the notification screen
    @Test
    fun backButtonLeavesNotificationScreen() {
        var backClicked = false

        composeTestRule.setContent {
            AdminNotificationLogScreen(
                onBackClick = { backClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Back").performClick()

        assertTrue(backClicked)
    }
}