package com.example.zephyr_bbb.entrant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.repository.UserRepository

/**
 * Lets the entrant view and edit their profile (name, email, phone). US 01.07.03.
 */
@Composable
fun ProfileScreen(onBackClick: () -> Unit) {
    DeviceIdentityGate { user ->
        val userRepository = remember { UserRepository() }

        // Start the fields with what's already saved for this user
        var name by remember(user.id) { mutableStateOf(user.name) }
        var email by remember(user.id) { mutableStateOf(user.email) }
        var phone by remember(user.id) { mutableStateOf(user.phone) }

        var isSaving by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val canSave = name.isNotBlank() && !isSaving

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBackClick) { Text("Back") }
            Text("My profile", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; message = null },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; message = null },
                label = { Text("Email (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; message = null },
                label = { Text("Phone (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    isSaving = true
                    errorMessage = null
                    userRepository.updateProfile(
                        name = name,
                        email = email,
                        phone = phone,
                        onSuccess = {
                            isSaving = false
                            message = "Profile saved"
                        },
                        onFailure = {
                            isSaving = false
                            errorMessage = it.message
                        }
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isSaving) "Saving…" else "Save") }

            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}