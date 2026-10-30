package com.example.zephyr_bbb.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.AppNotification
import com.example.zephyr_bbb.database.repository.AdminRepository
import com.example.zephyr_bbb.database.repository.UserRepository

// admin screen for reviewing notification logs
@Composable
fun AdminNotificationLogScreen(
    onBackClick: () -> Unit
) {

    val adminRepository = remember {
        AdminRepository()
    }

    // state used for notification data and errors
    var notifications by remember {
        mutableStateOf<List<AppNotification>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // load all notification logs
    fun loadNotifications() {
        isLoading = true
        errorMessage = null

        adminRepository.getNotificationLogs(
            onSuccess = { result ->
                notifications = result
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
                loadNotifications()
            },
            onFailure = { error ->
                errorMessage = error.message
                isLoading = false
            }
        )
    }

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
            text = "All Notifications",
            modifier = Modifier.padding(vertical = 12.dp)
        )

        errorMessage?.let { error ->
            Text(
                text = "Error: $error",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (isLoading && notifications.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(notifications) { notification ->

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        Text(notification.title)

                        Text(notification.body)

                        Text("Type: ${notification.type}")

                        Text(
                            "From: ${notification.senderId}"
                        )

                        Text(
                            "Recipient: ${notification.recipientId}"
                        )

                        Text(
                            "Status: ${notification.deliveryStatus}"
                        )
                    }
                }
            }
        }
    }
}