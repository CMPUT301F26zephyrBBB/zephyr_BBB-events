package com.example.zephyr_bbb.entrant

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zephyr_bbb.R
import com.example.zephyr_bbb.database.repository.ImageRepository
import com.example.zephyr_bbb.database.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Test tag for the push notifications switch row on the profile screen. */
internal const val NOTIFICATIONS_SWITCH_TAG = "notificationsSwitch"

/** Test tag for the circular profile photo (or placeholder) on the profile screen. */
internal const val PROFILE_AVATAR_TAG = "profileAvatar"

/** Size of the profile photo circle. */
private val AVATAR_SIZE = 96.dp

/**
 * Lets the entrant view and edit their profile (name, email, phone),
 * add, change or remove their profile photo, opt out of receiving notifications,
 * open the app policy and guidelines, and delete their profile.
 * US 01.07.03, US 01.08.01, US 01.08.02, US 01.10.01, US 01.11.01.
 *
 * This is the stateful wrapper: it loads the signed-in user, talks to
 * [UserRepository] and [ImageRepository], opens the system photo picker,
 * and passes plain values down to [ProfileContent].
 *
 * @param onBackClick is invoked when the user clicks the back button
 * @param onPolicyClick is invoked when the user clicks App policy and guidelines
 * @param onProfileDeleted is invoked after the profile has been deleted, so the
 * caller can leave this screen and clear the back stack
 */
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onPolicyClick: () -> Unit,
    onProfileDeleted: () -> Unit
) {
    DeviceIdentityGate { user ->
        val userRepository = remember { UserRepository() }
        val imageRepository = remember { ImageRepository() }
        val context = LocalContext.current

        // Start the fields with what's already saved for this user
        var name by remember(user.id) { mutableStateOf(user.name) }
        var email by remember(user.id) { mutableStateOf(user.email) }
        var phone by remember(user.id) { mutableStateOf(user.phone) }
        var notificationsEnabled by remember(user.id) { mutableStateOf(user.notificationsEnabled) }

        var isSaving by remember { mutableStateOf(false) }
        var isSavingNotifications by remember { mutableStateOf(false) }
        var showNotificationsOffDialog by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var isDeleting by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Profile photo state
        val scope = rememberCoroutineScope()
        var profileImageId by remember(user.id) { mutableStateOf(user.profileImageId) }
        var photoUrl by remember { mutableStateOf<String?>(null) }
        var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
        var showRemovePhotoDialog by remember { mutableStateOf(false) }
        var isPhotoBusy by remember { mutableStateOf(false) }

        // The system photo picker needs no storage permission
        val photoPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri -> if (uri != null) pendingPhotoUri = uri }

        // Look up the photo's download URL whenever the image id changes.
        // Coil then downloads and shows it, the same way the owner campsite screens do
        LaunchedEffect(profileImageId) {
            val imageId = profileImageId
            photoUrl = null
            if (imageId.isNullOrBlank()) return@LaunchedEffect
            imageRepository.getImageUrl(
                imageId = imageId,
                onSuccess = { url ->
                    // Ignore a late answer for a photo that has since been changed or removed
                    if (profileImageId == imageId) photoUrl = url
                },
                onFailure = {
                    Log.w("ProfileScreen", "Could not get profile photo URL.", it)
                    errorMessage = "Could not load your profile photo."
                }
            )
        }

        ProfileContent(
            photoUrl = photoUrl,
            initials = EntrantUtils.initials(name),
            hasProfileImage = !profileImageId.isNullOrBlank(),
            isPhotoBusy = isPhotoBusy,
            onChangePhotoClick = {
                message = null
                errorMessage = null
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemovePhotoClick = {
                message = null
                errorMessage = null
                showRemovePhotoDialog = true
            },
            showUploadPhotoDialog = pendingPhotoUri != null,
            onConfirmUploadPhoto = {
                val uri = pendingPhotoUri ?: return@ProfileContent
                pendingPhotoUri = null
                isPhotoBusy = true
                scope.launch {
                    val file = try {
                        withContext(Dispatchers.IO) {
                            ProfileImageUtils.downscaleToJpeg(context, uri)
                        }
                    } catch (e: Exception) {
                        Log.w("ProfileScreen", "Could not read picked photo.", e)
                        isPhotoBusy = false
                        errorMessage = "Could not read that photo. Try a different one."
                        return@launch
                    }
                    imageRepository.uploadImage(
                        uri = Uri.fromFile(file),
                        targetType = "profile",
                        targetId = user.id,
                        onSuccess = { newId ->
                            file.delete()
                            profileImageId = newId
                            isPhotoBusy = false
                            message = "Profile photo updated"
                        },
                        onFailure = {
                            file.delete()
                            isPhotoBusy = false
                            errorMessage = it.message ?: "Could not upload your photo."
                        }
                    )
                }
            },
            onCancelUploadPhoto = { pendingPhotoUri = null },
            showRemovePhotoDialog = showRemovePhotoDialog,
            onConfirmRemovePhoto = {
                val imageId = profileImageId
                showRemovePhotoDialog = false
                if (imageId.isNullOrBlank()) return@ProfileContent
                isPhotoBusy = true
                imageRepository.deleteImage(
                    imageId = imageId,
                    onSuccess = {
                        profileImageId = null
                        isPhotoBusy = false
                        message = "Profile photo removed"
                    },
                    onFailure = {
                        isPhotoBusy = false
                        errorMessage = it.message ?: "Could not remove your photo."
                    }
                )
            },
            onCancelRemovePhoto = { showRemovePhotoDialog = false },
            name = name,
            onNameChange = { name = it; message = null },
            email = email,
            onEmailChange = { email = it; message = null },
            phone = phone,
            onPhoneChange = { phone = it; message = null },
            isSaving = isSaving,
            message = message,
            errorMessage = errorMessage,
            onSaveClick = {
                isSaving = true
                errorMessage = null
                userRepository.updateProfile(
                    name = name,
                    email = email,
                    phone = phone,
                    onSuccess = {
                        isSaving = false
                        message = "Profile saved"
                    },
                    onFailure = {
                        isSaving = false
                        errorMessage = it.message
                    }
                )
            },
            notificationsEnabled = notificationsEnabled,
            notificationsToggleEnabled = !isSavingNotifications,
            onNotificationsToggle = { enabled ->
                val previous = notificationsEnabled
                // Flip the switch right away, and undo it if the save fails
                notificationsEnabled = enabled
                isSavingNotifications = true
                message = null
                errorMessage = null
                userRepository.updateNotificationPreference(
                    enabled = enabled,
                    onSuccess = {
                        isSavingNotifications = false
                        if (enabled) {
                            message = "Notifications turned on"
                        } else {
                            showNotificationsOffDialog = true
                        }
                    },
                    onFailure = {
                        isSavingNotifications = false
                        notificationsEnabled = previous
                        errorMessage = it.message ?: "Could not update notifications."
                    }
                )
            },
            showNotificationsOffDialog = showNotificationsOffDialog,
            onDismissNotificationsOffDialog = { showNotificationsOffDialog = false },
            onPolicyClick = onPolicyClick,
            showDeleteDialog = showDeleteDialog,
            isDeleting = isDeleting,
            onDeleteClick = {
                message = null
                errorMessage = null
                showDeleteDialog = true
            },
            onDeleteConfirmed = {
                isDeleting = true
                deleteProfile(
                    // The current photo, which may have changed since the screen opened
                    profileImageId = profileImageId,
                    userRepository = userRepository,
                    imageRepository = imageRepository,
                    onSuccess = {
                        Toast.makeText(context, "Your profile was deleted", Toast.LENGTH_SHORT).show()
                        onProfileDeleted()
                    },
                    onFailure = {
                        isDeleting = false
                        showDeleteDialog = false
                        errorMessage = it.message ?: "Could not delete your profile."
                    }
                )
            },
            onDeleteDismissed = { showDeleteDialog = false },
            onBackClick = onBackClick
        )
    }
}

