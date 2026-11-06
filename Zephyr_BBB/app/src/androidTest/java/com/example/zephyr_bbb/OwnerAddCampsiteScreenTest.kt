package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.owner.OwnerAddCampsiteScreen
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Tests for OwnerAddCampsiteScreen.kt
// Tests to make sure that all the input fields show up, and that the back works, and that the input
// field also works

@RunWith(AndroidJUnit4::class)
class OwnerAddCampsiteScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun addCampsiteScreenInputFields() {
        composeTestRule.setContent {
            OwnerAddCampsiteScreen(
                onBackClick = {}
            )
        }
        composeTestRule.onNodeWithText("Add Campsite").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter Campsite Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter Campsite Location").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter Campsite Description").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Campsite").assertIsDisplayed()
    }

    @Test
    fun addCampsiteScreenBackWorks() {
        var backClicked = false
        composeTestRule.setContent {
            OwnerAddCampsiteScreen(
                onBackClick = { backClicked = true }
            )
        }
        composeTestRule.onNodeWithText("Back").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun testAddCampsiteScreenTextInput() {
        composeTestRule.setContent {
            OwnerAddCampsiteScreen(
                onBackClick = {}
            )
        }
        composeTestRule.onNodeWithText("Enter Campsite Name").performTextInput("Campsite-1")
        composeTestRule.onNodeWithText("Campsite-1").assertIsDisplayed()
    }
}
