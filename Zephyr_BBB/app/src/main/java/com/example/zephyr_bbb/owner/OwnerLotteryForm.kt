package com.example.zephyr_bbb.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.LaunchedEffect
import java.util.Date
import java.util.TimeZone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.Lottery
import com.google.firebase.Timestamp

/**
 * A composable function that shows the Create Lottery form.
 *  It does not load or save anything itself; the screen that uses it passes in the current
 *  Values and is told when they change.
 *  The owner enters the lottery name and the stay period.
 *  The form is checked with LotteryFormFields.validate() before onSubmit is called.
 *  The owner can set the registration period.
 *  The owner can pick one or more campsites.
 *  The owner can set the number of winners.
 *  It is also used by OwnerEditLotteryScreen, which fills it with an existing lottery.
 *
 *  @param heading The text shown beside the back button
 *  @param fields The values currently in the form
 *  @param submitLabel The text on the submit button
 *  @param errorMessage An error from loading or saving, shown above the submit button
 *  @param isSubmitting True while saving, which disables the submit button
 *  @param onFieldsChange is invoked with the new values whenever the owner changes the form
 *  @param onSubmit is invoked when the owner presses the submit button and the form is valid
 *  @param onBackClick is invoked when the user clicks the back button
 *  @param campsites The owner's campsites that can be included in the lottery
 */
