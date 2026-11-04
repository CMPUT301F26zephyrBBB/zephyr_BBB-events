package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.owner.OwnerMyCampsitesScreen
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Tests for OwnerMyCampsiteScreen.kt
// Tests to make sure all the buttons show up, and that both the back and add buttons work

@RunWith(AndroidJUnit4::class)
class OwnerMyCampsitesScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ownerMyCampsitesScreenButtons(){
        composeTestRule.setContent {
            OwnerMyCampsitesScreen(
                onBackClick = {},
                onAddCampsiteClick = {},
                onCampsiteClick = {}
            )
        }
        composeTestRule.onNodeWithText("My Campsites").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add Campsite").assertIsDisplayed()
    }

    @Test
    fun ownerMyCampsitesScreenBackWorks(){
        var backClicked = false
        composeTestRule.setContent {
            OwnerMyCampsitesScreen(
                onAddCampsiteClick = {},
                onCampsiteClick = {},
                onBackClick = {backClicked = true}
            )
        }
        composeTestRule.onNodeWithText("Back").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun ownerMyCampsitesScreenAddWorks() {
        var addClicked = false
        composeTestRule.setContent {
            OwnerMyCampsitesScreen(
                onAddCampsiteClick = {addClicked = true},
                onCampsiteClick = {},
                onBackClick = {}
            )
        }
        composeTestRule.onNodeWithText("Add Campsite").performClick()
        assertTrue(addClicked)
    }
}