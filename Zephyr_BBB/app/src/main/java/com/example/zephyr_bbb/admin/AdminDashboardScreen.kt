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
// outstanding: all messages and role switch not added yet (US 03.12.01, US 03.11.01)

/**
 * Main admin menu with links to every admin tool.
 *
 * @param onBackClick called when the admin presses back
 * @param onProfilesClick opens the profile browser
 * @param onCampsitesClick opens the campsite browser
 * @param onImagesClick opens the uploaded image browser
 * @param onNotificationsClick opens the notification log
 */
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