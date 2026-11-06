package com.example.zephyr_bbb.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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

/**Composable function that creates the screen seen by the owner when they click My Lotteries
* on the owner dashboard.
* Loads the owner's lotteries and campsites and shows them in a list, with a button to
* create a new lottery.
 * @param onBackClick is invoked when the user clicks the back button
 * @param onCreateLotteryClick is invoked when the user clicks the Create Lottery button
 * @param onLotteryClick is invoked when the user clicks View Lottery on one of their lotteries
*/
@Composable
fun OwnerMyLotteriesScreen(
    onBackClick: () -> Unit,
    onCreateLotteryClick: () -> Unit = {},
    onLotteryClick: (Lottery) -> Unit = {}
) {
    val userRepository = remember {
        UserRepository()
    }
    val campsiteRepository = remember {
        CampsiteRepository()
    }
    val lotteryRepository = remember {
        LotteryRepository()
    }
    var lotteries by remember {
        mutableStateOf<List<Lottery>>(emptyList())
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
    var reloadKey by remember {
        mutableIntStateOf(0)
    }
    LaunchedEffect(reloadKey) {
        val loadLotteries = {
            lotteryRepository.getMyLotteries(
                onSuccess = { list ->
                    lotteries = list.sortedBy { it.registrationOpensAt }
                    now = Date()
                    isLoading = false
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

    OwnerMyLotteriesContent(
        lotteries = lotteries,
        campsiteNames = campsiteNames,
        now = now,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onBackClick = onBackClick,
        onCreateLotteryClick = onCreateLotteryClick,
        onLotteryClick = onLotteryClick
    )
}

/**Composable function that shows the list of an owner's lotteries.
* Each lottery shows its name, campsites and status, with a View Lottery button.
* A Create Lottery button sits at the bottom of the screen.
* @param lotteries The lotteries to list
* @param campsiteNames Campsite names keyed by campsite ID
* @param now The current time, used to work out each lottery's status
* @param isLoading True while the lotteries are being loaded
* @param errorMessage An error to show, or null
* @param onBackClick is invoked when the user clicks the back button
* @param onCreateLotteryClick is invoked when the user clicks the Create Lottery button
* @param onLotteryClick is invoked when the user clicks View
*/
@Composable
fun OwnerMyLotteriesContent(
    lotteries: List<Lottery>,
    campsiteNames: Map<String, String>,
    now: Date,
    isLoading: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onCreateLotteryClick: () -> Unit,
    onLotteryClick: (Lottery) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Back Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onBackClick
            ) {
                Text("<")
            }

            Text("My Lotteries")
        }

        val today = SimpleDateFormat("MMM d, yyyy", Locale.US).format(now)
        val countText = if (lotteries.size == 1) "1 lottery" else "${lotteries.size} lotteries"

        Text("As of $today · $countText")

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (isLoading) {
            Text("Loading lotteries...")
        } else if (lotteries.isEmpty()) {
            Text("You have no lotteries yet.")
        }

        // Makes and shows all the lottery objects on the screen in a scrollable list
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(lotteries) { lottery ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val names = lottery.campsiteIds.mapNotNull { campsiteNames[it] }

                    Text(lottery.title)

                    if (names.isNotEmpty()) {
                        Text(names.joinToString(", "))
                    }
                    Text(
                        "Status: ${
                            lotteryStatusLabel(
                                lottery.status,
                                lottery.registrationOpensAt?.toDate(),
                                lottery.registrationClosesAt?.toDate(),
                                now
                            )
                        }"
                    )
                    Button(
                        onClick = {
                            onLotteryClick(lottery)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Lottery")
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        Button(
            onClick = onCreateLotteryClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Lottery")
        }
    }
}