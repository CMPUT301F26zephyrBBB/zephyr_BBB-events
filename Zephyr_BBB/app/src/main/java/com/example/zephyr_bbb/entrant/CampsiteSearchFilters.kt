package com.example.zephyr_bbb.entrant

import com.example.zephyr_bbb.database.model.Campsite
import java.util.Locale

// Holds the entrant's search filters, the checks done before they reach CampsiteRepository,
// and the small helpers that turn a Campsite into the text shown on a search result.
// Outstanding issues: amenities are comapred to the free text tags an owner typed, so
// "fire pit" and "firepit" are different amenities

/**
 * The filters an entrant can apply when searching for campsites.
 * Dates use YYYY-MM-DD, with the check-out night excluded.
 *
 * @property searchText free text matched against name, description and tags
 * @property location text matched against the campsite's location name
 * @property amenities comma separated amenities that every result must have as a tag
 * @property guests number of guests that must fit in the campsite
 * @property checkInDate first night of the stay, or null for no date filter
 * @property checkOutDate day the guests leave, or null for no date filter
 */
data class CampsiteSearchFilters(
    val searchText: String = "",
    val location: String = "",
    val amenities: String = "",
    val guests: Long = 1,
    val checkInDate: String? = null,
    val checkOutDate: String? = null
) {
    /**
     * Checks the filters before they are sent to the repository.
     *
     * @return an error message to show the user, or null if the filters are valid
     */
    fun validate(): String? {
        if (guests < 1) return "Guests must be at least 1"
        if ((checkInDate == null) != (checkOutDate == null)) return "Enter both dates or neither"
        if (checkInDate != null && checkOutDate != null) {
            val checkIn = isoDateToMillis(checkInDate)
            val checkOut = isoDateToMillis(checkOutDate)
            if (checkIn == null || checkOut == null) return "Dates must use YYYY-MM-DD"
            if (checkOut <= checkIn) return "Check-out must be after check-in"
        }
        return null
    }

    /**
     * How many filters from the filter menu are active (location, amenities, dates, guests).
     * The search text is not counted because it has its own search bar.
     */
    fun activeFilterCount(): Int {
        var count = 0
        if (location.isNotBlank()) count++
        if (amenities.isNotBlank()) count++
        if (checkInDate != null && checkOutDate != null) count++
        if (guests > 1) count++
        return count
    }

    /**
     * Short description of the active filters, ex:
     * "Jasper, AB · 2026-10-25 to 2026-10-27 · 4 guests".
     * Returns an empty string when nothing is filtered.
     */
    fun summary(): String {
        val parts = mutableListOf<String>()
        if (searchText.isNotBlank()) parts.add(searchText.trim())
        if (location.isNotBlank()) parts.add(location.trim())
        if (amenities.isNotBlank()) parts.add(amenities.trim())
        if (checkInDate != null && checkOutDate != null) parts.add("$checkInDate to $checkOutDate")
        if (guests > 1) parts.add("$guests guests")
        return parts.joinToString(" · ")
    }
}

/**
 * Average rating of this campsite out of 5.
 *
 * @return the average, or null when nobody has rated the campsite yet
 */
fun Campsite.averageRating(): Double? =
    if (ratingCount == 0L) null else ratingSum.toDouble() / ratingCount

/**
 * Rating text for a search result, for example "★ 4.6 (38 reviews)" or "No ratings yet".
 */
fun Campsite.ratingSummary(): String {
    val average = averageRating() ?: return "No ratings yet"
    val reviews = if (ratingCount == 1L) "1 review" else "$ratingCount reviews"
    return "★ ${String.format(Locale.US, "%.1f", average)} ($reviews)"
}

/**
 * Whether this campsite has every amenity the entrant asked for.
 * Each comma separated amenity must be contained in at least one of the campsite's tags
 * An empty request matches every campsite.
 *
 * @param amenities comma separated amenities, for example "fire pit, potable water"
 */
fun Campsite.matchesAmenities(amenities: String): Boolean {
    val wanted = amenities.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return wanted.all { amenity -> tags.any { it.contains(amenity, ignoreCase = true) } }
}

/**
 * Text shown above the search results, for example "3 campsites found".
 */
fun resultCountText(count: Int): String =
    if (count == 1) "1 campsite found" else "$count campsites found"