/**
 * Deletes the signed-in user's profile image (if they have one) and then their profile.
 * The image goes first, while the user is still signed in and allowed to delete it.
 * If only the image fails, the profile is still deleted so the user isn't stuck.
 * US 01.08.01.
 *
 * @param profileImageId the user's profile image id, or null if they have none
 * @param userRepository used to delete the profile and sign out
 * @param imageRepository used to delete the profile image
 * @param onSuccess is invoked once the profile is deleted
 * @param onFailure is invoked if the profile could not be deleted
 */
private fun deleteProfile(
    profileImageId: String?,
    userRepository: UserRepository,
    imageRepository: ImageRepository,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val deleteUser = {
        userRepository.deleteCurrentProfile(onSuccess = onSuccess, onFailure = onFailure)
    }

    if (profileImageId.isNullOrBlank()) {
        deleteUser()
        return
    }

    imageRepository.deleteImage(
        imageId = profileImageId,
        onSuccess = { deleteUser() },
        onFailure = { error ->
            Log.w("ProfileScreen", "Could not delete profile image before deleting profile.", error)
            deleteUser()
        }
    )
}

/**
 * The profile screen's UI with no Firebase access, so it can be tested on its own.
 * US 01.07.03, US 01.08.01, US 01.08.02, US 01.10.01, US 01.11.01.
 *
 * @param photoUrl download URL of the profile photo, or null to show the placeholder
 * @param initials shown in the placeholder; if blank, a person icon is shown instead
 * @param hasProfileImage true if the user has a saved photo, which switches
 * "Add photo" to "Change photo" and shows "Remove photo"
 * @param isPhotoBusy true while a photo is uploading or being removed
 * @param onChangePhotoClick is invoked when the user clicks Add photo or Change photo
 * @param onRemovePhotoClick is invoked when the user clicks Remove photo
 * @param showUploadPhotoDialog true to show the "Upload profile image?" dialog
 * @param onConfirmUploadPhoto is invoked when the user clicks Upload in that dialog
 * @param onCancelUploadPhoto is invoked when the user cancels or closes that dialog
 * @param showRemovePhotoDialog true to show the "Remove profile photo?" dialog
 * @param onConfirmRemovePhoto is invoked when the user clicks Remove in that dialog
 * @param onCancelRemovePhoto is invoked when the user cancels or closes that dialog
 * @param name the name currently in the name field
 * @param onNameChange is invoked when the user edits the name
 * @param email the email currently in the email field
 * @param onEmailChange is invoked when the user edits the email
 * @param phone the phone number currently in the phone field
 * @param onPhoneChange is invoked when the user edits the phone number
 * @param isSaving true while the profile fields are being saved
 * @param message a success message to show, or null
 * @param errorMessage an error message to show, or null
 * @param onSaveClick is invoked when the user clicks Save
 * @param notificationsEnabled whether the user currently receives notifications
 * @param notificationsToggleEnabled false while the notification preference is being saved
 * @param onNotificationsToggle is invoked with the new value when the user flips the switch
 * @param showNotificationsOffDialog true to show the "notifications turned off" dialog
 * @param onDismissNotificationsOffDialog is invoked when the user closes that dialog
 * @param onPolicyClick is invoked when the user clicks App policy and guidelines
 * @param showDeleteDialog true to show the "Delete profile?" confirmation dialog
 * @param isDeleting true while the profile is being deleted
 * @param onDeleteClick is invoked when the user clicks Delete profile
 * @param onDeleteConfirmed is invoked when the user confirms the deletion in the dialog
 * @param onDeleteDismissed is invoked when the user cancels or closes the dialog
 * @param onBackClick is invoked when the user clicks the back button
 */
