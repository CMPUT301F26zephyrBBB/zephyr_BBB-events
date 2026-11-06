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
// Outstanding issues: after a join or leave the lotteries are reloaded, so the entrant count on
// screen is only as fresh as that reload

/**
 * State holder for one campsite's details and its open lotteries
 * (01.01.05, 01.01.06, 01.02.01, 01.02.02).
 * Create one per details screen so the state of one campsite is never shown for another
 *
 * @param campsiteRepository where the campsite is read
 * @param lotteryRepository where the open lotteries, the entrant's entries, and joining and leaving
 * are handled
 */
class CampsiteDetailsViewModel(
    private val campsiteRepository: CampsiteRepository = CampsiteRepository(),
    private val lotteryRepository: LotteryRepository = LotteryRepository()
) : ViewModel() {

    /** The campsite being shown, or null while loading or if it could not be found. */
    var campsite by mutableStateOf<Campsite?>(null)
        private set

    /** The open lotteries of the campsite, the one closing registration first at the top. */
    var lotteries by mutableStateOf<List<Lottery>>(emptyList())
        private set

    /** The entrant's own entry in each lottery of [lotteries], keyed by lottery ID. */
    var entries by mutableStateOf<Map<String, LotteryEntry?>>(emptyMap())
        private set

    /** True while the campsite and its lotteries are being loaded. */
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

    /**
     * The button the entrant should currently see for one of the campsite's lotteries.
     *
     * @param lottery one of the lotteries in [lotteries]
     */
    fun joinActionFor(lottery: Lottery): JoinAction = joinAction(lottery, entries[lottery.id])

    /**
     * Loads the campsite, its open lotteries, and whether the entrant has joined each of them.
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
                        loadLotteries(id, thisLoad)
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

    // Lotteries are optional so a campsite without any is still shown, so a failure here is not
    // treated as the whole screen failing
    private fun loadLotteries(id: String, forLoad: Int) {
        lotteryRepository.getActiveLotteriesForCampsite(
            campsiteId = id,
            onSuccess = { found ->
                if (forLoad == loadNumber) {
                    val sorted = sortLotteries(found)
                    lotteries = sorted
                    loadEntries(sorted, forLoad)
                }
            },
            onFailure = {
                if (forLoad == loadNumber) {
                    lotteries = emptyList()
                    entries = emptyMap()
                    isLoading = false
                }
            }
        )
    }

    // Looks up the entrant's entry in every lottery, and finishes loading once all have answered
    private fun loadEntries(open: List<Lottery>, forLoad: Int) {
        if (open.isEmpty()) {
            entries = emptyMap()
            isLoading = false
            return
        }
        val loaded = mutableMapOf<String, LotteryEntry?>()
        fun answered(lotteryId: String, entry: LotteryEntry?) {
            if (forLoad != loadNumber) return
            loaded[lotteryId] = entry
            if (loaded.size == open.size) {
                entries = loaded.toMap()
                isLoading = false
            }
        }
        try {
            open.forEach { lottery ->
                lotteryRepository.getMyEntry(
                    lotteryId = lottery.id,
                    onSuccess = { answered(lottery.id, it) },
                    onFailure = { answered(lottery.id, null) }
                )
            }
        } catch (error: IllegalStateException) {
            // Nobody is signed in yet, so the entrant cannot have joined anything
            entries = emptyMap()
            isLoading = false
        }
    }

    /**
     * Joins the waiting list of one of the open lotteries, then reloads so the screen shows the
     * new state.
     *
     * @param lotteryId the ID of the lottery to join
     */
    fun joinLottery(lotteryId: String) {
        if (isWorking || lotteries.none { it.id == lotteryId }) return
        isWorking = true
        actionError = null
        attempt {
            lotteryRepository.joinWaitlist(
                lotteryId = lotteryId,
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

    /**
     * Leaves the waiting list of one of the open lotteries, then reloads so the screen shows the
     * new state.
     *
     * @param lotteryId the ID of the lottery to leave
     */
    fun leaveLottery(lotteryId: String) {
        if (isWorking || lotteries.none { it.id == lotteryId }) return
        isWorking = true
        actionError = null
        attempt {
            lotteryRepository.leaveWaitlist(
                lotteryId = lotteryId,
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
