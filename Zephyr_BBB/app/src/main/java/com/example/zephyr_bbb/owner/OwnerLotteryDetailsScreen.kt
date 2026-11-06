package com.example.zephyr_bbb.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.database.repository.CampsiteRepository
import com.example.zephyr_bbb.database.repository.LotteryRepository
import com.example.zephyr_bbb.database.repository.UserRepository
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Composable function that creates the screen seen by the owner when they click View Lottery.
 * Loads the lottery and the names of its campsites.
 *
 * @param lotteryId The unique id of the lottery to show
 * @param onBackClick is invoked when the user clicks the back button
 * @param onEditLotteryClick is invoked with the lottery id when the user clicks Edit Lottery
*/
@Composable
fun OwnerLotteryDetailsScreen(
    lotteryId: String,
    onBackClick: () -> Unit,
    onEditLotteryClick: (String) -> Unit) {
    val userRepository = remember {
        UserRepository()
    }
    val campsiteRepository = remember {
        CampsiteRepository()
    }
    val lotteryRepository = remember {
        LotteryRepository()
    }
    var lottery by remember {
        mutableStateOf<Lottery?>(null)
    }
    var campsiteNames by remember {
        mutableStateOf<Map<String, String>>(emptyMap())
    }
    var now by remember {
        mutableStateOf(Date())
    }
    var isLoading by remember {
        mutableStateOf(true)
    }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }
    LaunchedEffect(lotteryId, reloadKey) {
        val loadLottery = {
            lotteryRepository.getLottery(
                lotteryId = lotteryId,
                onSuccess = { loaded ->
                    lottery = loaded
                    now = Date()
                    isLoading = false

                    if (loaded == null) {
                        errorMessage = "This lottery could not be found."
                    }
                },
                onFailure = { error ->
                    errorMessage = error.message
                    isLoading = false
                }
            )
        }

        try {
            userRepository.getOrCreateCurrentUser(
                onSuccess = {
                    campsiteRepository.getMyCampsites(
                        onSuccess = { list ->
                            campsiteNames = list.associate { it.id to it.name }
                        },
                        onFailure = { error ->
                            errorMessage = error.message
                        }
                    )
                    lotteryRepository.getLottery(
                        lotteryId = lotteryId,
                        onSuccess = { loaded ->
                            lottery = loaded
                            now = Date()
                            isLoading = false

                            if (loaded == null) {
                                errorMessage = "This lottery could not be found."
                            }
                },
                onFailure = { error ->
                    errorMessage = error.message
                    isLoading = false
                }
                )
            },
            onFailure = { error ->
                errorMessage = error.message
                isLoading = false
            }
            )
        } catch (e: Exception) {
            errorMessage = e.message
            isLoading = false
        }
    }
    LotteryDetailsContent(
        lottery = lottery,
        campsiteNames = campsiteNames,
        now = now,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onBackClick = onBackClick,
        onEditLotteryClick = { onEditLotteryClick(lotteryId) },
    )
}
/**
 * Composable function that shows a lottery's details: its campsites, stay dates and status.
 * The Edit Lottery button only shows while the lottery can still be changed.
 * It shows the registration period.
 * It shows the booking period.
 * It shows the waitlist size and its capacity, or "unlimited".
 *
 *  @param lottery The lottery to show, or null while it is loading
 *  @param campsiteNames Campsite names keyed by campsite ID
 *  @param now The current time, used to work out the lottery's status
 *  @param isLoading True while the lottery is being loaded
 *  @param errorMessage An error to show, or null
 *  @param onBackClick is invoked when the user clicks the back button
 *  @param onEditLotteryClick is invoked when the user clicks Edit Lottery
 */
@Composable
fun LotteryDetailsContent(
    lottery: Lottery?,
    campsiteNames: Map<String, String>,
    now: Date,
    isLoading: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onEditLotteryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Back button and text at the top of the screen
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onBackClick
            ) {
                Text("<")
            }

            Text("Lottery Details")
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (lottery == null) {
            if (isLoading) {
                Text("Loading lottery...")
            }
            return@Column
        }

        val status = lotteryStatusLabel(
            lottery.status,
            lottery.registrationOpensAt?.toDate(),
            lottery.registrationClosesAt?.toDate(),
            now
        )
        val names = lottery.campsiteIds.map { campsiteNames[it] ?: "Unknown campsite" }

        Text(lottery.title)
        Text("Status: $status")

        Text("Campsites")
        Text(names.joinToString(" · "))

        Text("Registration period")
        Text(
            formatClosingRange(
                lottery.registrationOpensAt?.toDate(),
                lottery.registrationClosesAt?.toDate()
            )
        )
        Text("Booking period")
        Text(
            formatClosingRange(
                lottery.bookingOpensAt?.toDate(),
                lottery.bookingClosesAt?.toDate()
            )
        )
        Text("Stay dates")
        Text(formatStayRange(lottery.stayStartDate, lottery.stayEndDate))

        Text("Winner count: ${lottery.winnerCount}")

        Text("Waitlist size: ${lottery.entryCount}")
        Text("Waitlist capacity: ${lottery.waitlistCapacity?.toString() ?: "unlimited"}")

        if (canEditLottery(lottery.status)) {
            Button(
                onClick = onEditLotteryClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Lottery")
            }
        }
    }
}
