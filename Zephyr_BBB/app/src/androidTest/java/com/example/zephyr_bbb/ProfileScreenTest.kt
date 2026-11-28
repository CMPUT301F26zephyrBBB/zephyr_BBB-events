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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.entrant.EntrantUtils
import com.example.zephyr_bbb.entrant.NOTIFICATIONS_SWITCH_TAG
import com.example.zephyr_bbb.entrant.ProfileContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// ui tests for the entrant profile screen (US 01.07.03, US 01.08.01, US 01.08.02, US 01.10.01, US 01.11.01)
@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // the value passed to the toggle callback, null if it was never called
    private var toggledTo: Boolean? = null
    private var dialogDismissed = false
    private var policyClicked = false
    private var deleteClicked = false
    private var deleteConfirmed = false
    private var deleteDismissed = false
    private var changePhotoClicked = false
    private var removePhotoClicked = false
    private var uploadConfirmed = false
    private var uploadCancelled = false
    private var removeConfirmed = false
    private var removeCancelled = false

    // shows the profile content with fake data and records the callbacks
    private fun showProfile(
        notificationsEnabled: Boolean = true,
        notificationsToggleEnabled: Boolean = true,
        showNotificationsOffDialog: Boolean = false,
        showDeleteDialog: Boolean = false,
        isDeleting: Boolean = false,
        photoUrl: String? = null,
        name: String = "Test Entrant",
        hasProfileImage: Boolean = false,
        isPhotoBusy: Boolean = false,
        showUploadPhotoDialog: Boolean = false,
        showRemovePhotoDialog: Boolean = false
    ) {
        composeTestRule.setContent {
            ProfileContent(
                photoUrl = photoUrl,
                initials = EntrantUtils.initials(name),
                hasProfileImage = hasProfileImage,
                isPhotoBusy = isPhotoBusy,
                onChangePhotoClick = { changePhotoClicked = true },
                onRemovePhotoClick = { removePhotoClicked = true },
                showUploadPhotoDialog = showUploadPhotoDialog,
                onConfirmUploadPhoto = { uploadConfirmed = true },
                onCancelUploadPhoto = { uploadCancelled = true },
                showRemovePhotoDialog = showRemovePhotoDialog,
                onConfirmRemovePhoto = { removeConfirmed = true },
                onCancelRemovePhoto = { removeCancelled = true },
                name = name,
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
                onPolicyClick = { policyClicked = true },
                showDeleteDialog = showDeleteDialog,
                isDeleting = isDeleting,
                onDeleteClick = { deleteClicked = true },
                onDeleteConfirmed = { deleteConfirmed = true },
                onDeleteDismissed = { deleteDismissed = true },
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
                photoUrl = null, initials = "TE", hasProfileImage = false, isPhotoBusy = false,
                onChangePhotoClick = {}, onRemovePhotoClick = {},
                showUploadPhotoDialog = false, onConfirmUploadPhoto = {}, onCancelUploadPhoto = {},
                showRemovePhotoDialog = false, onConfirmRemovePhoto = {}, onCancelRemovePhoto = {},
                name = "Test Entrant", onNameChange = {},
                email = "", onEmailChange = {},
                phone = "", onPhoneChange = {},
                isSaving = false, message = null, errorMessage = null, onSaveClick = {},
                notificationsEnabled = false,
                notificationsToggleEnabled = true,
                onNotificationsToggle = {},
                showNotificationsOffDialog = showDialog,
                onDismissNotificationsOffDialog = { showDialog = false },
                onPolicyClick = {},
                showDeleteDialog = false,
                isDeleting = false,
                onDeleteClick = {},
                onDeleteConfirmed = {},
                onDeleteDismissed = {},
                onBackClick = {}
            )
        }
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.onNodeWithText("Push notifications turned off").assertDoesNotExist()
    }

    // the policy link is at the bottom of the profile and opens the policy
    @Test
    fun policyLinkOpensPolicy() {
        showProfile()
        composeTestRule.onNodeWithText("App policy and guidelines").performScrollTo().performClick()
        assertTrue(policyClicked)
    }

    // the delete button is on the profile and asks to delete
    @Test
    fun deleteButtonIsDisplayedAndClickable() {
        showProfile()
        composeTestRule.onNodeWithText("Delete profile").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete profile").performClick()
        assertTrue(deleteClicked)
    }

    // nothing is deleted until the user confirms
    @Test
    fun deleteDialogHiddenByDefault() {
        showProfile()
        composeTestRule.onNodeWithText("Delete profile?").assertDoesNotExist()
        assertFalse(deleteConfirmed)
    }

    // the dialog explains that deleting can't be undone
    @Test
    fun deleteDialogShowsWarning() {
        showProfile(showDeleteDialog = true)
        composeTestRule.onNodeWithText("Delete profile?").assertIsDisplayed()
        composeTestRule.onNodeWithText("can't be undone", substring = true).assertIsDisplayed()
    }

    // confirming in the dialog deletes the profile
    @Test
    fun confirmingDeleteCallsOnDeleteConfirmed() {
        showProfile(showDeleteDialog = true)
        composeTestRule.onNodeWithText("Delete").performClick()
        assertTrue(deleteConfirmed)
        assertFalse(deleteDismissed)
    }

    // cancelling the dialog keeps the profile
    @Test
    fun cancellingDeleteCallsOnDeleteDismissed() {
        showProfile(showDeleteDialog = true)
        composeTestRule.onNodeWithText("Cancel").performClick()
        assertTrue(deleteDismissed)
        assertFalse(deleteConfirmed)
    }

    // while deleting, the buttons can't be pressed again
    @Test
    fun buttonsDisabledWhileDeleting() {
        showProfile(showDeleteDialog = true, isDeleting = true)
        composeTestRule.onNodeWithText("Deleting…").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Cancel").assertIsNotEnabled()
    }

    // the dialog closes after cancelling when the screen state updates
    @Test
    fun deleteDialogClosesAfterCancel() {
        var showDialog by mutableStateOf(true)
        composeTestRule.setContent {
            ProfileContent(
                photoUrl = null, initials = "TE", hasProfileImage = false, isPhotoBusy = false,
                onChangePhotoClick = {}, onRemovePhotoClick = {},
                showUploadPhotoDialog = false, onConfirmUploadPhoto = {}, onCancelUploadPhoto = {},
                showRemovePhotoDialog = false, onConfirmRemovePhoto = {}, onCancelRemovePhoto = {},
                name = "Test Entrant", onNameChange = {},
                email = "", onEmailChange = {},
                phone = "", onPhoneChange = {},
                isSaving = false, message = null, errorMessage = null, onSaveClick = {},
                notificationsEnabled = true,
                notificationsToggleEnabled = true,
                onNotificationsToggle = {},
                showNotificationsOffDialog = false,
                onDismissNotificationsOffDialog = {},
                onPolicyClick = {},
                showDeleteDialog = showDialog,
                isDeleting = false,
                onDeleteClick = { showDialog = true },
                onDeleteConfirmed = {},
                onDeleteDismissed = { showDialog = false },
                onBackClick = {}
            )
        }
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Delete profile?").assertDoesNotExist()
    }

    // profile photo (US 01.08.02)

    // with no photo, the placeholder shows initials and only Add photo is offered
    @Test
    fun noPhotoShowsPlaceholderAndAddButton() {
        showProfile()
        composeTestRule.onNodeWithContentDescription("No profile photo").assertIsDisplayed()
        composeTestRule.onNodeWithText("TE").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add photo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Change photo").assertDoesNotExist()
        composeTestRule.onNodeWithText("Remove photo").assertDoesNotExist()
    }

    // with a blank name there are no initials to show
    @Test
    fun blankNameShowsNoInitials() {
        showProfile(name = "")
        composeTestRule.onNodeWithContentDescription("No profile photo").assertIsDisplayed()
        composeTestRule.onNodeWithText("TE").assertDoesNotExist()
    }

    // with a photo, the photo is shown and it can be changed or removed
    @Test
    fun photoShowsChangeAndRemoveButtons() {
        showProfile(photoUrl = "https://example.com/photo.jpg", hasProfileImage = true)
        composeTestRule.onNodeWithContentDescription("Profile photo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Change photo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Remove photo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add photo").assertDoesNotExist()
    }

    @Test
    fun addPhotoCallsOnChangePhotoClick() {
        showProfile()
        composeTestRule.onNodeWithText("Add photo").performClick()
        assertTrue(changePhotoClicked)
    }

    @Test
    fun removePhotoCallsOnRemovePhotoClick() {
        showProfile(hasProfileImage = true)
        composeTestRule.onNodeWithText("Remove photo").performClick()
        assertTrue(removePhotoClicked)
    }

    // the photo buttons can't be pressed while a photo is uploading or being removed
    @Test
    fun photoButtonsDisabledWhileBusy() {
        showProfile(hasProfileImage = true, isPhotoBusy = true)
        composeTestRule.onNodeWithText("Change photo").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Remove photo").assertIsNotEnabled()
    }

    @Test
    fun photoButtonsEnabledWhenNotBusy() {
        showProfile(hasProfileImage = true)
        composeTestRule.onNodeWithText("Change photo").assertIsEnabled()
        composeTestRule.onNodeWithText("Remove photo").assertIsEnabled()
    }

    // after picking a photo, the user confirms before it's uploaded
    @Test
    fun uploadDialogConfirmCallsOnConfirmUpload() {
        showProfile(showUploadPhotoDialog = true)
        composeTestRule.onNodeWithText("Upload profile image?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Upload").performClick()
        assertTrue(uploadConfirmed)
        assertFalse(uploadCancelled)
    }

    @Test
    fun uploadDialogCancelCallsOnCancelUpload() {
        showProfile(showUploadPhotoDialog = true)
        composeTestRule.onNodeWithText("Cancel").performClick()
        assertTrue(uploadCancelled)
        assertFalse(uploadConfirmed)
    }

    @Test
    fun photoDialogsHiddenByDefault() {
        showProfile(hasProfileImage = true)
        composeTestRule.onNodeWithText("Upload profile image?").assertDoesNotExist()
        composeTestRule.onNodeWithText("Remove profile photo?").assertDoesNotExist()
    }

    // removing the photo asks first
    @Test
    fun removeDialogConfirmCallsOnConfirmRemove() {
        showProfile(hasProfileImage = true, showRemovePhotoDialog = true)
        composeTestRule.onNodeWithText("Remove profile photo?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Remove").performClick()
        assertTrue(removeConfirmed)
        assertFalse(removeCancelled)
    }

    @Test
    fun removeDialogCancelCallsOnCancelRemove() {
        showProfile(hasProfileImage = true, showRemovePhotoDialog = true)
        composeTestRule.onNodeWithText("Cancel").performClick()
        assertTrue(removeCancelled)
        assertFalse(removeConfirmed)
    }
}
