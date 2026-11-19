package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.admin.AdminUserScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// ui tests for the admin profile screen
@RunWith(AndroidJUnit4::class)
class AdminUserScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // profile screen opens with its title and back button
    @Test
    fun profileScreenShowsTitleAndBack() {
        composeTestRule.setContent {
            AdminUserScreen(
                onBackClick = {}
            )
        }

        composeTestRule.onNodeWithText("All Profiles").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
    }

    // back button leaves the profile screen
    @Test
    fun backButtonLeavesProfileScreen() {
        var backClicked = false

        composeTestRule.setContent {
            AdminUserScreen(
                onBackClick = { backClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Back").performClick()

        assertTrue(backClicked)
    }
}