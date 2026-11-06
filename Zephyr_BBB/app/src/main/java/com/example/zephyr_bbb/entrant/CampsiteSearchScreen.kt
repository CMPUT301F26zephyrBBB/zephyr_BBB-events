package com.example.zephyr_bbb.entrant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.Campsite

// Entrant search screen: search bar, filter button and the list of matching campsites
// (01.01.01, 01.01.03 The actual filters are edited on CampsiteFilterScreen.
// Outstanding issues: the search only runs when the user clicks Search, presses search on the
// keyboard or applies filters, not while typing.

/**
 * Composable function that creates the screen where entrants search for campsites
 * Shows every campsite when the screen is first opened, and the matching campsites after a search
 * Each campsite is shown as a summary card.
 *
 * @param viewModel holds the filters and results, shared with the filter screen
 * @param onFilterClick invoked when the user clicks the filters button
 * @param onCampsiteClick invoked when the user clicks view details on a campsite
 */
@Composable
fun CampsiteSearchScreen(
    viewModel: CampsiteSearchViewModel,
    onFilterClick: () -> Unit,
    onCampsiteClick: (Campsite) -> Unit
) {
    // Show all campsites the first time the screen is opened, but keep the results when the
    // user comes back from the details screen
    LaunchedEffect(Unit) {
        viewModel.searchIfNeeded()
    }

    val filters = viewModel.filters
    val filterCount = filters.activeFilterCount()
    val summary = filters.summary()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Search Campsites",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp)
        )

        // Search bar and the button that opens the filter menu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = filters.searchText,
                onValueChange = { viewModel.updateFilters(filters.copy(searchText = it)) },
                label = { Text("Search campsites") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = onFilterClick) {
                Text(if (filterCount > 0) "Filters ($filterCount)" else "Filters")
            }
        }

        Button(
            onClick = { viewModel.search() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search")
        }

        // What is currently being searched for, with a way to reset it
        if (summary.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Filters: $summary",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        viewModel.clearFilters()
                        viewModel.search()
                    }
                ) {
                    Text("Clear all")
                }
            }
        }

        when {
            viewModel.isLoading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Searching...")
                }
            }

            viewModel.error != null -> {
                Text(
                    text = viewModel.error ?: "",
                    color = MaterialTheme.colorScheme.error
                )
            }

            viewModel.results.isEmpty() -> {
                Text("No campsites match your search. Try changing your filters.")
            }

            else -> {
                Text(
                    text = resultCountText(viewModel.results.size),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Scrollable list of the matching campsites
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(viewModel.results, key = { it.id }) { campsite ->
                val lotteryText = when {
                    !viewModel.activeLotteries.containsKey(campsite.id) -> "Checking lottery..."
                    viewModel.activeLotteries[campsite.id] == null -> "No active lottery"
                    else -> lotteryStatusText(
                        viewModel.activeLotteries[campsite.id]?.registrationClosesAt?.toDate()?.time
                    )
                }
                CampsiteSummaryCard(
                    campsite = campsite,
                    lotteryText = lotteryText,
                    onViewDetailsClick = { onCampsiteClick(campsite) }
                )
            }
        }
    }
}
