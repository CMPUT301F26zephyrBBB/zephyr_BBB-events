package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.AvailabilityDay
import com.example.zephyr_bbb.database.model.Campsite
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Reads and updates nightly campsite availability.
 *
 * Path: campsites/{campsiteId}/availability/{YYYY-MM-DD}
 * Missing dates are unavailable.
 * ReservationRepository handles reserving nights after a lottery win.
 */
class AvailabilityRepository {

    private val auth = FirebaseProvider.auth
    private val db = FirebaseProvider.db

    private fun availabilityRef(campsiteId: String) =
        db.collection(Collections.CAMPSITES)
            .document(campsiteId)
            .collection(Collections.AVAILABILITY)

    /**
     * Returns the campsite's configured nights in date order.
     */
    fun getAvailability(
        campsiteId: String,
        onSuccess: (List<AvailabilityDay>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        availabilityRef(campsiteId)
            .orderBy("date")
            .get()
            .addOnSuccessListener { snapshot ->
                val days = try {
                    snapshot.toObjects(AvailabilityDay::class.java)
                } catch (error: Exception) {
                    onFailure(error)
                    return@addOnSuccessListener
                }

                onSuccess(days)
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    /**
     * Sets one night to "open" or "blocked".
     * Dates must use YYYY-MM-DD.
     */
    fun setAvailability(
        campsiteId: String,
        date: String,
        status: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        setAvailabilityForDates(
            campsiteId = campsiteId,
            dates = listOf(date),
            status = status,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    /**
     * Sets multiple selected nights to "open" or "blocked".
     *
     * Only the campsite owner can change availability.
     * Already-reserved nights cannot be overwritten.
     * Either all selected nights are updated or none are.
     */
    fun setAvailabilityForDates(
        campsiteId: String,
        dates: List<String>,
        status: String,
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

        if (status !in listOf("open", "blocked")) {
            onFailure(
                IllegalArgumentException(
                    "Status must be open or blocked."
                )
            )
            return
        }

        val selectedDates = dates.distinct()

        try {
            require(selectedDates.isNotEmpty()) {
                "Select at least one date."
            }

            selectedDates.forEach { date ->
                validateDate(date)
            }
        } catch (error: Exception) {
            onFailure(error)
            return
        }

        val campsiteRef = db.collection(Collections.CAMPSITES)
            .document(campsiteId)

        val dayRefs = selectedDates.map { date ->
            availabilityRef(campsiteId).document(date)
        }

        db.runTransaction { transaction ->
            val campsite = transaction.get(campsiteRef)
                .toObject(Campsite::class.java)
                ?: throw IllegalStateException(
                    "Campsite was not found."
                )

            if (campsite.ownerId != uid) {
                throw IllegalStateException(
                    "Only the owner can edit availability."
                )
            }

            // Firestore transactions must finish reads before writes.
            val existingDays = dayRefs.map { ref ->
                transaction.get(ref)
                    .toObject(AvailabilityDay::class.java)
            }

            if (existingDays.any { it?.status == "reserved" }) {
                throw IllegalStateException(
                    "One or more selected nights are already reserved."
                )
            }

            val now = Timestamp.now()

            dayRefs.forEachIndexed { index, ref ->
                val existing = existingDays[index]

                val day = AvailabilityDay(
                    date = selectedDates[index],
                    status = status,
                    reservationId = null,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )

                transaction.set(ref, day)
            }

            true
        }
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    private fun validateDate(date: String) {
        val format = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.ROOT
        ).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val parsed = format.parse(date)
            ?: throw IllegalArgumentException("Invalid date.")

        require(format.format(parsed) == date) {
            "Dates must use YYYY-MM-DD."
        }
    }
}