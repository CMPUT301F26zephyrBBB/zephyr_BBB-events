package com.example.zephyr_bbb.entrant



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.database.model.LotteryEntry
import com.example.zephyr_bbb.database.repository.LotteryRepository

/**
 * "You won" screen (US 01.03.01). The entry status decides what shows,
 * not the notification, because winners with notifications off get none.
 * Reserve goes to date selection (US 01.04.01); Decline gives up the spot (US 01.04.03).
 */
@Composable
fun WinScreen(
    lotteryId: String,
    onBackClick: () -> Unit,
    onReserveClick: (lotteryId: String) -> Unit
) {
    DeviceIdentityGate {
        val lotteryRepository = remember { LotteryRepository() }
        var lottery by remember { mutableStateOf<Lottery?>(null) }
        var entry by remember { mutableStateOf<LotteryEntry?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var showDeclineDialog by remember { mutableStateOf(false) }
        var isDeclining by remember { mutableStateOf(false) }

        fun loadEntry() {
            lotteryRepository.getMyEntry(
                lotteryId,
                onSuccess = { entry = it; isLoading = false },
                onFailure = { errorMessage = it.message; isLoading = false }
            )
        }

        LaunchedEffect(lotteryId) {
            lotteryRepository.getLottery(
                lotteryId,
                onSuccess = { lottery = it },
                onFailure = { errorMessage = it.message }
            )
            loadEntry()
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBackClick) { Text("Back") }

            val title = lottery?.title ?: ""

            when {
                isLoading -> Text("Loading…")

                entry?.status == "selected" -> {
                    Text("You won!", style = MaterialTheme.typography.headlineMedium)
                    Text("You were selected in the lottery for $title.")
                    lottery?.let {
                        Text("Stay period: ${it.stayStartDate} to ${it.stayEndDate}")
                    }
                    Button(
                        onClick = { onReserveClick(lotteryId) },
                        enabled = !isDeclining,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Reserve my stay") }
                    OutlinedButton(
                        onClick = { showDeclineDialog = true },
                        enabled = !isDeclining,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Decline") }
                }

                entry?.status == "booked" -> Text("You already reserved a stay for $title.")
                entry?.status == "declined" -> Text("You declined your spot for $title.")
                entry?.status == "not_selected" -> Text("You were not selected for $title.")
                entry == null -> Text("You are not entered in this lottery.")
                else -> Text("The draw for $title has not happened yet.")
            }

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        if (showDeclineDialog) {
            AlertDialog(
                onDismissRequest = { showDeclineDialog = false },
                title = { Text("Decline your spot?") },
                text = { Text("You won't be able to reserve a stay from this lottery.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeclineDialog = false
                        isDeclining = true
                        lotteryRepository.decline(
                            lotteryId,
                            onSuccess = { isDeclining = false; loadEntry() },
                            onFailure = { errorMessage = it.message; isDeclining = false }
                        )
                    }) { Text("Decline") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeclineDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}