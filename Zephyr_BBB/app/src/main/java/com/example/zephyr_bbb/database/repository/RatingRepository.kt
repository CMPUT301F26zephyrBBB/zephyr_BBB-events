package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.Rating
import com.google.firebase.Timestamp

/**
 * Saves campsite ratings and maintains the campsite's rating totals.
 * Ratings: campsites/{campsiteId}/ratings/{entrantId}
 * Call after sign-in.
 */
class RatingRepository {

    private val db = FirebaseProvider.db
    private val campsitesRef = db.collection(Collections.CAMPSITES)

    /**
     * Creates or updates the current user's 1–5 campsite rating.
     * Updates the campsite's ratingSum and ratingCount together.
     */
    fun rateCampsite(
        campsiteId: String,
        score: Long,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (score !in 1L..5L) {
            onFailure(
                IllegalArgumentException(
                    "Choose a rating from 1 to 5."
                )
            )
            return
        }

        val uid = FirebaseProvider.requireUid()
        val campsiteRef = campsitesRef.document(campsiteId)

        val ratingRef = campsiteRef
            .collection(Collections.RATINGS)
            .document(uid)

        db.runTransaction { transaction ->
            val campsite = transaction.get(campsiteRef)
                .toObject(Campsite::class.java)!!

            val existing = transaction.get(ratingRef)
                .toObject(Rating::class.java)

            val now = Timestamp.now()

            val rating = Rating(
                entrantId = uid,
                score = score,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )

            val newSum =
                campsite.ratingSum - (existing?.score ?: 0L) + score

            val newCount =
                campsite.ratingCount + if (existing == null) 1L else 0L

            transaction.set(ratingRef, rating)

            transaction.update(
                campsiteRef,
                mapOf(
                    "ratingSum" to newSum,
                    "ratingCount" to newCount,
                    "updatedAt" to now
                )
            )

            true
        }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Reads the current user's rating for a campsite.
     * Returns null if they have not rated it.
     */
    fun getMyRating(
        campsiteId: String,
        onSuccess: (Rating?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        campsitesRef.document(campsiteId)
            .collection(Collections.RATINGS)
            .document(FirebaseProvider.requireUid())
            .get()
            .addOnSuccessListener {
                onSuccess(it.toObject(Rating::class.java))
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Returns the ratings for a campsite.
     */
    fun getRatings(
        campsiteId: String,
        onSuccess: (List<Rating>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        campsitesRef.document(campsiteId)
            .collection(Collections.RATINGS)
            .get()
            .addOnSuccessListener {
                onSuccess(it.toObjects(Rating::class.java))
            }
            .addOnFailureListener { onFailure(it) }
    }
}