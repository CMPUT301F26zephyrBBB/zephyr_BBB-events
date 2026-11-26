package com.example.zephyr_bbb.entrant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.repository.UserRepository

/** Test tag for the push notifications switch row on the profile screen. */
internal const val NOTIFICATIONS_SWITCH_TAG = "notificationsSwitch"

/**
 * Lets the entrant view and edit their profile (name, email, phone) and
 * opt out of receiving notifications. US 01.07.03, US 01.10.01.
 *
 * This is the stateful wrapper: it loads the signed-in user, talks to
 * [UserRepository], and passes plain values down to [ProfileContent].
 *
 * @param onBackClick is invoked when the user clicks the back button
 */
@Composable
fun ProfileScreen(onBackClick: () -> Unit) {
    DeviceIdentityGate { user ->
        val userRepository = remember { UserRepository() }

        // Start the fields with what's already saved for this user
        var name by remember(user.id) { mutableStateOf(user.name) }
        var email by remember(user.id) { mutableStateOf(user.email) }
        var phone by remember(user.id) { mutableStateOf(user.phone) }
        var notificationsEnabled by remember(user.id) { mutableStateOf(user.notificationsEnabled) }

        var isSaving by remember { mutableStateOf(false) }
        var isSavingNotifications by remember { mutableStateOf(false) }
        var showNotificationsOffDialog by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        ProfileContent(
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
            onBackClick = onBackClick
        )
    }
}

/**
 * The profile screen's UI with no Firebase access, so it can be tested on its own.
 * US 01.07.03, US 01.10.01.
 *
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
 * @param onBackClick is invoked when the user clicks the back button
 */
@Composable
internal fun ProfileContent(
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
