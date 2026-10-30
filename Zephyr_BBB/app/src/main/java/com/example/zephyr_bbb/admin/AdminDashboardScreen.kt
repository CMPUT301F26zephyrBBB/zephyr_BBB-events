package com.example.zephyr_bbb.admin

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

// main menu for admin tools
@Composable
fun AdminDashboardScreen(
    onBackClick: () -> Unit,
    onProfilesClick: () -> Unit,
    onCampsitesClick: () -> Unit,
    onImagesClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Button(
            onClick = onBackClick
        ) {
            Text("Back")
        }

        Text("Admin Dashboard")

        Button(
            onClick = onProfilesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("All Profiles")
        }

        Button(
            onClick = onCampsitesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("All Campsites")
        }

        Button(
            onClick = onImagesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("All Uploaded Images")
        }

        Button(
            onClick = onNotificationsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("All Notifications")
        }
    }
}