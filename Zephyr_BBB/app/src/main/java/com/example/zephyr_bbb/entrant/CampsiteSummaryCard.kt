package com.example.zephyr_bbb.entrant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.Campsite

// One card in the search results list (US 01.01.04)
// Outstanding issues: none known.

/**
 * Shows the summary information of a campsite: name, location, maximum occupancy, rating and
 * whether it has an open lottery.
 *
 * @param campsite the campsite to summarize
 * @param lotteryText line describing the campsite's lottery, for example "Lottery open · closes Oct 22"
 * @param onViewDetailsClick is invoked when the user clicks the view details button
 */
@Composable
fun CampsiteSummaryCard(
    campsite: Campsite,
    lotteryText: String,
    onViewDetailsClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = campsite.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = campsite.locationName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Max ${campsite.maxOccupancy} guests · ${campsite.ratingSummary()}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = lotteryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedButton(
                onClick = onViewDetailsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("View Details")
            }
        }
    }
}
