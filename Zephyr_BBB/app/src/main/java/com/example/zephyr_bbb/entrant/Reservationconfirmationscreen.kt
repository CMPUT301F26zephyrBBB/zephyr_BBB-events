package com.example.zephyr_bbb.entrant



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.Reservation
import com.example.zephyr_bbb.database.repository.CampsiteRepository
import com.example.zephyr_bbb.database.repository.ReservationRepository

/**
 * Shows a booked reservation's details. US 01.05.01.
 */
@Composable
fun ReservationConfirmationScreen(
    reservationId: String,
    onDoneClick: () -> Unit
) {
    DeviceIdentityGate {
        val reservationRepository = remember { ReservationRepository() }
        val campsiteRepository = remember { CampsiteRepository() }
        var reservation by remember { mutableStateOf<Reservation?>(null) }
        var campsite by remember { mutableStateOf<Campsite?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(reservationId) {
            reservationRepository.getReservation(
                reservationId,
                onSuccess = { loaded ->
                    reservation = loaded
                    isLoading = false
                    loaded?.let {
                        campsiteRepository.getCampsite(
                            it.campsiteId,
                            onSuccess = { site -> campsite = site },
                            onFailure = { e -> errorMessage = e.message }
                        )
                    }
                },
                onFailure = { errorMessage = it.message; isLoading = false }
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val r = reservation
            when {
                isLoading -> Text("Loading…")
                r == null -> Text("Reservation not found.")
                else -> {
                    Text(
                        if (r.status == "confirmed") "Reservation confirmed" else "Reservation ${r.status}",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("Campsite: ${campsite?.name ?: "…"}")
                    campsite?.locationName?.let { Text("Location: $it") }
                    Text("Check-in: ${r.checkInDate}")
                    Text("Check-out: ${r.checkOutDate}")
                    Text("Guests: ${r.guestCount}")
                    Text("Total: " + EntrantUtils.formatPrice(r.totalPriceCents, r.currency))
                    Text("Confirmation #: ${r.id}", style = MaterialTheme.typography.labelMedium)
                }
            }

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(onClick = onDoneClick, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        }
    }
}