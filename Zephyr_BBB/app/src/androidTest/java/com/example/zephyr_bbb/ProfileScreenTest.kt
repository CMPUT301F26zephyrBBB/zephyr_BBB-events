package com.example.zephyr_bbb

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.entrant.NOTIFICATIONS_SWITCH_TAG
import com.example.zephyr_bbb.entrant.ProfileContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// ui tests for the entrant profile screen (US 01.07.03, US 01.10.01)
@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // the value passed to the toggle callback, null if it was never called
    private var toggledTo: Boolean? = null
    private var dialogDismissed = false

    // shows the profile content with fake data and records the callbacks
    private fun showProfile(
        notificationsEnabled: Boolean = true,
        notificationsToggleEnabled: Boolean = true,
        showNotificationsOffDialog: Boolean = false
    ) {
        composeTestRule.setContent {
            ProfileContent(
                name = "Test Entrant",
                onNameChange = {},
                email = "",
                onEmailChange = {},
                phone = "",
                onPhoneChange = {},
                isSaving = false,
                message = null,
                errorMessage = null,
                onSaveClick = {},
                notificationsEnabled = notificationsEnabled,
                notificationsToggleEnabled = notificationsToggleEnabled,
                onNotificationsToggle = { toggledTo = it },
                showNotificationsOffDialog = showNotificationsOffDialog,
                onDismissNotificationsOffDialog = { dialogDismissed = true },
                onBackClick = {}
            )
        }
    }

    // the toggle and its label should be on the profile screen
    @Test
    fun notificationToggleIsDisplayed() {
        showProfile()
        composeTestRule.onNodeWithText("Push Notifications").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(NOTIFICATIONS_SWITCH_TAG).assertIsDisplayed()
    }

    // the switch shows the saved preference
    @Test
    fun switchReflectsSavedPreference() {
        showProfile(notificationsEnabled = true)
        composeTestRule.onNodeWithTag(NOTIFICATIONS_SWITCH_TAG).performScrollTo().assertIsOn()
    }

    @Test
    fun switchShowsOffWhenOptedOut() {
        showProfile(notificationsEnabled = false)
        composeTestRule.onNodeWithTag(NOTIFICATIONS_SWITCH_TAG).performScrollTo().assertIsOff()
    }

    // turning the switch off asks to opt out
    @Test
    fun clickingSwitchWhenOnOptsOut() {
        showProfile(notificationsEnabled = true)
        composeTestRule.onNodeWithTag(NOTIFICATIONS_SWITCH_TAG).performScrollTo().performClick()
        assertEquals(false, toggledTo)
    }

    // turning the switch on asks to opt back in
    @Test
    fun clickingSwitchWhenOffOptsIn() {
        showProfile(notificationsEnabled = false)
        composeTestRule.onNodeWithTag(NOTIFICATIONS_SWITCH_TAG).performScrollTo().performClick()
        assertEquals(true, toggledTo)
    }

    // clicking the label also flips the switch
    @Test
    fun clickingLabelTogglesSwitch() {
        showProfile(notificationsEnabled = true)
        composeTestRule.onNodeWithText("Push Notifications").performScrollTo().performClick()
        assertEquals(false, toggledTo)
    }

    // the switch can't be flipped while a save is in progress
    @Test
    fun switchIsDisabledWhileSaving() {
        showProfile(notificationsToggleEnabled = false)
        composeTestRule.onNodeWithTag(NOTIFICATIONS_SWITCH_TAG).performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithTag(NOTIFICATIONS_SWITCH_TAG).performClick()
        assertNull(toggledTo)
    }

    @Test
    fun switchIsEnabledWhenNotSaving() {
        showProfile()
        composeTestRule.onNodeWithTag(NOTIFICATIONS_SWITCH_TAG).performScrollTo().assertIsEnabled()
    }

    // the opt out dialog explains what changed and can be closed
    @Test
    fun optOutDialogShowsAndDismisses() {
        showProfile(notificationsEnabled = false, showNotificationsOffDialog = true)
        composeTestRule.onNodeWithText("Push notifications turned off").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").performClick()
        assertTrue(dialogDismissed)
    }

    // no dialog unless the user just opted out
    @Test
    fun optOutDialogHiddenByDefault() {
        showProfile()
        composeTestRule.onNodeWithText("Push notifications turned off").assertDoesNotExist()
    }

    // the dialog goes away once the screen stops asking for it
    @Test
    fun optOutDialogClosesWhenStateCleared() {
        var showDialog by mutableStateOf(true)
        composeTestRule.setContent {
            ProfileContent(
                name = "Test Entrant", onNameChange = {},
                email = "", onEmailChange = {},
                phone = "", onPhoneChange = {},
                isSaving = false, message = null, errorMessage = null, onSaveClick = {},
                notificationsEnabled = false,
                notificationsToggleEnabled = true,
                onNotificationsToggle = {},
                showNotificationsOffDialog = showDialog,
                onDismissNotificationsOffDialog = { showDialog = false },
                onBackClick = {}
            )
        }
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.onNodeWithText("Push notifications turned off").assertDoesNotExist()
    }
}
