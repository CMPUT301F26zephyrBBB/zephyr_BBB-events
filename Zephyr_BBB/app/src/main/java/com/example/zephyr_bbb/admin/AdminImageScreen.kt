package com.example.zephyr_bbb.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.example.zephyr_bbb.database.model.ImageRecord
import com.example.zephyr_bbb.database.repository.AdminRepository
import com.example.zephyr_bbb.database.repository.UserRepository
import androidx.compose.foundation.layout.statusBarsPadding

// admin screen for browsing and removing uploaded images
// outstanding: shows image details as text, thumbnails need storage uploads first

/**
 * Lets an admin browse all uploaded images and remove one after confirming.
 *
 * @param onBackClick called when the admin presses back
 */
@Composable
fun AdminImageScreen(
    onBackClick: () -> Unit
){

    val adminRepository = remember {
        AdminRepository()
    }

    // state used for image records and errors
    var images by remember {
        mutableStateOf<List<ImageRecord>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var imageToRemove by remember {
        mutableStateOf<ImageRecord?>(null)
    }

    // load all uploaded image records
    fun loadImages() {
        isLoading = true
        errorMessage = null

        adminRepository.getImages(
            onSuccess = { result ->
                images = result
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
                loadImages()
            },
            onFailure = { error ->
                errorMessage = error.message
                isLoading = false
            }
        )
    }

    // confirmation before removing an image
    imageToRemove?.let { image ->

        AlertDialog(
            onDismissRequest = {
                imageToRemove = null
            },
            title = {
                Text("Remove Image?")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Uploaded by: ${image.uploaderId}")
                    Text("Removing this image will delete it from the database.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        imageToRemove = null
                        isLoading = true
                        errorMessage = null

                        // remove image record, then refresh the list
                        adminRepository.removeImage(
                            imageId = image.id,
                            onSuccess = {
                                loadImages()
                            },
                            onFailure = { error ->
                                errorMessage = error.message
                                isLoading = false
                            }
                        )
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        imageToRemove = null
                    }
                ) {
                    Text("Cancel")
                }
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

        Text("Manage Images")

        errorMessage?.let { error ->
            Text(
                text = "Error: $error",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (isLoading && images.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        // display all uploaded image records
        LazyColumn {
            items(images) { image ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    Text("Image ID: ${image.id}")
                    Text("Uploader ID: ${image.uploaderId}")
                    Text("Target type: ${image.targetType}")
                    Text("Target ID: ${image.targetId}")
                    Text("Status: ${image.status}")

                    Button(
                        onClick = {
                            // ask before removing
                            imageToRemove = image
                        }
                    ) {
                        Text("Remove Image")
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}