package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.admin.AdminImageScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// ui tests for the admin image screen
@RunWith(AndroidJUnit4::class)
class AdminImageScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // image screen opens with its title and back button
    @Test
    fun imageScreenShowsTitleAndBack() {
        composeTestRule.setContent {
            AdminImageScreen(
                onBackClick = {}
            )
        }

        composeTestRule.onNodeWithText("Manage Images").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
    }

    // back button leaves the image screen
    @Test
    fun backButtonLeavesImageScreen() {
        var backClicked = false

        composeTestRule.setContent {
            AdminImageScreen(
                onBackClick = { backClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Back").performClick()

        assertTrue(backClicked)
    }
}