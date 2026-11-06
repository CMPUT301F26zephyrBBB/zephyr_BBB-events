package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.owner.OwnerEditCampsiteScreen
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Tests for OwnerEditCampsiteScreen.kt
// Tests to make sure all input fields are there, and that the back and inputs work

@RunWith(AndroidJUnit4::class)
class OwnerEditCampsiteScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun editCampsiteScreenInputFields() {
        composeTestRule.setContent {
            OwnerEditCampsiteScreen(
                campsiteEditId = "test_campsite_1",
                onBackClick = {}
            )
        }
        composeTestRule.onNodeWithText("Edit Campsite").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Campsite Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Campsite Location").assertIsDisplayed()
        composeTestRule.onNodeWithText("Campsite Description").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Changes").assertIsDisplayed()
    }

    @Test
    fun editCampsiteScreenBackWorks() {
        var backClicked = false

        composeTestRule.setContent {
            OwnerEditCampsiteScreen(
                campsiteEditId = "Campsite-1",
                onBackClick = { backClicked = true }
            )
        }
        composeTestRule.onNodeWithText("Back").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun editCampsiteScreenTextInput() {
        composeTestRule.setContent {
            OwnerEditCampsiteScreen(
                campsiteEditId = "Campsite-1",
                onBackClick = {}
            )
        }
        composeTestRule.onNodeWithText("Campsite Name").performTextInput("Campsite-2")
        composeTestRule.onNodeWithText("Campsite-2").assertIsDisplayed()
    }
}
