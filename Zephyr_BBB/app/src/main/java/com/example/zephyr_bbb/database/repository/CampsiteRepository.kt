package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.Campsite
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Handles campsite creation, editing, searching and viewing
 * Saved campsites are immediately included in searches.
 * Availability records are managed by AvailabilityRepository.
 */
class CampsiteRepository {

    private val auth = FirebaseProvider.auth
    private val db = FirebaseProvider.db
    private val campsitesRef = db.collection(Collections.CAMPSITES)

    /**
     * Creates a campsite owned by the current user.
     * Returns the generated campsite ID.
     */
    fun createCampsite(
        campsite: Campsite,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure(
                IllegalStateException("Sign in first.")
            )
            return
        }

        if (!validDetails(campsite)) {
            onFailure(
                IllegalArgumentException(
                    "Enter a name, positive occupancy and non-negative price."
                )
            )
            return
        }

        val newCampsite = campsite.copy(
            id = "",
            ownerId = uid,
            name = campsite.name.trim(),
            locationName = campsite.locationName.trim(),
            ratingSum = 0,
            ratingCount = 0,
            createdAt = null,
            updatedAt = null
        )

        campsitesRef.add(newCampsite)
            .addOnSuccessListener { document ->
                onSuccess(document.id)
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    /**
     * Reads campsite details by ID.
     * A QR scanner can pass the scanned campsite ID here.
     * Returns null if the campsite does not exist.
     */
    fun getCampsite(
        campsiteId: String,
        onSuccess: (Campsite?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        campsitesRef.document(campsiteId)
            .get()
            .addOnSuccessListener { snapshot ->
                val campsite = try {
                    snapshot.toObject(Campsite::class.java)
                } catch (error: Exception) {
                    onFailure(error)
                    return@addOnSuccessListener
                }

                onSuccess(campsite)
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    /**
     * Lists campsites belonging to the current user.
     */
    fun getMyCampsites(
        onSuccess: (List<Campsite>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure(
                IllegalStateException("Sign in first.")
            )
            return
        }

        readCampsites(
            query = campsitesRef.whereEqualTo("ownerId", uid),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    /**
     * Searches campsites by text, location and guest count.
     * Providing both dates also filters by nightly availability.
     * Dates use YYYY-MM-DD, with checkout excluded.
     * Leaving the filters empty returns all campsites.
     */
    fun searchCampsites(
        searchText: String = "",
        location: String = "",
        guests: Long = 1,
        checkInDate: String? = null,
        checkOutDate: String? = null,
        onSuccess: (List<Campsite>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (guests < 1) {
            onFailure(
                IllegalArgumentException(
                    "Guest count must be at least one."
                )
            )
            return
        }

        if ((checkInDate == null) != (checkOutDate == null)) {
            onFailure(
                IllegalArgumentException(
                    "Provide both check-in and checkout dates."
                )
            )
            return
        }

        val nightCount = try {
            if (checkInDate != null && checkOutDate != null) {
                countNights(checkInDate, checkOutDate)
            } else {
                null
            }
        } catch (error: Exception) {
            onFailure(error)
            return
        }

        val text = searchText.trim()
        val locationText = location.trim()

        readCampsites(
            query = campsitesRef,
            onSuccess = { campsites ->
                val matches = campsites.filter { campsite ->
                    val matchesText =
                        text.isBlank() ||
                                campsite.name.contains(text, true) ||
                                campsite.description.contains(text, true) ||
                                campsite.tags.any {
                                    it.contains(text, true)
                                }

                    val matchesLocation =
                        campsite.locationName.contains(
                            locationText,
                            ignoreCase = true
                        )

                    val matchesGuests =
                        campsite.maxOccupancy >= guests

                    matchesText && matchesLocation && matchesGuests
                }

                if (
                    checkInDate == null ||
                    checkOutDate == null ||
                    nightCount == null ||
                    matches.isEmpty()
                ) {
                    onSuccess(matches)
                } else {
                    filterByAvailability(
                        campsites = matches,
                        checkInDate = checkInDate,
                        checkOutDate = checkOutDate,
                        nightCount = nightCount,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                }
            },
            onFailure = onFailure
        )
    }

    /**
     * Updates an existing campsite's details.
     * Preserves ownership, rating totals and creation time.
     */
    fun updateCampsite(
        campsite: Campsite,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure(
                IllegalStateException("Sign in first.")
            )
            return
        }

        if (campsite.id.isBlank() || !validDetails(campsite)) {
            onFailure(
                IllegalArgumentException(
                    "Provide a campsite ID and valid details."
                )
            )
            return
        }

        val campsiteRef = campsitesRef.document(campsite.id)

        db.runTransaction { transaction ->
            val existing = transaction.get(campsiteRef)
                .toObject(Campsite::class.java)
                ?: throw IllegalStateException(
                    "Campsite was not found."
                )

            if (existing.ownerId != uid) {
                throw IllegalStateException(
                    "Only the owner can edit this campsite."
                )
            }

            transaction.update(
                campsiteRef,
                mapOf(
                    "name" to campsite.name.trim(),
                    "description" to campsite.description.trim(),
                    "locationName" to campsite.locationName.trim(),
                    "coordinates" to campsite.coordinates,
                    "maxOccupancy" to campsite.maxOccupancy,
                    "pricePerNightCents" to campsite.pricePerNightCents,
                    "currency" to campsite.currency,
                    "tags" to campsite.tags,
                    "rules" to campsite.rules,
                    "cancellationPolicy" to campsite.cancellationPolicy,
                    "promoImageId" to campsite.promoImageId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            true
        }
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    private fun validDetails(campsite: Campsite): Boolean {
        return campsite.name.isNotBlank() &&
                campsite.maxOccupancy > 0 &&
                campsite.pricePerNightCents >= 0
    }

    private fun readCampsites(
        query: Query,
        onSuccess: (List<Campsite>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        query.get()
            .addOnSuccessListener { snapshot ->
                val campsites = try {
                    snapshot.toObjects(Campsite::class.java)
                } catch (error: Exception) {
                    onFailure(error)
                    return@addOnSuccessListener
                }

                onSuccess(
                    campsites.sortedBy {
                        it.name.lowercase(Locale.ROOT)
                    }
                )
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    private fun countNights(
        checkInDate: String,
        checkOutDate: String
    ): Long {
        val format = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.ROOT
        ).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val start = format.parse(checkInDate)
            ?: throw IllegalArgumentException(
                "Invalid check-in date."
            )

        val end = format.parse(checkOutDate)
            ?: throw IllegalArgumentException(
                "Invalid checkout date."
            )

        require(
            format.format(start) == checkInDate &&
                    format.format(end) == checkOutDate
        ) {
            "Dates must use YYYY-MM-DD."
        }

        require(end.after(start)) {
            "Checkout must be after check-in."
        }

        return (end.time - start.time) / 86_400_000L
    }

    private fun filterByAvailability(
        campsites: List<Campsite>,
        checkInDate: String,
        checkOutDate: String,
        nightCount: Long,
        onSuccess: (List<Campsite>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val calendarTasks = campsites.map { campsite ->
            campsitesRef.document(campsite.id)
                .collection(Collections.AVAILABILITY)
                .get()
        }

        Tasks.whenAllSuccess<QuerySnapshot>(calendarTasks)
            .addOnSuccessListener { calendars ->
                val available = campsites.filterIndexed { index, _ ->
                    val openDates = calendars[index].documents
                        .filter { day ->
                            day.getString("status") == "open" &&
                                    day.id >= checkInDate &&
                                    day.id < checkOutDate
                        }
                        .map { it.id }
                        .toSet()

                    openDates.size.toLong() == nightCount
                }

                onSuccess(available)
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }
}