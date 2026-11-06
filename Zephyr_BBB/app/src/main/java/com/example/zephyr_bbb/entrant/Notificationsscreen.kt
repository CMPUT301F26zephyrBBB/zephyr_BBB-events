package com.example.zephyr_bbb.entrant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.AppNotification
import com.example.zephyr_bbb.database.repository.NotificationRepository

/**
 * Live list of the entrant's notifications.
 * Tapping a "lottery_won" notification opens the win screen (US 01.03.01).
 * Tapping a reservation notification opens its confirmation.
 */
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    onWinClick: (lotteryId: String) -> Unit,
    onReservationClick: (reservationId: String) -> Unit
) {
    DeviceIdentityGate {
        val notificationRepository = remember { NotificationRepository() }
        var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Listen for changes while this screen is open, stop when it closes.
        DisposableEffect(Unit) {
            val listener = notificationRepository.observeMyNotifications(
                onSuccess = {
                    notifications = it.sortedByDescending { n -> n.createdAt }
                    isLoading = false
                },
                onFailure = {
                    errorMessage = it.message
                    isLoading = false
                }
            )
            onDispose { listener.remove() }
        }

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            TextButton(onClick = onBackClick) { Text("Back") }
            Text("Notifications", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> Text("Loading…")
                errorMessage != null -> Text(errorMessage ?: "")
                notifications.isEmpty() -> Text("No notifications yet.")
                else -> LazyColumn {
                    items(notifications, key = { it.id }) { notification ->
                        NotificationRow(
                            notification = notification,
                            onClick = {
                                val lotteryId = notification.lotteryId
                                val reservationId = notification.reservationId
                                if (notification.type == "lottery_won" && lotteryId != null) {
                                    onWinClick(lotteryId)
                                } else if (reservationId != null) {
                                    onReservationClick(reservationId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    val canOpen = (notification.type == "lottery_won" && notification.lotteryId != null) ||
            notification.reservationId != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = canOpen, onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(notification.title, style = MaterialTheme.typography.titleMedium)
            Text(notification.body)
            if (notification.type == "lottery_won") {
                Text("Tap to reserve or decline", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
