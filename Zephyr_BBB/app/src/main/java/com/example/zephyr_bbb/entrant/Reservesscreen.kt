package com.example.zephyr_bbb.entrant



import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.database.repository.AvailabilityRepository
import com.example.zephyr_bbb.database.repository.CampsiteRepository
import com.example.zephyr_bbb.database.repository.LotteryRepository
import com.example.zephyr_bbb.database.repository.ReservationRepository

/**
 * Lets a lottery winner pick a campsite, reservable dates and guests,
 * accept the rules, and book. US 01.04.01 (reservable dates) and
 * US 01.05.01 (confirmation, together with ReservationConfirmationScreen).
 */
@Composable
fun ReserveScreen(
    lotteryId: String,
    onBackClick: () -> Unit,
    onReserved: (reservationId: String) -> Unit
) {
    DeviceIdentityGate {
        val lotteryRepository = remember { LotteryRepository() }
        val campsiteRepository = remember { CampsiteRepository() }
        val availabilityRepository = remember { AvailabilityRepository() }
        val reservationRepository = remember { ReservationRepository() }

        var lottery by remember { mutableStateOf<Lottery?>(null) }
        var campsites by remember { mutableStateOf<List<Campsite>>(emptyList()) }
        var selectedCampsite by remember { mutableStateOf<Campsite?>(null) }
        var openNights by remember { mutableStateOf<List<String>>(emptyList()) }
        var isLoadingNights by remember { mutableStateOf(false) }
        var firstNight by remember { mutableStateOf<String?>(null) }
        var lastNight by remember { mutableStateOf<String?>(null) }
        var guestCount by remember { mutableLongStateOf(1L) }
        var rulesAccepted by remember { mutableStateOf(false) }
        var isBooking by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Load the lottery, then each campsite in it.
        LaunchedEffect(lotteryId) {
            lotteryRepository.getLottery(
                lotteryId,
                onSuccess = { loaded ->
                    lottery = loaded
                    loaded?.campsiteIds?.forEach { campsiteId ->
                        campsiteRepository.getCampsite(
                            campsiteId,
                            onSuccess = { campsite ->
                                if (campsite != null) {
                                    campsites = campsites + campsite
                                    if (selectedCampsite == null) selectedCampsite = campsite
                                }
                            },
                            onFailure = { errorMessage = it.message }
                        )
                    }
                },
                onFailure = { errorMessage = it.message }
            )
        }

        // When the campsite changes, load its open nights inside the stay period.
        LaunchedEffect(selectedCampsite?.id, lottery?.id) {
            val campsite = selectedCampsite ?: return@LaunchedEffect
            val currentLottery = lottery ?: return@LaunchedEffect
            firstNight = null
            lastNight = null
            guestCount = 1L
            rulesAccepted = false
            isLoadingNights = true
            availabilityRepository.getAvailability(
                campsite.id,
                onSuccess = { days ->
                    // A night must be before stayEndDate, because checkout
                    // is the next day and can't be after stayEndDate.
                    openNights = days
                        .filter { it.status == "open" }
                        .map { it.date }
                        .filter { it >= currentLottery.stayStartDate && it < currentLottery.stayEndDate }
                        .sorted()
                    isLoadingNights = false
                },
                onFailure = { errorMessage = it.message; isLoadingNights = false }
            )
        }

        // Tap a night to start, tap a later night to end the stay.
        fun onNightClick(night: String) {
            val first = firstNight
            if (first == null || lastNight != null || night < first) {
                firstNight = night
                lastNight = null
            } else {
                lastNight = night
            }
        }

        val chosenNights = firstNight?.let { first ->
            EntrantUtils.nightsBetween(first, lastNight ?: first)
        } ?: emptyList()
        val allNightsOpen = chosenNights.isNotEmpty() && chosenNights.all { it in openNights }
        val checkInDate = chosenNights.firstOrNull()
        val checkOutDate = chosenNights.lastOrNull()?.let { EntrantUtils.nextDay(it) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBackClick) { Text("Back") }
            Text("Reserve your stay", style = MaterialTheme.typography.headlineMedium)
            lottery?.let { Text("Stay period: ${it.stayStartDate} to ${it.stayEndDate}") }

            // 1. Campsite
            if (campsites.size > 1) {
                Text("Campsite", style = MaterialTheme.typography.titleMedium)
                campsites.forEach { campsite ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCampsite = campsite }
                    ) {
                        RadioButton(
                            selected = selectedCampsite?.id == campsite.id,
                            onClick = { selectedCampsite = campsite }
                        )
                        Text("${campsite.name} – ${campsite.locationName}")
                    }
                }
            } else {
                selectedCampsite?.let {
                    Text("${it.name} – ${it.locationName}", style = MaterialTheme.typography.titleMedium)
                }
            }

            // 2. Dates
            Text("Nights", style = MaterialTheme.typography.titleMedium)
            Text("Tap your first night, then your last night.")
            when {
                isLoadingNights -> Text("Loading available dates…")
                openNights.isEmpty() -> Text("No open nights for this campsite in the stay period.")
                else -> openNights.forEach { night ->
                    val isChosen = night in chosenNights
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNightClick(night) },
                        colors = if (isChosen) {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            CardDefaults.cardColors()
                        }
                    ) {
                        Text(night, modifier = Modifier.padding(12.dp))
                    }
                }
            }
            if (chosenNights.isNotEmpty() && !allNightsOpen) {
                Text(
                    "Some nights in that range are taken. Pick nights next to each other.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (allNightsOpen) {
                Text("Check-in $checkInDate, check-out $checkOutDate (${chosenNights.size} nights)")
            }

            // 3. Guests
            selectedCampsite?.let { campsite ->
                Text("Guests", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { guestCount-- },
                        enabled = guestCount > 1
                    ) { Text("−") }
                    Text("  $guestCount  ")
                    OutlinedButton(
                        onClick = { guestCount++ },
                        enabled = guestCount < campsite.maxOccupancy
                    ) { Text("+") }
                    Text("  (max ${campsite.maxOccupancy})")
                }

                if (allNightsOpen) {
                    Text(
                        "Total: " + EntrantUtils.formatPrice(
                            campsite.pricePerNightCents * chosenNights.size,
                            campsite.currency
                        )
                    )
                }

                // 4. Rules
                Text("Campsite rules", style = MaterialTheme.typography.titleMedium)
                Text(campsite.rules.ifBlank { "No rules listed." })
                if (campsite.cancellationPolicy.isNotBlank()) {
                    Text("Cancellation policy", style = MaterialTheme.typography.titleMedium)
                    Text(campsite.cancellationPolicy)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { rulesAccepted = !rulesAccepted }
                ) {
                    Checkbox(checked = rulesAccepted, onCheckedChange = { rulesAccepted = it })
                    Text("I accept the campsite rules and cancellation policy")
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isBooking = true
                        reservationRepository.createReservation(
                            lotteryId = lotteryId,
                            campsiteId = campsite.id,
                            checkInDate = checkInDate!!,
                            checkOutDate = checkOutDate!!,
                            guestCount = guestCount,
                            rulesAccepted = rulesAccepted,
                            onSuccess = { reservationId ->
                                isBooking = false
                                onReserved(reservationId)
                            },
                            onFailure = {
                                errorMessage = it.message
                                isBooking = false
                            }
                        )
                    },
                    enabled = allNightsOpen && rulesAccepted && !isBooking,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isBooking) "Booking…" else "Confirm reservation") }
            }

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}