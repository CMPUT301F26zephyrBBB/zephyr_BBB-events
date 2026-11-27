package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Test for HomeScreen.kt
// Tests to make sure it initializes properly

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreenTitleAndWelcomeTextDisplayed() {
        composeTestRule.setContent{
            HomeScreen(
                onOwnerClick = {},
                onAdminClick = {}
            )
        }
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome to Zephyr").assertIsDisplayed()
    }

    // the profile icon is in the header and can be clicked
    @Test
    fun profileIconIsDisplayed() {
        composeTestRule.setContent {
            HomeScreen(
                onOwnerClick = {},
                onAdminClick = {}
            )
        }
        composeTestRule.onNodeWithContentDescription("My profile")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    // clicking the profile icon opens the profile
    @Test
    fun profileIconOpensProfile() {
        var profileClicked = false
        composeTestRule.setContent {
            HomeScreen(
                onOwnerClick = {},
                onAdminClick = {},
                onProfileClick = { profileClicked = true }
            )
        }
        composeTestRule.onNodeWithContentDescription("My profile").performClick()
        assertTrue(profileClicked)
    }
}
