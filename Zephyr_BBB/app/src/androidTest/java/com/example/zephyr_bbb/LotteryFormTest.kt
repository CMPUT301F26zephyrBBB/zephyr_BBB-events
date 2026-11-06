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
import com.example.zephyr_bbb.owner.toFormFields
import com.example.zephyr_bbb.owner.toLottery
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
* Tests for OwnerLotteryForm.kt, the Create / Edit Lottery form. They run on the emulator with fake
* campsites, so they do not use Firebase.
* The form shows its fields, refuses to submit without a name, and builds the Lottery.
* Several campsites can be ticked.
* The registration fields accept typed dates.
* A booking period that ends too early is not submitted.
* A lottery converts back into form fields for editing, and errors from the screen are shown.
*/

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
        campsiteIds = setOf("c1", "c2"),
        registrationStart = "2026-10-01",
        registrationEnd = "2026-10-14",
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

    /** Text can be typed into the period, winner and capacity fields. */
    @Test
    fun lotteryFormEntersPeriodsWinnersAndCapacity() {
        showForm()

        composeTestRule.onNodeWithText("Lottery name").performTextInput("October Foothills")
        composeTestRule.onNodeWithText("Registration start").performScrollTo().performTextInput("2026-10-01")
        composeTestRule.onNodeWithText("Registration end").performScrollTo().performTextInput("2026-10-14")
        composeTestRule.onNodeWithText("Booking start").performScrollTo().performTextInput("2026-10-18")
        composeTestRule.onNodeWithText("Booking end").performScrollTo().performTextInput("2026-10-31")
        composeTestRule.onNodeWithText("Stay start").performScrollTo().performTextInput("2026-10-18")
        composeTestRule.onNodeWithText("Stay end (checkout)").performScrollTo().performTextInput("2026-11-01")
        composeTestRule.onNodeWithText("Winner count").performScrollTo().performTextReplacement("8")
        composeTestRule.onNodeWithText("Waitlist capacity (optional)").performScrollTo().performTextInput("20")

        assertEquals("October Foothills", latestFields.title)
        assertEquals("2026-10-01", latestFields.registrationStart)
        assertEquals("2026-10-14", latestFields.registrationEnd)
        assertEquals("2026-10-18", latestFields.bookingStart)
        assertEquals("2026-10-31", latestFields.bookingEnd)
        assertEquals("2026-10-18", latestFields.stayStart)
        assertEquals("2026-11-01", latestFields.stayEnd)
        assertEquals("8", latestFields.winnerCount)
        assertEquals("20", latestFields.waitlistCapacity)
    }

    /** Publishing without a name shows an error and does not submit. */
    @Test
    fun lotteryFormDoesNotSubmitWithoutName() {
        showForm(startFields = validFields.copy(title = ""))

        composeTestRule.onNodeWithText("Publish").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Enter a lottery name.").performScrollTo().assertIsDisplayed()
        assertEquals(0, submitCount)
    }

    /** Publishing with a booking period that ends too early shows an error and does not submit. */
    @Test
    fun lotteryFormDoesNotSubmitBadDates() {
        showForm(startFields = validFields.copy(bookingEnd = "2026-10-10"))

        composeTestRule.onNodeWithText("Publish").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Booking must end on or after the day it starts.")
            .performScrollTo()
            .assertIsDisplayed()
        assertEquals(0, submitCount)
    }

    /** An error passed in by the screen, such as a capacity below the waitlist size, is shown. */
    @Test
    fun lotteryFormShowsErrorFromScreen() {
        showForm(
            startFields = validFields,
            errorMessage = "Capacity cannot be below the current waitlist size."
        )

        composeTestRule.onNodeWithText("Capacity cannot be below the current waitlist size.")
            .performScrollTo()
            .assertIsDisplayed()
    }
    /** The form values become the Lottery that is saved, including the inclusive closing times. */
    @Test
    fun formFieldsConvertToLottery() {
        val lottery = validFields.copy(waitlistCapacity = "20").toLottery(id = "lottery_1")

        assertEquals("lottery_1", lottery.id)
        assertEquals("October Foothills", lottery.title)
        assertEquals(setOf("c1", "c2"), lottery.campsiteIds.toSet())
        assertEquals(8L, lottery.winnerCount)
        assertEquals(20L, lottery.waitlistCapacity)
        assertEquals("2026-10-18", lottery.stayStartDate)
        assertEquals("2026-11-01", lottery.stayEndDate)
        // Registration closes at the start of the day after the last registration day
        assertTrue(lottery.registrationClosesAt!! > lottery.registrationOpensAt!!)
        assertTrue(lottery.bookingClosesAt!! > lottery.registrationClosesAt!!)
    }

    /** A lottery turns into form fields and back without losing anything, which is how editing fills the form. */
    @Test
    fun lotteryConvertsBackToSameFormFields() {
        val fields = validFields.copy(waitlistCapacity = "20")
        assertEquals(fields, fields.toLottery(id = "lottery_1").toFormFields())
        assertFalse(validFields.toLottery().toFormFields().waitlistCapacity.isNotBlank())
    }
}