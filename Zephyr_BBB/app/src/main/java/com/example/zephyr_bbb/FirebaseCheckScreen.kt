package com.example.zephyr_bbb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.example.zephyr_bbb.database.repository.CampsiteRepository
import com.example.zephyr_bbb.database.repository.UserRepository

/**
 * Temporary screen for checking user and campsite persistence.
 */
@Composable
fun FirebaseCheckScreen(
    modifier: Modifier = Modifier
) {
    var status by remember {
        mutableStateOf("Connecting...")
    }

    var signedIn by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var createdId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        UserRepository().getOrCreateCurrentUser(
            onSuccess = { user ->
                status =
                    "Firebase connected!\n" +
                            "Roles: ${user.roles.joinToString()}"
                signedIn = true
            },
            onFailure = { error ->
                status = "Connection failed: ${error.message}"
            }
        )
    }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(status)

        Button(
            enabled = signedIn && !busy && createdId == null,
            onClick = {
                busy = true
                status = "Saving test campsite..."

                val repository = CampsiteRepository()

                val testCampsite = Campsite(
                    name = "DB Test Campsite",
                    description = "Dummy listing for database testing.",
                    locationName = "Test location",
                    maxOccupancy = 4,
                    pricePerNightCents = 6500
                )

                repository.createCampsite(
                    campsite = testCampsite,
                    onSuccess = { id ->
                        createdId = id
                        status = "Saved. Reading campsite..."

                        repository.getCampsite(
                            campsiteId = id,
                            onSuccess = { campsite ->
                                busy = false

                                status = if (campsite == null) {
                                    "Saved, but no campsite was returned.\nID: $id"
                                } else {
                                    "Campsite saved and read!\n\n" +
                                            "ID: ${campsite.id}\n" +
                                            "Name: ${campsite.name}\n" +
                                            "Guests: ${campsite.maxOccupancy}\n" +
                                            "Price: ${campsite.pricePerNightCents} cents\n" +
                                            "Status: ${campsite.status}"
                                }
                            },
                            onFailure = { error ->
                                busy = false
                                status =
                                    "Saved with ID: $id\n" +
                                            "Read failed: ${error.message}"
                            }
                        )
                    },
                    onFailure = { error ->
                        busy = false
                        status = "Save failed: ${error.message}"
                    }
                )
            }
        ) {
            Text("Test campsite save and read")
        }
    }
}