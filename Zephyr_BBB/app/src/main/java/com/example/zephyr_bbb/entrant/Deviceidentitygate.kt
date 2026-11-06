package com.example.zephyr_bbb.entrant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.User
import com.example.zephyr_bbb.database.repository.UserRepository

/**
 * Makes sure the device is signed in (anonymous Firebase auth) before
 * showing an entrant screen, then passes the signed-in User to it.
 * The device's Firebase UID is the entrant's identity. US 01.09.01.
 */
@Composable
fun DeviceIdentityGate(
    modifier: Modifier = Modifier,
    content: @Composable (User) -> Unit
) {
    val userRepository = remember { UserRepository() }
    var user by remember { mutableStateOf<User?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        errorMessage = null
        userRepository.getOrCreateCurrentUser(
            onSuccess = { user = it },
            onFailure = { errorMessage = it.message ?: "Could not sign in." }
        )
    }

    val signedInUser = user
    when {
        signedInUser != null -> content(signedInUser)

        errorMessage != null -> Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(errorMessage ?: "")
            Button(onClick = { attempt++ }) { Text("Try again") }
        }

        else -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
