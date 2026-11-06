package com.example.zephyr_bbb.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp



/**
 * Composable function that displays the main screen for the app
 * This screen gets the users current roles assigned to them and allows them to see different
 * things depending on what there permissions are.
 * If the user is an admin it shows a button that leads to an admin dashboard
 * If the user is a campsite owner it shows a button that leads to an owner dashboard
 *
 * @param onBackClick is invoked when the user clicks the back button
 * @param onCampsitesClick is invoked when the user clicks the My Campsites Button
 * @param onLotteriesClick is invoked when the user clicks the My Lotteries Button
 * @param onReservationsClick is invoked when the user clicks the Reservations Button
 * @param onNotificationsClick is invoked when the user clicks the Notifications Button
 */
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Back Button
            Button(
                onClick = onBackClick
            ) {
                Text("<")
            }
            Text("Campsite Owner Dashboard")
        }
        Text("Welcome, Owner")
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