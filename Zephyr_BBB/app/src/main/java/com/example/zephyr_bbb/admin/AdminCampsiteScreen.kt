package com.example.zephyr_bbb.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.repository.AdminRepository
import com.example.zephyr_bbb.database.repository.UserRepository

// admin screen for browsing and removing campsites
@Composable
fun AdminCampsiteScreen(
    onBackClick: () -> Unit
) {

    val adminRepository = remember {
        AdminRepository()
    }

    // state used for campsite data and errors
    var campsites by remember {
        mutableStateOf<List<Campsite>>(emptyList())
    }

    var selectedCampsite by remember {
        mutableStateOf<Campsite?>(null)
    }

    var campsiteToRemove by remember {
        mutableStateOf<Campsite?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // load all registered campsites
    fun loadCampsites() {
        isLoading = true
        errorMessage = null

        adminRepository.getCampsites(
            onSuccess = { result ->
                campsites = result
                isLoading = false
            },
            onFailure = { error ->
                errorMessage = error.message
                isLoading = false
            }
        )
    }

    // sign in first, then load admin data
    LaunchedEffect(Unit) {
        UserRepository().getOrCreateCurrentUser(
            onSuccess = {
                loadCampsites()
            },
            onFailure = { error ->
                errorMessage = error.message
                isLoading = false
            }
        )
    }

    // confirmation before removing a campsite
    campsiteToRemove?.let { campsite ->

        AlertDialog(
            onDismissRequest = {
                campsiteToRemove = null
            },
            title = {
                Text("Remove Campsite?")
            },
            text = {
                Text(
                    "Removing this campsite will delete the listing from the system."
                )
            },
            confirmButton = {
                Button(
                    onClick = {

                        isLoading = true
                        errorMessage = null

                        adminRepository.removeCampsite(
                            campsiteId = campsite.id,
                            onSuccess = {
                                campsiteToRemove = null
                                selectedCampsite = null
                                loadCampsites()
                            },
                            onFailure = { error ->
                                campsiteToRemove = null
                                errorMessage = error.message
                                isLoading = false
                            }
                        )
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        campsiteToRemove = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // show selected campsite details
    selectedCampsite?.let { campsite ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Button(
                onClick = {
                    selectedCampsite = null
                }
            ) {
                Text("Back")
            }

            Text("Campsite Details")

            Text("Name: ${campsite.name}")
            Text("Location: ${campsite.locationName}")
            Text("Maximum occupancy: ${campsite.maxOccupancy}")
            Text("Status: ${campsite.status}")

            Button(
                onClick = {
                    campsiteToRemove = campsite
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Remove Campsite")
            }
        }

        return
    }

    // display all registered campsites
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {

        Button(
            onClick = onBackClick
        ) {
            Text("Back")
        }

        Text(
            text = "All Campsites",
            modifier = Modifier.padding(vertical = 12.dp)
        )

        errorMessage?.let { error ->
            Text(
                text = "Error: $error",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (isLoading && campsites.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        LazyColumn {
            items(campsites) { campsite ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    Text(campsite.name)
                    Text(campsite.locationName)

                    Button(
                        onClick = {
                            selectedCampsite = campsite
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Details")
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}