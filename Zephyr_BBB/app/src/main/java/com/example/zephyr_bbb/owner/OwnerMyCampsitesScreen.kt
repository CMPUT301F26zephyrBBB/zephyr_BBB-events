package com.example.zephyr_bbb.owner


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
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
import com.example.zephyr_bbb.database.repository.CampsiteRepository
import kotlin.collections.emptyList


// Function that creates the screen that the user sees when they want to see all there listings for
// campsites, and also to add new listings
@Composable
fun OwnerMyCampsitesScreen(
    onBackClick: () -> Unit,
    onAddCampsiteClick: () -> Unit,
    onCampsiteClick: (Campsite) -> Unit
) {
    val campsiteRepository = remember {
        CampsiteRepository()
    }
    var campsites by remember {
        mutableStateOf<List<Campsite>>(emptyList())
    }
    var selectedCampsite by remember {
        mutableStateOf<Campsite?>(null)
    }
    var isLoading by remember {
        mutableStateOf(true)
    }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {
        campsiteRepository.getMyCampsites(
            onSuccess = {list->
                campsites = list
                isLoading = false
            },
            onFailure = {error->
                errorMessage = error.message
                isLoading = false
            }
        )
    }
    // A campsite object that appears on the screen
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
            Text("Description: ${campsite.description}")
            Text("Maximum occupancy: ${campsite.maxOccupancy}")
            Text("Price per night: ${campsite.pricePerNightCents / 100.0}")
            Text("Status: ${campsite.status}")
            Button(
                onClick = {
                    selectedCampsite = null
                    onCampsiteClick(campsite)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Campsite")
            }
        }
        return
    }

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
                Text("Back")
            }

            Text("My Campsites")
        }
        // Makes and shows all the campsite objects on the screen
        // In a scrollable list
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
        // Button that stays at the bottom of the screen
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ){
            Button(
                onClick = onAddCampsiteClick
            ){
                Text("Add Campsite")
            }
        }
    }
}