package com.example.zephyr_bbb.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.User
import com.example.zephyr_bbb.database.repository.AdminRepository
import com.example.zephyr_bbb.database.repository.UserRepository

// admin screen for browsing and removing profiles
@Composable
fun AdminUserScreen(
    onBackClick: () -> Unit
) {

    val adminRepository = remember {
        AdminRepository()
    }

    // state used for profile data and errors
    var users by remember {
        mutableStateOf<List<User>>(emptyList())
    }

    var selectedUser by remember {
        mutableStateOf<User?>(null)
    }

    var userToRemove by remember {
        mutableStateOf<User?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // load all active users
    fun loadUsers() {
        isLoading = true
        errorMessage = null

        adminRepository.getUsers(
            onSuccess = { result ->
                users = result
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
                loadUsers()
            },
            onFailure = { error ->
                errorMessage = error.message
                isLoading = false
            }
        )
    }

    // confirmation before removing a profile
    userToRemove?.let { user ->

        AlertDialog(
            onDismissRequest = {
                userToRemove = null
            },
            title = {
                Text("Remove User?")
            },
            text = {
                Text(
                    "Removing this user will remove their profile from the active system."
                )
            },
            confirmButton = {
                Button(
                    onClick = {

                        isLoading = true
                        errorMessage = null

                        // use owner removal when the selected user is an owner
                        if ("owner" in user.roles) {

                            adminRepository.removeOwner(
                                ownerId = user.id,
                                onSuccess = {
                                    userToRemove = null
                                    selectedUser = null
                                    loadUsers()
                                },
                                onFailure = { error ->
                                    userToRemove = null
                                    errorMessage = error.message
                                    isLoading = false
                                }
                            )

                        } else {

                            adminRepository.removeProfile(
                                userId = user.id,
                                onSuccess = {
                                    userToRemove = null
                                    selectedUser = null
                                    loadUsers()
                                },
                                onFailure = { error ->
                                    userToRemove = null
                                    errorMessage = error.message
                                    isLoading = false
                                }
                            )
                        }
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        userToRemove = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // show selected user's profile details
    selectedUser?.let { user ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Button(
                onClick = {
                    selectedUser = null
                }
            ) {
                Text("Back")
            }

            Text(user.name.ifEmpty { "Profile" })

            Text("Name: ${user.name}")
            Text("Email: ${user.email}")
            Text("Roles: ${user.roles.joinToString()}")

            Button(
                onClick = {
                    userToRemove = user
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if ("owner" in user.roles) {
                        "Remove Owner"
                    } else {
                        "Remove User"
                    }
                )
            }
        }

        return
    }

    // display all profiles
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
            text = "All Profiles",
            modifier = Modifier.padding(vertical = 12.dp)
        )

        errorMessage?.let { error ->
            Text(
                text = "Error: $error",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (isLoading && users.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        LazyColumn {
            items(users) { user ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    Text(
                        if (user.name.isEmpty()) {
                            "Unnamed User"
                        } else {
                            user.name
                        }
                    )

                    Text("Roles: ${user.roles.joinToString()}")

                    Button(
                        onClick = {
                            selectedUser = user
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Profile")
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}