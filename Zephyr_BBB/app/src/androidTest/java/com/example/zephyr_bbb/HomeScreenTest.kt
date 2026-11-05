package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        composeTestRule.onNodeWithText("Weclome to Zephyr").assertIsDisplayed()
    }
}