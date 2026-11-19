package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.admin.AdminCampsiteScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// ui tests for the admin campsite screen
@RunWith(AndroidJUnit4::class)
class AdminCampsiteScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // campsite screen opens with its title and back button
    @Test
    fun campsiteScreenShowsTitleAndBack() {
        composeTestRule.setContent {
            AdminCampsiteScreen(
                onBackClick = {}
            )
        }

        composeTestRule.onNodeWithText("All Campsites").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
    }

    // back button leaves the campsite screen
    @Test
    fun backButtonLeavesCampsiteScreen() {
        var backClicked = false

        composeTestRule.setContent {
            AdminCampsiteScreen(
                onBackClick = { backClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Back").performClick()

        assertTrue(backClicked)
    }
}