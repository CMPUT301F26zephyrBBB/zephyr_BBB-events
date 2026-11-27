package com.example.zephyr_bbb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.repository.UserRepository

/**
 * Composable function that displays the main screen for the app
 * This screen gets the users current roles assigned to them and allows them to see different
 * things depending on what there permissions are.
 * If the user is an admin it shows a button that leads to an admin dashboard
 * If the user is a campsite owner it shows a button that leads to an owner dashboard
 * Every user gets a profile button in the top right corner that opens their profile
 *
 * @param onAdminClick is invoked when the user clicks the admin dashboard button
 * @param onOwnerClick is invoked when the user clicks the owner dashboard button
 * @param onScanClick is invoked when the user clicks the button to scan a campsite QR code
 * @param onProfileClick is invoked when the user clicks the profile icon in the top right
 */
@Composable
fun HomeScreen(
    onOwnerClick: () -> Unit,
    onAdminClick: () -> Unit,
    onScanClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {

    var isOwner by remember {
        mutableStateOf(false)
    }

    var isAdmin by remember {
        mutableStateOf(false)
    }

    // check the signed-in user's roles
    LaunchedEffect(Unit) {
        UserRepository().getOrCreateCurrentUser(
            onSuccess = { user ->
                isAdmin = "admin" in user.roles
                isOwner = "owner" in user.roles
            },
            onFailure = {
                isAdmin = false
                isOwner = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Title on the left, profile icon in the top right corner
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Home",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            // IconButton keeps the touch target at least 48dp
            IconButton(onClick = onProfileClick) {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = "My profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Text("Welcome to Zephyr")

        // Scanning a campsite's QR code opens its details (US 01.12.01)
        Button(
            onClick = onScanClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Campsite QR Code")
        }

        // only admins can see the admin dashboard button
        if (isAdmin) {
            Button(
                onClick = onAdminClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Admin Dashboard")
            }
        }

        // Only owners can see the home page for owners
        if (isOwner) {
            Button(
                onClick = onOwnerClick,
                modifier = Modifier.fillMaxWidth()
            ){
                Text("Owner Dashboard")
            }
        }

    }
}