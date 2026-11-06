package com.example.zephyr_bbb.entrant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.database.model.LotteryEntry
import com.example.zephyr_bbb.database.repository.CampsiteRepository
import com.example.zephyr_bbb.database.repository.LotteryRepository

// Holds the state of the campsite details screen so the screen only displays it and forwards
// events (keeps UI separate from logic).
// Outstanding issues: after a join or leave the lottery is reloaded, so the entrant count on
// screen is only as fresh as that reload

/**
 * State holder for one campsite's details and its open lottery
 * (01.01.05, 01.01.06, 01.02.01, 01.02.02).
 * Create one per details screen so the state of one campsite is never shown for another
 *
 * @param campsiteRepository where the campsite is read
 * @param lotteryRepository where the open lottery, the entrant's entry, and joining and leaving
 * are handled
 */
class CampsiteDetailsViewModel(
    private val campsiteRepository: CampsiteRepository = CampsiteRepository(),
    private val lotteryRepository: LotteryRepository = LotteryRepository()
) : ViewModel() {

    /** The campsite being shown, or null while loading or if it could not be found. */
    var campsite by mutableStateOf<Campsite?>(null)
        private set

    /** The open lottery shown for the campsite, or null if it has none. */
    var lottery by mutableStateOf<Lottery?>(null)
        private set

    /** The entrant's own entry in [lottery], or null if they have not joined. */
    var entry by mutableStateOf<LotteryEntry?>(null)
        private set

    /** True while the campsite and lottery are being loaded. */
    var isLoading by mutableStateOf(false)
        private set

    /** True when the campsite ID did not match any campsite. */
    var notFound by mutableStateOf(false)
        private set

    /** Message to show when the campsite could not be loaded. */
    var loadError by mutableStateOf<String?>(null)
        private set

    /** True while a join or leave request is running. */
    var isWorking by mutableStateOf(false)
        private set

    /** Message to show when joining or leaving the waiting list failed. */
    var actionError by mutableStateOf<String?>(null)
        private set

    // Used to ignore the answer of an older load if a newer one has been started since
    private var loadNumber = 0
    private var campsiteId = ""

    /** The button the entrant should currently see for the lottery. */
    val joinAction: JoinAction
        get() = joinAction(lottery, entry)

    /**
     * Loads the campsite, its open lottery, and whether the entrant has joined that lottery.
     *
     * @param id the ID of the campsite to show, for example from a search result or a QR code
     */
    fun load(id: String) {
        campsiteId = id
        val thisLoad = ++loadNumber
        isLoading = true
        notFound = false
        loadError = null
        actionError = null

        campsiteRepository.getCampsite(
            campsiteId = id,
            onSuccess = { found ->
                if (thisLoad == loadNumber) {
                    campsite = found
                    if (found == null) {
                        notFound = true
                        isLoading = false
                    } else {
                        loadLottery(id, thisLoad)
                    }
                }
            },
            onFailure = { failure ->
                if (thisLoad == loadNumber) {
                    loadError = failure.message ?: "Could not load the campsite"
                    isLoading = false
                }
            }
        )
    }

    // The lottery is optional so a campsite without one is still shown, so a failure here is not
    // treated as the whole screen failing
    private fun loadLottery(id: String, forLoad: Int) {
        lotteryRepository.getActiveLotteriesForCampsite(
            campsiteId = id,
            onSuccess = { lotteries ->
                if (forLoad == loadNumber) {
                    val chosen = chooseLottery(lotteries)
                    lottery = chosen
                    if (chosen == null) {
                        entry = null
                        isLoading = false
                    } else {
                        loadEntry(chosen.id, forLoad)
                    }
                }
            },
            onFailure = {
                if (forLoad == loadNumber) {
                    lottery = null
                    entry = null
                    isLoading = false
                }
            }
        )
    }

    private fun loadEntry(lotteryId: String, forLoad: Int) {
        attempt {
            lotteryRepository.getMyEntry(
                lotteryId = lotteryId,
                onSuccess = { found ->
                    if (forLoad == loadNumber) {
                        entry = found
                        isLoading = false
                    }
                },
                onFailure = {
                    if (forLoad == loadNumber) {
                        entry = null
                        isLoading = false
                    }
                }
            )
        }
    }

    /** Joins the open lottery's waiting list, then reloads so the screen shows the new state */
    fun joinLottery() {
        val current = lottery ?: return
        if (isWorking) return
        isWorking = true
        actionError = null
        attempt {
            lotteryRepository.joinWaitlist(
                lotteryId = current.id,
                onSuccess = {
                    isWorking = false
                    load(campsiteId)
                },
                onFailure = { failure ->
                    isWorking = false
                    actionError = failure.message ?: "Could not join the lottery"
                }
            )
        }
    }

    /** Leaves the open lottery's waiting list, then reloads so the screen shows the new state. */
    fun leaveLottery() {
        val current = lottery ?: return
        if (isWorking) return
        isWorking = true
        actionError = null
        attempt {
            lotteryRepository.leaveWaitlist(
                lotteryId = current.id,
                onSuccess = {
                    isWorking = false
                    load(campsiteId)
                },
                onFailure = { failure ->
                    isWorking = false
                    actionError = failure.message ?: "Could not leave the waiting list"
                }
            )
        }
    }

    // The repository throws right away (not through onFailure) if nobody is signed in yet
    private fun attempt(action: () -> Unit) {
        try {
            action()
        } catch (error: IllegalStateException) {
            isWorking = false
            actionError = error.message ?: "Sign in before joining a lottery"
        }
    }
}
