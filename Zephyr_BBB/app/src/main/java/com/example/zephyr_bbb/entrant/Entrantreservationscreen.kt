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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.Reservation
import com.example.zephyr_bbb.database.repository.ReservationRepository

/**
 * The Reservations tab: links to notifications and lists the entrant's
 * reservations. The profile (and the app policy inside it) is opened from
 * the icon on the Home screen.
 *
 * @param onNotificationsClick is invoked when the user clicks Notifications
 * @param onReservationClick is invoked with a reservation's id when the user clicks it
 */
@Composable
fun EntrantReservationsScreen(
    onNotificationsClick: () -> Unit,
    onReservationClick: (reservationId: String) -> Unit
) {
    DeviceIdentityGate {
        val reservationRepository = remember { ReservationRepository() }
        var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            reservationRepository.getMyReservations(
                onSuccess = {
                    reservations = it.sortedBy { r -> r.checkInDate }
                    isLoading = false
                },
                onFailure = { errorMessage = it.message; isLoading = false }
            )
        }
        val today = millisToIsoDate(System.currentTimeMillis())
        val active = reservations.filter {it.status == "confirmed"  && it.checkOutDate > today }
        val past = reservations.filter { it !in active }


        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Reservations", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onNotificationsClick) { Text("Notifications") }
            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> Text("Loading…")
                errorMessage != null -> Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                reservations.isEmpty() -> Text("No reservations yet. Win a lottery to book a stay.")
                else -> LazyColumn {
                    item { Text("Active", style = MaterialTheme.typography.titleMedium) }

                    items(active, key = {it.id}){ r ->
                        ReservationCard(r, onClick = { onReservationClick(r.id) })
                    }
                    item { Text("Past", style = MaterialTheme.typography.titleMedium) }
                    items(past, key = {it.id}){ r ->
                        ReservationCard(r, onClick = { onReservationClick(r.id) })

                    }
                }
            }
        }
    }
}

@Composable
fun ReservationCard(r: Reservation, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("${r.checkInDate} to ${r.checkOutDate}", style = MaterialTheme.typography.titleMedium)
            Text("Guests: ${r.guestCount} · Status: ${r.status}")
        }
    }



}

