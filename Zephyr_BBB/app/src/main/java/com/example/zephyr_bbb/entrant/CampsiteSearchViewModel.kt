package com.example.zephyr_bbb.entrant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.database.repository.CampsiteRepository
import com.example.zephyr_bbb.database.repository.LotteryRepository

// Holds the entrant search state so the search and filter screens only display it and forward
// events (keeps UI separate from logic)
// Outstanding issues: results are not paged, so a very large number of campsites loads at once,
// and the active lottery of each result is a separate query (one per site).

/**
 * State holder for searching campsites (01.01.01 to 01.01.04).
 * One instance is shared by the search screen and the filter screen, so the filters applied on
 * one screen are the ones the other screen searches with.
 *
 * @param campsiteRepository where campsites are searched
 * @param lotteryRepository where the open lottery of each result is looked up
 */
class CampsiteSearchViewModel(
    private val campsiteRepository: CampsiteRepository = CampsiteRepository(),
    private val lotteryRepository: LotteryRepository = LotteryRepository()
) : ViewModel() {

    /** The filters currently selected by the entrant. */
    var filters by mutableStateOf(CampsiteSearchFilters())
        private set

    /** Campsites matching [filters] from the last successful search. */
    var results by mutableStateOf<List<Campsite>>(emptyList())
        private set

    /**
     * The open lottery of each result, keyed by campsite ID.
     * A campsite that is missing from the map is still being looked up, and a null value means
     * it has no open lottery (or the lookup failed).
     */
    var activeLotteries by mutableStateOf<Map<String, Lottery?>>(emptyMap())
        private set

    /** True while a search is running. */
    var isLoading by mutableStateOf(false)
        private set

    /** Message to show the user when the filters are invalid or the search failed. */
    var error by mutableStateOf<String?>(null)
        private set

    // Used to ignore the answer of an older search if a newer one has been started since
    private var searchNumber = 0
    private var hasSearched = false

    /** Replaces the current filters. Does not search until [search] is called. */
    fun updateFilters(newFilters: CampsiteSearchFilters) {
        filters = newFilters
    }

    /** Resets all filters to their defaults. Does not search until [search] is called. */
    fun clearFilters() {
        filters = CampsiteSearchFilters()
    }

    /** Runs the first search, and does nothing if one has already been done. */
    fun searchIfNeeded() {
        if (!hasSearched) search()
    }

    /** Validates the filters, then loads the matching campsites into [results]. */
    fun search() {
        val problem = filters.validate()
        if (problem != null) {
            error = problem
            return
        }
        hasSearched = true
        error = null
        isLoading = true
        val thisSearch = ++searchNumber
        val searched = filters

        campsiteRepository.searchCampsites(
            searchText = searched.searchText,
            location = searched.location,
            guests = searched.guests,
            checkInDate = searched.checkInDate,
            checkOutDate = searched.checkOutDate,
            onSuccess = { campsites ->
                if (thisSearch == searchNumber) {
                    // The repository cant filter by amenity, so that is done here
                    val matches = campsites.filter { it.matchesAmenities(searched.amenities) }
                    results = matches
                    activeLotteries = emptyMap()
                    isLoading = false
                    loadActiveLotteries(matches, thisSearch)
                }
            },
            onFailure = { failure ->
                if (thisSearch == searchNumber) {
                    results = emptyList()
                    error = failure.message ?: "Could not load campsites"
                    isLoading = false
                }
            }
        )
    }

    // Looks up the open lottery of each campsite so the result card can show it
    private fun loadActiveLotteries(campsites: List<Campsite>, forSearch: Int) {
        campsites.forEach { campsite ->
            lotteryRepository.getActiveLotteriesForCampsite(
                campsiteId = campsite.id,
                onSuccess = { lotteries ->
                    if (forSearch == searchNumber) {
                        activeLotteries = activeLotteries + (campsite.id to lotteries.firstOrNull())
                    }
                },
                onFailure = {
                    if (forSearch == searchNumber) {
                        activeLotteries = activeLotteries + (campsite.id to null)
                    }
                }
            )
        }
    }
}
