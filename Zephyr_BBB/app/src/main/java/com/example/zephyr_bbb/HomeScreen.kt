package com.example.zephyr_bbb

import android.R.attr.onClick
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.example.zephyr_bbb.database.repository.UserRepository

// main home screen
@Composable
fun HomeScreen(
    onOwnerClick: () -> Unit,
    onAdminClick: () -> Unit,
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

        Text("Home")

        Text("Welcome to Zephyr")

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