@Composable
fun LotteryForm(
    heading: String,
    fields: LotteryFormFields,
    campsites: List<Campsite>,
    submitLabel: String,
    errorMessage: String?,
    isSubmitting: Boolean,
    onFieldsChange: (LotteryFormFields) -> Unit,
    onSubmit: () -> Unit,
    onBackClick: () -> Unit
) {
    var validationError by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Back button and text at the top of the screen
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onBackClick
            ) {
                Text("<")
            }

            Text(heading)
        }
        OutlinedTextField(
            value = fields.title,
            onValueChange = { onFieldsChange(fields.copy(title = it)) },
            label = { Text("Lottery name") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Campsites · select one or more")

        if (campsites.isEmpty()) {
            Text("Add a campsite before creating a lottery.")
        }

        campsites.forEach { campsite ->
            val checked = campsite.id in fields.campsiteIds

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = checked,
                        role = Role.Checkbox,
                        onValueChange = { isChecked ->
                            val newIds = if (isChecked) {
                                fields.campsiteIds + campsite.id
                            } else {
                                fields.campsiteIds - campsite.id
                            }
                            onFieldsChange(fields.copy(campsiteIds = newIds))
                        }
                    )
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = null
                )
                Text(campsiteLabel(campsite))
            }
        }

        DateFieldRow(
            startLabel = "Registration start",
            startValue = fields.registrationStart,
            onStartChange = { onFieldsChange(fields.copy(registrationStart = it)) },
            endLabel = "Registration end",
            endValue = fields.registrationEnd,
            onEndChange = { onFieldsChange(fields.copy(registrationEnd = it)) }
        )

        DateFieldRow(
            startLabel = "Booking start",
            startValue = fields.bookingStart,
            onStartChange = { onFieldsChange(fields.copy(bookingStart = it)) },
            endLabel = "Booking end",
            endValue = fields.bookingEnd,
            onEndChange = { onFieldsChange(fields.copy(bookingEnd = it)) }
        )

        DateFieldRow(
            startLabel = "Stay start",
            startValue = fields.stayStart,
            onStartChange = { onFieldsChange(fields.copy(stayStart = it)) },
            endLabel = "Stay end (checkout)",
            endValue = fields.stayEnd,
            onEndChange = { onFieldsChange(fields.copy(stayEnd = it)) }
        )

        OutlinedTextField(
            value = fields.winnerCount,
            onValueChange = { input ->
                if (input.all { it.isDigit() }) {
                    onFieldsChange(fields.copy(winnerCount = input))
                }
            },
            label = { Text("Winner count") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = fields.waitlistCapacity,
            onValueChange = { input ->
                if (input.all { it.isDigit() }) {
                    onFieldsChange(fields.copy(waitlistCapacity = input))
                }
            },
            label = { Text("Waitlist capacity (optional)") },
            supportingText = { Text("Leave blank for an unlimited waitlist.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        val shownError = validationError ?: errorMessage

        if (!shownError.isNullOrBlank()) {
            Text(
                text = shownError,
                color = MaterialTheme.colorScheme.error
            )
        }

        // Checks the form, then hands it to the screen to save
        Button(
            onClick = {
                validationError = fields.validate()

                if (validationError == null) {
                    onSubmit()
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(submitLabel)
        }
    }
}

/**
 * Two date input fields side by side, used for each start/end period on the lottery form.
 */
@Composable
private fun DateFieldRow(
    startLabel: String,
    startValue: String,
    onStartChange: (String) -> Unit,
    endLabel: String,
    endValue: String,
    onEndChange: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        DatePickerField(
            label = startLabel,
            value = startValue,
            onValueChange = onStartChange,
            modifier = Modifier.weight(1f)
        )
        DatePickerField(
            label = endLabel,
            value = endValue,
            onValueChange = onEndChange,
            modifier = Modifier.weight(1f)
        )
    }
}
/**
 * A read-only date field that opens a calendar date picker when tapped.
 * The chosen day is reported as YYYY-MM-DD.
 *
 * @param label The text shown on the field
 * @param value The current date, YYYY-MM-DD, or blank
 * @param onValueChange is invoked with the picked date as YYYY-MM-DD
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    // A read-only text field swallows clicks, so watch its presses instead
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                showPicker = true
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("Select date") },
        singleLine = true,
        interactionSource = interactionSource,
        modifier = modifier
    )

    if (showPicker) {
        // The date picker works in UTC milliseconds
        val utc = TimeZone.getTimeZone("UTC")
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseLotteryDate(value, utc)?.time
                ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onValueChange(formatLotteryDate(Date(millis), utc))
                        }
                        showPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/**
 * Saves LotteryFormFields across screen rotation and process death, for use with rememberSaveable.
 */
val LotteryFormFieldsSaver: Saver<LotteryFormFields, Any> = listSaver(
    save = { f ->
        listOf(
            f.title, ArrayList(f.campsiteIds), f.registrationStart, f.registrationEnd,
            f.bookingStart, f.bookingEnd, f.stayStart, f.stayEnd, f.winnerCount, f.waitlistCapacity
        )
    },
    restore = { v ->
        @Suppress("UNCHECKED_CAST")
        LotteryFormFields(
            title = v[0] as String,
            campsiteIds = (v[1] as List<String>).toSet(),
            registrationStart = v[2] as String,
            registrationEnd = v[3] as String,
            bookingStart = v[4] as String,
            bookingEnd = v[5] as String,
            stayStart = v[6] as String,
            stayEnd = v[7] as String,
            winnerCount = v[8] as String,
            waitlistCapacity = v[9] as String
        )
    }
)

/**
 * The text shown for a campsite in lottery lists, e.g. "Pine Creek A12 · Jasper".
 */
fun campsiteLabel(campsite: Campsite): String {
    return if (campsite.locationName.isBlank()) {
        campsite.name
    } else {
        "${campsite.name} · ${campsite.locationName}"
    }
}

/**
* Builds the Lottery that is sent to LotteryRepository from the form values.
* Only call this after validate() returns null.
*
* @param id The existing lottery ID when editing, blank when creating
*/
fun LotteryFormFields.toLottery(id: String = ""): Lottery {
    return Lottery(
        id = id,
        title = title.trim(),
        stayStartDate = stayStart.trim(),
        stayEndDate = stayEnd.trim(),
        campsiteIds = campsiteIds.toList(),
        registrationOpensAt = parseLotteryDate(registrationStart)?.let { Timestamp(it) },
        registrationClosesAt = parseLotteryEndDate(registrationEnd)?.let { Timestamp(it) },
        bookingOpensAt = parseLotteryDate(bookingStart)?.let { Timestamp(it) },
        bookingClosesAt = parseLotteryEndDate(bookingEnd)?.let { Timestamp(it) },
        winnerCount = winnerCount.toLongOrNull() ?: 1L,
        waitlistCapacity = waitlistCapacityOrNull()
    )
}

/**
 * Fills the form with an existing lottery so the owner can edit it.
 */
fun Lottery.toFormFields(): LotteryFormFields {
    return LotteryFormFields(
        title = title,
        campsiteIds = campsiteIds.toSet(),
        registrationStart = formatLotteryDate(registrationOpensAt?.toDate()),
        registrationEnd = formatLotteryEndDate(registrationClosesAt?.toDate()),
        bookingStart = formatLotteryDate(bookingOpensAt?.toDate()),
        bookingEnd = formatLotteryEndDate(bookingClosesAt?.toDate()),
        stayStart = stayStartDate,
        stayEnd = stayEndDate,
        winnerCount = winnerCount.toString(),
        waitlistCapacity = waitlistCapacity?.toString() ?: ""
    )
}