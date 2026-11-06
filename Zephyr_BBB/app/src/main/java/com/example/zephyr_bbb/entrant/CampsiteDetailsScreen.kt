package com.example.zephyr_bbb.entrant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.Lottery

// Entrant campsite details screen (01.01.05) with the open lottery of the campsite and the
// buttons to join it and leave its waiting list (01.01.06, 01.02.01, 01.02.02)
// Outstanding issues: a campsite with no open lottery has no waiting list to join, because the
// database only has waiting lists that belong to a lottery. Only one lottery is shown.
// This meets the user story criteria without implementing the waitlist to get into a full lottery
// we illustrated in our mockups

/**
 * Composable function that creates the details screen for one campsite
 * It loads the campsite when it is opened and passes what was loaded to [CampsiteDetailsContent].
 *
 * @param campsiteId the ID of the campsite to show
 * @param viewModel holds the campsite, its open lottery, and the entrant's entry in it
 * @param onBackClick is invoked when the user clicks the back button
 */
@Composable
fun CampsiteDetailsScreen(
    campsiteId: String,
    viewModel: CampsiteDetailsViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(campsiteId) {
        viewModel.load(campsiteId)
    }

    CampsiteDetailsContent(
        campsite = viewModel.campsite,
        lottery = viewModel.lottery,
        joinAction = viewModel.joinAction,
        isLoading = viewModel.isLoading,
        isWorking = viewModel.isWorking,
        notFound = viewModel.notFound,
        loadError = viewModel.loadError,
        actionError = viewModel.actionError,
        onBackClick = onBackClick,
        onRetryClick = { viewModel.load(campsiteId) },
        onJoinConfirmed = { viewModel.joinLottery() },
        onLeaveConfirmed = { viewModel.leaveLottery() }
    )
}

/**
 * Composable function that displays the details of a campsite and its lottery
 *
 * @param campsite the campsite to show, or null while it is loading or if it was not found
 * @param lottery the open lottery of the campsite, or null if it has none
 * @param joinAction which lottery button to show
 * @param isLoading true while the campsite is loading
 * @param isWorking true while a join or leave request is running, which disables the buttons
 * @param notFound true when no campsite exists with the requested ID
 * @param loadError message to show if the campsite could not be loaded
 * @param actionError message to show if joining or leaving failed
 * @param onBackClick is invoked when the user clicks the back button
 * @param onRetryClick is invoked when the user clicks retry after a load error
 * @param onJoinConfirmed is invoked when the user confirms joining the lottery
 * @param onLeaveConfirmed is invoked when the user confirms leaving the waiting list
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CampsiteDetailsContent(
    campsite: Campsite?,
    lottery: Lottery?,
    joinAction: JoinAction,
    isLoading: Boolean,
    isWorking: Boolean,
    notFound: Boolean,
    loadError: String?,
    actionError: String?,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onJoinConfirmed: () -> Unit,
    onLeaveConfirmed: () -> Unit
) {
    var showJoinDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    if (showJoinDialog) {
        val closes = registrationText(
            lottery?.registrationOpensAt?.toDate()?.time,
            lottery?.registrationClosesAt?.toDate()?.time
        )
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join lottery?") },
            text = {
                Text(
                    "You will be added to the waiting list for ${campsite?.name ?: "this campsite"}. " +
                        "Winners are drawn after registration closes. $closes"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showJoinDialog = false
                        onJoinConfirmed()
                    }
                ) {
                    Text("Join")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave waiting list?") },
            text = { Text("You will lose your place in this lottery. You can join again while registration is open.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        onLeaveConfirmed()
                    }
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Stay")
                }
            }
        )
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
                text = "Campsite Details",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        when {
            isLoading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Loading campsite...")
                }
            }

            loadError != null -> {
                Text(text = loadError, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onRetryClick) {
                    Text("Retry")
                }
            }

            notFound || campsite == null -> {
                Text("This campsite could not be found.")
            }

            else -> {
                Text(text = campsite.name, style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = campsite.locationName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Max ${campsite.maxOccupancy} guests · ${campsite.ratingSummary()}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = priceText(campsite.pricePerNightCents, campsite.currency),
                    style = MaterialTheme.typography.titleMedium
                )

                if (campsite.description.isNotBlank()) {
                    Text(campsite.description)
                }

                if (campsite.tags.isNotEmpty()) {
                    Text("Amenities", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        campsite.tags.forEach { tag ->
                            SuggestionChip(onClick = {}, label = { Text(tag) })
                        }
                    }
                }

                if (campsite.rules.isNotBlank()) {
                    Text("Rules", style = MaterialTheme.typography.titleSmall)
                    Text(campsite.rules)
                }

                if (campsite.cancellationPolicy.isNotBlank()) {
                    Text("Cancellation policy", style = MaterialTheme.typography.titleSmall)
                    Text(campsite.cancellationPolicy)
                }

                LotteryCard(
                    lottery = lottery,
                    joinAction = joinAction,
                    isWorking = isWorking,
                    actionError = actionError,
                    onJoinClick = { showJoinDialog = true },
                    onLeaveClick = { showLeaveDialog = true }
                )
            }
        }
    }
}

// The card with the open lottery of the campsite and the button to join or leave it
@Composable
private fun LotteryCard(
    lottery: Lottery?,
    joinAction: JoinAction,
    isWorking: Boolean,
    actionError: String?,
    onJoinClick: () -> Unit,
    onLeaveClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Lottery", style = MaterialTheme.typography.titleMedium)

            if (lottery == null) {
                Text("No active lottery for this campsite right now.")
            } else {
                if (lottery.title.isNotBlank()) {
                    Text(lottery.title, style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    registrationText(
                        lottery.registrationOpensAt?.toDate()?.time,
                        lottery.registrationClosesAt?.toDate()?.time
                    )
                )
                val stay = stayText(lottery.stayStartDate, lottery.stayEndDate)
                if (stay.isNotEmpty()) {
                    Text(stay)
                }
                Text(winnersText(lottery.winnerCount))
                Text(entrantsText(lottery.entryCount))

                when (joinAction) {
                    JoinAction.JOIN -> {
                        Button(
                            onClick = onJoinClick,
                            enabled = !isWorking,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Join Lottery")
                        }
                    }

                    JoinAction.LEAVE -> {
                        Text(
                            text = "You are on the waiting list.",
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = onLeaveClick,
                            enabled = !isWorking,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Leave Waiting List")
                        }
                    }

                    JoinAction.FULL -> {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Waiting list full")
                        }
                    }

                    JoinAction.NONE -> {
                        Text("You already have a result in this lottery.")
                    }
                }
            }

            actionError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
