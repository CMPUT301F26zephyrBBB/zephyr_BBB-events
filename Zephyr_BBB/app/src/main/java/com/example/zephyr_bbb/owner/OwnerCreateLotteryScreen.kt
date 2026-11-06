package com.example.zephyr_bbb.owner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.repository.CampsiteRepository
import com.example.zephyr_bbb.database.repository.LotteryRepository
import com.example.zephyr_bbb.database.repository.UserRepository

/**
 * A composable function that creates the screen owners see when they click Create Lottery,
 * either from My Lotteries or from a campsite's details.
 * Publishes the lottery with LotteryRepository.createLottery.
 *
 * @param preselectedCampsiteId A campsite to tick before the form opens, or null for none
 * @param onBackClick is invoked when the user clicks the back button, or once the lottery is published
 */
@Composable
fun OwnerCreateLotteryScreen(
    preselectedCampsiteId: String? = null,
    onBackClick: () -> Unit
) {
    val userRepository = remember {
        UserRepository()
    }
    val campsiteRepository = remember {
        CampsiteRepository()
    }
    val lotteryRepository = remember {
        LotteryRepository()
    }
    var fields by remember {
        mutableStateOf(
            LotteryFormFields(
//                campsiteIds = setOfNotNull(preselectedCampsiteId?.ifBlank { null })
            )
        )
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }
    var isSubmitting by remember {
        mutableStateOf(false)
    }

    LotteryForm(
        heading = "Create Lottery",
        fields = fields,
//        campsites = campsites,
        submitLabel = "Publish",
        errorMessage = errorMessage,
        isSubmitting = isSubmitting,
        onFieldsChange = { fields = it },
        onSubmit = {
            isSubmitting = true
            errorMessage = null

            try {
                // Calls the function that creates and sends the lottery to the DB
                lotteryRepository.createLottery(
                    lottery = fields.toLottery(),
                    onSuccess = {
                        isSubmitting = false
                        onBackClick()
                    },
                    onFailure = { error ->
                        errorMessage = error.message ?: "Failed to create lottery"
                        isSubmitting = false
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to create lottery"
                isSubmitting = false
            }
        },
        onBackClick = onBackClick
    )
}