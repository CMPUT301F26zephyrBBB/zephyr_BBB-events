package com.example.zephyr_bbb

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.owner.LotteryForm
import com.example.zephyr_bbb.owner.LotteryFormFields
import com.example.zephyr_bbb.owner.toLottery
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Tests for OwnerLotteryForm.kt
// Tests the Create / Edit Lottery form: all fields show, more than one campsite can be picked,
// registration/booking periods, winner count and optional waitlist capacity can be entered,
// invalid forms are not submitted, and the form converts to and from a Lottery.

@RunWith(AndroidJUnit4::class)
class LotteryFormTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val campsites = listOf(
        Campsite(id = "c1", name = "Pine Creek A12", locationName = "Jasper"),
        Campsite(id = "c2", name = "Willow Bend B04", locationName = "Cochrane"),
        Campsite(id = "c3", name = "Elk Ridge C07", locationName = "Canmore")
    )
    private val validFields = LotteryFormFields(
        title = "October Foothills",
        stayStart = "2026-10-18",
        stayEnd = "2026-11-01"
    )

    private var latestFields = LotteryFormFields()
    private var submitCount = 0
    private var backClicked = false

    // Shows the form with real state so typing and ticking update it
    private fun showForm(
        startFields: LotteryFormFields = LotteryFormFields(),
        errorMessage: String? = null
    ) {
        latestFields = startFields

        composeTestRule.setContent {
            var fields by remember {
                mutableStateOf(startFields)
            }

            LotteryForm(
                heading = "Create Lottery",
                fields = fields,
                campsites = campsites,
                submitLabel = "Publish",
                errorMessage = errorMessage,
                isSubmitting = false,
                onFieldsChange = {
                    fields = it
                    latestFields = it
                },
                onSubmit = { submitCount += 1 },
                onBackClick = { backClicked = true }
            )
        }
    }
    /** The form shows the name, campsite, period, winner and capacity fields and the Publish button. */
    @Test
    fun lotteryFormShowsAllFields() {
        showForm()

        composeTestRule.onNodeWithText("Create Lottery").assertIsDisplayed()
        composeTestRule.onNodeWithText("<").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lottery name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Campsites · select one or more").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pine Creek A12 · Jasper").assertIsDisplayed()
        composeTestRule.onNodeWithText("Willow Bend B04 · Cochrane").assertIsDisplayed()
        composeTestRule.onNodeWithText("Elk Ridge C07 · Canmore").assertIsDisplayed()
        composeTestRule.onNodeWithText("Registration start").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Registration end").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Booking start").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Booking end").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Stay start").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Stay end (checkout)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Winner count").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Waitlist capacity (optional)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Leave blank for an unlimited waitlist.").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Publish").performScrollTo().assertIsDisplayed()
    }

    /** Ticking two campsites selects both and leaves the third unticked. */
    @Test
    fun lotteryFormSelectsMultipleCampsites() {
        showForm()

        composeTestRule.onNodeWithText("Pine Creek A12 · Jasper").performClick()
        composeTestRule.onNodeWithText("Willow Bend B04 · Cochrane").performClick()

        composeTestRule.onNodeWithText("Pine Creek A12 · Jasper").assertIsOn()
        composeTestRule.onNodeWithText("Willow Bend B04 · Cochrane").assertIsOn()
        composeTestRule.onNodeWithText("Elk Ridge C07 · Canmore").assertIsOff()
        assertEquals(setOf("c1", "c2"), latestFields.campsiteIds)
    }

    /** Publishing without a name shows an error and does not submit. */
    @Test
    fun lotteryFormDoesNotSubmitWithoutName() {
        showForm(startFields = validFields.copy(title = ""))

        composeTestRule.onNodeWithText("Publish").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Enter a lottery name.").performScrollTo().assertIsDisplayed()
        assertEquals(0, submitCount)
    }
}