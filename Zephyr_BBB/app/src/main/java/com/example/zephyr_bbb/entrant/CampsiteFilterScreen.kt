package com.example.zephyr_bbb.entrant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// Filter menu for the entrant search (US 01.01.02): location, dates, number of guests and
// amenities.
// Outstanding issues: the amenities are typed as free text, there is no list to pick from.

private const val MAX_GUESTS = 50

// Only lets today and later days be picked in the date range picker
@OptIn(ExperimentalMaterial3Api::class)
private class FutureDates(private val firstDayMillis: Long) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= firstDayMillis
}

/**
 * Composable function that creates the screen where entrants choose search filters.
 * The values are held on this screen until the user clicks Apply Filters, which saves them in
 * the view model and runs the search.
 *
 * @param viewModel holds the filters and results, shared with the search screen
 * @param onBackClick is invoked when the user clicks the back button
 * @param onApplyClick is invoked after the filters are applied and the search has started
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampsiteFilterScreen(
    viewModel: CampsiteSearchViewModel,
    onBackClick: () -> Unit,
    onApplyClick: () -> Unit
) {
    val current = viewModel.filters
    var location by remember { mutableStateOf(current.location) }
    var amenities by remember { mutableStateOf(current.amenities) }
    var guests by remember { mutableStateOf(current.guests.toInt().coerceIn(1, MAX_GUESTS)) }
    var checkIn by remember { mutableStateOf(current.checkInDate) }
    var checkOut by remember { mutableStateOf(current.checkOutDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = checkIn?.let { isoDateToMillis(it) },
            initialSelectedEndDateMillis = checkOut?.let { isoDateToMillis(it) },
            selectableDates = remember { FutureDates(todayUtcMillis()) }
        )
        val start = pickerState.selectedStartDateMillis
        val end = pickerState.selectedEndDateMillis

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                // A stay needs a check-in and a later check-out
                TextButton(
                    enabled = start != null && end != null && end > start,
                    onClick = {
                        checkIn = start?.let { millisToIsoDate(it) }
                        checkOut = end?.let { millisToIsoDate(it) }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = pickerState,
                modifier = Modifier.height(500.dp)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = onBackClick) {
                Text("Back")
            }
            Text(
                text = "Filter Campsites",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = amenities,
            onValueChange = { amenities = it },
            label = { Text("Amenities (comma separated)") },
            supportingText = { Text("For example: fire pit, potable water") },
            modifier = Modifier.fillMaxWidth()
        )

        // Check-in and check-out are chosen together in one date range picker
        Text("Dates", style = MaterialTheme.typography.labelLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                val hasDates = checkIn != null && checkOut != null
                Text(if (hasDates) "$checkIn to $checkOut" else "Select dates")
            }
            if (checkIn != null || checkOut != null) {
                TextButton(
                    onClick = {
                        checkIn = null
                        checkOut = null
                    }
                ) {
                    Text("Clear dates")
                }
            }
        }

        // Number of guests, changed with the minus and plus buttons so it is always a valid number
        Text("Number of guests", style = MaterialTheme.typography.labelLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { guests-- },
                enabled = guests > 1,
                modifier = Modifier.semantics { contentDescription = "Fewer guests" }
            ) {
                Text("-")
            }
            Text(
                text = guests.toString(),
                style = MaterialTheme.typography.titleLarge
            )
            OutlinedButton(
                onClick = { guests++ },
                enabled = guests < MAX_GUESTS,
                modifier = Modifier.semantics { contentDescription = "More guests" }
            ) {
                Text("+")
            }
        }

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                val newFilters = viewModel.filters.copy(
                    location = location,
                    amenities = amenities,
                    guests = guests.toLong(),
                    checkInDate = checkIn,
                    checkOutDate = checkOut
                )
                val problem = newFilters.validate()
                if (problem != null) {
                    errorMessage = problem
                } else {
                    viewModel.updateFilters(newFilters)
                    viewModel.search()
                    onApplyClick()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply Filters")
        }

        OutlinedButton(
            onClick = {
                location = ""
                amenities = ""
                guests = 1
                checkIn = null
                checkOut = null
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Filters")
        }
    }
}
