package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.entrant.APP_GUIDELINES
import com.example.zephyr_bbb.entrant.APP_POLICY
import com.example.zephyr_bbb.entrant.AppPolicyScreen
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// ui tests for the app policy and guidelines screen (US 01.11.01)
@RunWith(AndroidJUnit4::class)
class AppPolicyScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var backClicked = false

    // show the policy screen before each test
    @Before
    fun setUp() {
        backClicked = false
        composeTestRule.setContent {
            AppPolicyScreen(onBackClick = { backClicked = true })
        }
    }

    // the screen title is shown
    @Test
    fun titleIsDisplayed() {
        composeTestRule.onNodeWithText("App Policy and Guidelines").assertIsDisplayed()
    }

    // both sections are on the screen
    @Test
    fun policyAndGuidelinesSectionsAreDisplayed() {
        composeTestRule.onNodeWithText("App Policy").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Guidelines").performScrollTo().assertIsDisplayed()
    }

    // every policy point and guideline is shown, scrolling down if needed
    @Test
    fun allPointsAreDisplayed() {
        (APP_POLICY.points + APP_GUIDELINES.points).forEach { point ->
            composeTestRule.onNodeWithText(point).performScrollTo().assertIsDisplayed()
        }
    }

    // back button leaves the screen
    @Test
    fun backButtonCallsOnBackClick() {
        composeTestRule.onNodeWithText("Back").performClick()
        assertTrue(backClicked)
    }
}
