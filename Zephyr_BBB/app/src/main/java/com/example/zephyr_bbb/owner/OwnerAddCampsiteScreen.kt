package com.example.zephyr_bbb.owner


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.repository.CampsiteRepository
import com.example.zephyr_bbb.database.repository.UserRepository


/**
 * A composable function that creates the screen that users see once they click the add button on the
 * OwnerMyCampsites screen
 * Allows the user to add/create new campsite objects. It provides the user with text input fields
 * to enter all the information about their campsite including, Name, location, description, price,
 * tags, image, cancellation policy, rules, max occupancy
 *
 * @param onBackClick  is invoked when the user clicks the back button
 */
@Composable
fun OwnerAddCampsiteScreen(
    onBackClick: () -> Unit
){
    var errorMessage by remember {
        mutableStateOf<String?>("")
    }
    var campsiteRepository = remember {
        CampsiteRepository()
    }
    var userRepository = remember {
        UserRepository()
    }
    var campsiteName by remember {
        mutableStateOf("")
    }
    var campsiteDescription by remember {
        mutableStateOf("")
    }
    var campsiteLocation by remember {
        mutableStateOf("")
    }
    var maxOccupancy by remember {
        mutableStateOf("")
    }
    var price by remember {
        mutableStateOf("")
    }
    var campsiteRules by remember {
        mutableStateOf("")
    }
    var campsiteCancellationPolicy by remember {
        mutableStateOf("")
    }
    var campsiteOwnerId by remember {
        mutableStateOf("")
    }
    var promoImageId by remember {
        mutableStateOf("")
    }
    var campsiteTags by remember {
        mutableStateOf("")
    }
    var isLoading by remember {
        mutableStateOf(true)
    }

    // Gets the campsite owners user id for the creation of the campsite
    LaunchedEffect(Unit) {
        try {
            userRepository.getOrCreateCurrentUser(
                onSuccess = { user ->
                    campsiteOwnerId = user.id
                    isLoading = false
                },
                onFailure = { error ->
                    errorMessage = error.message
                    isLoading = false
                }
            )
        } catch (e:Exception){
                errorMessage = "Initialization Crash ${e.message}"
                isLoading = false

        }
    }

    // Creates what is actually shown on screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            // Found out about the verticalScroll element from Google Gemini, "In Android Studio kotlin
            // how can I make it so I can scroll down a column of outlined text fields" November 1, 2026
            // More info in the doc folder in the file "SethCitation.txt"
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Back button and text at the top of the screen
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Button(
                onClick = onBackClick
            ) {
                Text("Back")
            }

            Text("Add Campsite")
        }

        // A input field for the user to enter the name of the campsite
        OutlinedTextField(
            value = campsiteName,
            onValueChange = {campsiteName = it},
            label = {Text("Enter Campsite Name")},
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for the user to enter the location of the campsite
        OutlinedTextField(
            value = campsiteLocation,
            onValueChange = {campsiteLocation = it},
            label = {Text("Enter Campsite Location")},
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for the user to enter the description
        OutlinedTextField(
            value = campsiteDescription,
            onValueChange = {campsiteDescription = it},
            label = {Text("Enter Campsite Description")},
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for the user to enter the maximum occupants
        OutlinedTextField(
            value = maxOccupancy,
            onValueChange = {input ->
                if (input.all {it.isDigit() }) {
                    maxOccupancy = input
                }
            },
            label = {Text("Enter Maximum Occupants")},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for the user to enter the price
        OutlinedTextField(
            value = price,
            onValueChange = {input ->
                if (input.all {it.isDigit() }) {
                    price = input
                }
            },
            label = {Text("Enter Campsite Price (in Cents Ex 1000 for $10")},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for the user to enter the rules
        OutlinedTextField(
            value = campsiteRules,
            onValueChange = {campsiteRules = it},
            label = {Text("Enter Campsite Rules")},
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for the user to enter the cancellation policy
        OutlinedTextField(
            value = campsiteCancellationPolicy,
            onValueChange = {campsiteCancellationPolicy = it},
            label = {Text("Enter Campsite Cancellation Policy")},
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for the user to enter a promotional image
        OutlinedTextField(
            value = promoImageId,
            onValueChange = { promoImageId = it },
            label = { Text("Enter Promo Image ID or URL") },
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for the user to enter tags for the campsite
        OutlinedTextField(
            value = campsiteTags,
            onValueChange = { campsiteTags = it },
            label = { Text("Enter Tags (comma-separated, Ex wifi, tent)") },
            modifier = Modifier.fillMaxWidth()
        )
        // Create button that is used once everything is inputted
        Button(
            onClick = {
                val campsiteInfo = Campsite(
                    name = campsiteName,
                    locationName = campsiteLocation,
                    description = campsiteDescription,
                    rules = campsiteRules,
                    cancellationPolicy = campsiteCancellationPolicy,
                    status = "draft",
                    currency = "CAD",
                    maxOccupancy = maxOccupancy.toLongOrNull() ?: 1L,
                    pricePerNightCents = price.toLongOrNull() ?: 0L,
                    policyVersion = 1L,
                    tags = campsiteTags
                        .split(",")
                        .map{it.trim()}
                        .filter{it.isNotEmpty()},
                    coordinates = null,
                    promoImageId = promoImageId.ifBlank {null},
                    id = "",
                    ownerId = campsiteOwnerId,
                    ratingSum = 0L,
                    ratingCount = 0L,
                    createdAt = null,
                    updatedAt = null
                )
                // Calls the function that creates and sends the campsite info to the DB
                campsiteRepository.createCampsite(
                    campsite = campsiteInfo,
                    onSuccess = {
                        errorMessage = null
                        onBackClick()
                    },
                    onFailure = {
                        errorMessage = "Failed to create campsite"
                    }
                )
            }
        ){
            Text("Create Campsite")
        }
    }
}