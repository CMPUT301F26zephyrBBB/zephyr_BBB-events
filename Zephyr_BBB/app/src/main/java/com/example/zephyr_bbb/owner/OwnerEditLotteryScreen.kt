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
 * A composable function that creates a screen for owners to edit one of their lotteries.
 * The form is filled with the lottery's current values. Changes are saved with
 * LotteryRepository.updateLottery, which only allows edits before winners are drawn and won't
 * let the waitlist capacity drop below the number of people already on it.
 *
 * @param lotteryEditId The unique id of the lottery that the user wants to edit
 * @param onBackClick is invoked when the user clicks the back button, or once the changes are saved
 */
@Composable
fun OwnerEditLotteryScreen(
    lotteryEditId: String,
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
        mutableStateOf(LotteryFormFields())
    }
    var campsites by remember {
        mutableStateOf<List<Campsite>>(emptyList())
    }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }
    var isSubmitting by remember {
        mutableStateOf(false)
    }
    var canEdit by remember {
        mutableStateOf(true)
    }

    // Signs in, then loads the lottery and the owner's campsites
    LaunchedEffect(lotteryEditId) {
        try {
            userRepository.getOrCreateCurrentUser(
                onSuccess = {
                    lotteryRepository.getLottery(
                        lotteryId = lotteryEditId,
                        onSuccess = { lottery ->
                            if (lottery == null) {
                                errorMessage = "This lottery could not be found."
                                canEdit = false
                            } else {
                                fields = lottery.toFormFields()
                                canEdit = canEditLottery(lottery.status)

                                if (!canEdit) {
                                    errorMessage = "Winners have been drawn, so this lottery can no longer be edited."
                                }
                            }
                        },
                        onFailure = { error ->
                            errorMessage = error.message
                        }
                    )
                    campsiteRepository.getMyCampsites(
                        onSuccess = { list ->
                            campsites = list
                        },
                        onFailure = { error ->
                            errorMessage = error.message
                        }
                    )
                },
                onFailure = { error ->
                    errorMessage = error.message
                }
            )
        } catch (e: Exception) {
            errorMessage = e.message
        }
    }

    LotteryForm(
        heading = "Edit Lottery",
        fields = fields,
        campsites = campsites,
        submitLabel = "Save Changes",
        errorMessage = errorMessage,
        isSubmitting = isSubmitting || !canEdit,
        onFieldsChange = { fields = it },
        onSubmit = {
            isSubmitting = true
            errorMessage = null

            try {
                // Calls the function that updates lottery objects
                lotteryRepository.updateLottery(
                    lottery = fields.toLottery(id = lotteryEditId),
                    onSuccess = {
                        isSubmitting = false
                        onBackClick()
                    },
                    onFailure = { error ->
                        errorMessage = error.message ?: "Failed to update lottery"
                        isSubmitting = false
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to update lottery"
                isSubmitting = false
            }
        },
        onBackClick = onBackClick
    )
}
