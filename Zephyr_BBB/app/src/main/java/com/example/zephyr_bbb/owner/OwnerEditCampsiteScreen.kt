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


/**
 * A composable function that creates a screen for users with owner permissions to use to edit their
 * existing campsite objects
 * has a multiple text input fields that allow the user to input or change the information that is
 * on their campsite listing.
 *
 * @param campsiteEditId The unique id of the campsite that the user wants to edit
 * @param onBackClick is invoked when the user clicks the back button
 */
@Composable
fun OwnerEditCampsiteScreen(
    campsiteEditId: String,
    onBackClick: () -> Unit
){
    val campsiteRepository = remember{
        CampsiteRepository()
    }
    var editName by remember{
        mutableStateOf("")
    }
    var editLocation by remember{
        mutableStateOf("")
    }
    var editDescription by remember{
        mutableStateOf("")
    }
    var editPrice by remember{
        mutableStateOf("")
    }
    var editOccupancy by remember{
        mutableStateOf("")
    }
    var editRules by remember{
        mutableStateOf("")
    }
    var editPolicy by remember{
        mutableStateOf("")
    }
    var editImage by remember{
        mutableStateOf("")
    }
    var editTags by remember{
        mutableStateOf("")
    }
    var isLoading by remember{
        mutableStateOf(true)
    }
    var errorMessage by remember{
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(campsiteEditId){
        campsiteRepository.getCampsite(
            campsiteId = campsiteEditId,
            onSuccess = {campsite->
                if (campsite != null){
                    editName = campsite.name
                    editLocation = campsite.locationName
                    editDescription = campsite.description
                    editOccupancy = campsite.maxOccupancy.toString()
                    editPrice = campsite.pricePerNightCents.toString()
                    editRules = campsite.rules
                    editPolicy = campsite.cancellationPolicy
                    editImage = campsite.promoImageId ?: ""
                    editTags = campsite.tags.joinToString(", ")
                }
                isLoading = false
            },
            onFailure = {error->
                errorMessage = error.message
                isLoading = false
            }
        )
    }
    // Creates the column which everything sits in
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ){
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    onBackClick()
                }
            ) {
                Text("Back")
            }

            Text("Edit Campsite")
        }
        // A input field for editing the name
        OutlinedTextField(
            value = editName,
            onValueChange = {editName = it},
            label = {Text("Campsite Name")},
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for editing the location
        OutlinedTextField(
            value = editLocation,
            onValueChange = {editLocation = it},
            label = {Text("Campsite Location")},
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for editing the description
        OutlinedTextField(
            value = editDescription,
            onValueChange = {editDescription = it},
            label = {Text("Campsite Description")},
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for editing the max occupancy
        OutlinedTextField(
            value = editOccupancy,
            onValueChange = {input->
                if (input.all{it.isDigit()})
                    editOccupancy = input
            },
            label = {Text("Maximum Occupancy")},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for editing the price
        OutlinedTextField(
            value = editPrice,
            onValueChange = {input->
                if (input.all{it.isDigit()})
                    editPrice = input
            },
            label = {Text("Enter Campsite Price (in Cents)")},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for editing the rules
        OutlinedTextField(
            value = editRules,
            onValueChange = {editRules = it},
            label = {Text("Campsite Rules")},
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for editing the cancellation policy
        OutlinedTextField(
            value = editPolicy,
            onValueChange = {editPolicy = it},
            label = {Text("Enter Campsite Cancellation Policy")},
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for editing the image
        OutlinedTextField(
            value = editImage,
            onValueChange = {editImage = it},
            label = {Text("Enter Promo Image ID or URL")},
            modifier = Modifier.fillMaxWidth()
        )
        // A input field for editing the tags
        OutlinedTextField(
            value = editTags,
            onValueChange = {editTags = it},
            label = {Text("Enter Tags (comma-separated, Ex wifi, tent)")},
            modifier = Modifier.fillMaxWidth()
        )
        // Button to save the changes
        Button(
            onClick = {
                // Makes a new campsite data object
                val campsiteUpdated = Campsite(
                    id = campsiteEditId,
                    name = editName,
                    locationName = editLocation,
                    description = editDescription,
                    maxOccupancy = editOccupancy.toLongOrNull() ?: 1L,
                    pricePerNightCents = editPrice.toLongOrNull() ?: 0L,
                    rules = editRules,
                    cancellationPolicy = editPolicy,
                    promoImageId = editImage.ifBlank { null },
                    tags = editTags
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() },
                )
                // Calls the function that updates campsite objects
                campsiteRepository.updateCampsite(
                    campsite = campsiteUpdated,
                    onSuccess = {
                        onBackClick()
                    },
                    onFailure = {error->
                        errorMessage = error.message
                    }
                )
            }
        ){
            Text("Save Changes")
        }
    }
}
