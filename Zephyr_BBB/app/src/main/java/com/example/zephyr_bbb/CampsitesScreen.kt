package com.example.zephyr_bbb

import androidx.compose.runtime.Composable
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.entrant.CampsiteSearchScreen
import com.example.zephyr_bbb.entrant.CampsiteSearchViewModel

// Entry point for the Campsites tab of the bottom navigation bar
// It only forwards to the entrant search screen, which lives in the entrant package.

/**
 * Composable function that displays the Campsites tab.
 *
 * @param viewModel holds the filters and results, shared with the filter screen
 * @param onFilterClick is invoked when the user clicks the filter button
 * @param onCampsiteClick is invoked when the user clicks view details on a campsite
 */
@Composable
fun CampsitesScreen(
    viewModel: CampsiteSearchViewModel,
    onFilterClick: () -> Unit,
    onCampsiteClick: (Campsite) -> Unit
) {
    CampsiteSearchScreen(
        viewModel = viewModel,
        onFilterClick = onFilterClick,
        onCampsiteClick = onCampsiteClick
    )
}
