package com.example.zephyr_bbb.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp



// Creates Dashboard for the Owner
// Which has buttons that navigate to My Campsites, My Lotteries, Reservations, and Notifications
@Composable
fun OwnerDashboardScreen(
    onBackClick: () -> Unit,
    onCampsitesClick: () -> Unit,
    onLotteriesClick: () -> Unit,
    onReservationsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Back Button
        Button(
            onClick = onBackClick
        ) {
            Text("Back")
        }
        Text("Campsite Owner Dashboard")
        // Navigates to Campsites
        Button(
            onClick = onCampsitesClick,
            modifier = Modifier.fillMaxWidth()
        ){
            Text("My Campsites")
        }
        // Navigates to Lotteries
        Button(
            onClick = onLotteriesClick,
            modifier = Modifier.fillMaxWidth()
        ){
            Text("My Lotteries")
        }
        // Navigates to Reservations
        Button(
            onClick = onReservationsClick,
            modifier = Modifier.fillMaxWidth()
        ){
            Text("Reservations")
        }
        // Navigates to Notifications
        Button(
            onClick = onNotificationsClick,
            modifier = Modifier.fillMaxWidth()
        ){
            Text("Notifications")
        }
    }
}