package com.example.zephyr_bbb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.zephyr_bbb.database.repository.ImageRepository
import com.example.zephyr_bbb.database.repository.UserRepository

/**
 * Composable function that displays the main screen for the app
 * This screen gets the users current roles assigned to them and allows them to see different
 * things depending on what there permissions are.
 * If the user is an admin it shows a button that leads to an admin dashboard
 * If the user is a campsite owner it shows a button that leads to an owner dashboard
 * Every user gets a profile button in the top right corner that opens their profile.
 * It shows their profile photo if they have one (US 01.08.02)
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

    // download URL of the user's profile photo, null if they don't have one
    var profilePhotoUrl by remember {
        mutableStateOf<String?>(null)
    }

    // check the signed-in user's roles
    LaunchedEffect(Unit) {
        UserRepository().getOrCreateCurrentUser(
            onSuccess = { user ->
                isAdmin = "admin" in user.roles
                isOwner = "owner" in user.roles
                // Home is loaded again each time it's opened, so a photo changed
                // on the profile screen shows up here when the user comes back
                val imageId = user.profileImageId
                if (!imageId.isNullOrBlank()) {
                    ImageRepository().getImageUrl(
                        imageId = imageId,
                        onSuccess = { url -> profilePhotoUrl = url },
                        onFailure = { profilePhotoUrl = null }
                    )
                } else {
                    profilePhotoUrl = null
                }
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
            ProfileIconButton(photoUrl = profilePhotoUrl, onClick = onProfileClick)
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

/**
 * The profile button in the top right of the home screen. Shows the user's
 * profile photo in a small circle, or the person icon if they don't have one
 * (or while the photo is still loading). US 01.08.02.
 *
 * @param photoUrl download URL of the user's profile photo, or null for the person icon
 * @param onClick is invoked when the user clicks the button
 */
@Composable
internal fun ProfileIconButton(
    photoUrl: String?,
    onClick: () -> Unit
) {
    // IconButton keeps the touch target at least 48dp
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = "My profile" }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.person),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            // Drawn over the icon, which shows through until the photo has loaded
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}