@Composable
internal fun ProfileContent(
    photoUrl: String?,
    initials: String,
    hasProfileImage: Boolean,
    isPhotoBusy: Boolean,
    onChangePhotoClick: () -> Unit,
    onRemovePhotoClick: () -> Unit,
    showUploadPhotoDialog: Boolean,
    onConfirmUploadPhoto: () -> Unit,
    onCancelUploadPhoto: () -> Unit,
    showRemovePhotoDialog: Boolean,
    onConfirmRemovePhoto: () -> Unit,
    onCancelRemovePhoto: () -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    isSaving: Boolean,
    message: String?,
    errorMessage: String?,
    onSaveClick: () -> Unit,
    notificationsEnabled: Boolean,
    notificationsToggleEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    showNotificationsOffDialog: Boolean,
    onDismissNotificationsOffDialog: () -> Unit,
    onPolicyClick: () -> Unit,
    showDeleteDialog: Boolean,
    isDeleting: Boolean,
    onDeleteClick: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit,
    onBackClick: () -> Unit
) {
    val canSave = name.isNotBlank() && !isSaving

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBackClick) { Text("Back") }
        Text("My profile", style = MaterialTheme.typography.headlineMedium)

        ProfileAvatar(
            photoUrl = photoUrl,
            initials = initials,
            isBusy = isPhotoBusy,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = onChangePhotoClick, enabled = !isPhotoBusy) {
                Text(if (hasProfileImage) "Change photo" else "Add photo")
            }
            if (hasProfileImage) {
                TextButton(
                    onClick = onRemovePhotoClick,
                    enabled = !isPhotoBusy,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Remove photo") }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Phone (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onSaveClick,
            enabled = canSave,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (isSaving) "Saving…" else "Save") }

        // The whole row is the toggle so the label is clickable too, and screen
        // readers announce it as one switch with a label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = notificationsEnabled,
                    enabled = notificationsToggleEnabled,
                    role = Role.Switch,
                    onValueChange = onNotificationsToggle
                )
                .padding(vertical = 8.dp)
                .testTag(NOTIFICATIONS_SWITCH_TAG)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Push Notifications", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Lottery results and reservation updates",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = notificationsEnabled,
                // Clicks are handled by the row above
                onCheckedChange = null,
                enabled = notificationsToggleEnabled
            )
        }

        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        // Policy link sits at the bottom, like the legal links in most apps' account screens
        HorizontalDivider()
        TextButton(onClick = onPolicyClick) { Text("App policy and guidelines") }

        // Destructive action goes last, in the error colour, so it's hard to hit by accident
        Button(
            onClick = onDeleteClick,
            enabled = !isDeleting,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Delete profile") }
    }

    if (showUploadPhotoDialog) {
        AlertDialog(
            onDismissRequest = onCancelUploadPhoto,
            title = { Text("Upload profile image?") },
            text = {
                Text("This photo will replace your current profile photo, if you have one.")
            },
            confirmButton = {
                TextButton(onClick = onConfirmUploadPhoto) { Text("Upload") }
            },
            dismissButton = {
                TextButton(onClick = onCancelUploadPhoto) { Text("Cancel") }
            }
        )
    }

    if (showRemovePhotoDialog) {
        AlertDialog(
            onDismissRequest = onCancelRemovePhoto,
            title = { Text("Remove profile photo?") },
            text = { Text("Your photo will be deleted and your initials shown instead.") },
            confirmButton = {
                TextButton(
                    onClick = onConfirmRemovePhoto,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = onCancelRemovePhoto) { Text("Cancel") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            // Can't be dismissed by tapping outside while the delete is running
            onDismissRequest = { if (!isDeleting) onDeleteDismissed() },
            title = { Text("Delete profile?") },
            text = {
                Text(
                    "This permanently deletes your name, contact details and profile " +
                        "image. This can't be undone.\n\n" +
                        "Upcoming reservations aren't cancelled automatically, so cancel " +
                        "any you won't use before deleting your profile."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onDeleteConfirmed,
                    enabled = !isDeleting,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(if (isDeleting) "Deleting…" else "Delete") }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismissed, enabled = !isDeleting) { Text("Cancel") }
            }
        )
    }

    if (showNotificationsOffDialog) {
        AlertDialog(
            onDismissRequest = onDismissNotificationsOffDialog,
            title = { Text("Push notifications turned off") },
            text = {
                Text(
                    "You won't receive push notifications about lottery results or " +
                        "reservations. You can turn them back on anytime from My Profile."
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissNotificationsOffDialog) { Text("OK") }
            }
        )
    }
}

/**
 * The round profile photo at the top of the profile screen. Shows the photo cropped
 * to a circle, or a placeholder with the user's initials (or a person icon if there
 * are none). The placeholder also shows while the photo is downloading.
 * A spinner sits on top while a photo is uploading or being removed.
 * US 01.08.02.
 *
 * @param photoUrl download URL of the photo, or null for the placeholder
 * @param initials shown in the placeholder
 * @param isBusy true to show the loading spinner over the circle
 * @param modifier used to position the avatar
 */
@Composable
private fun ProfileAvatar(
    photoUrl: String?,
    initials: String,
    isBusy: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(AVATAR_SIZE)
            .clip(CircleShape)
            .testTag(PROFILE_AVATAR_TAG)
            .semantics(mergeDescendants = true) {
                contentDescription = if (photoUrl != null) "Profile photo" else "No profile photo"
            }
    ) {
        // A light tint of the theme green (the theme doesn't set primaryContainer,
        // so that would fall back to the default purple)
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(AVATAR_SIZE)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (initials.isNotBlank()) {
                    Text(initials, style = MaterialTheme.typography.headlineMedium)
                } else {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
        // Drawn over the placeholder, which shows through until the download finishes
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(AVATAR_SIZE)
            )
        }
        if (isBusy) {
            CircularProgressIndicator()
        }
    }
}
