package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.Review
import com.google.firebase.Timestamp

/**
 * Manages written campsite reviews.
 * Reviews: campsites/{campsiteId}/reviews/{entrantId}
 * Call after sign-in.
 */
class ReviewRepository {
    private val db = FirebaseProvider.db
    private val campsitesRef = db.collection(Collections.CAMPSITES)
    /**
     * Creates or updates the current user's campsite review.
     * Each entrant can have one review per campsite.
     */
    fun postReview(
        campsiteId: String,
        text: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val reviewText = text.trim()
        if (reviewText.isBlank() || reviewText.length > 2000) {
            onFailure(
                IllegalArgumentException(
                    "Review must contain 1 to 2000 characters."
                )
            )
            return
        }
        val uid = FirebaseProvider.requireUid()
        val campsiteRef = campsitesRef.document(campsiteId)

        val reviewRef = campsiteRef
            .collection(Collections.REVIEWS)
            .document(uid)

        db.runTransaction { transaction ->

            check(transaction.get(campsiteRef).exists()) {
                "Campsite does not exist."
            }
            val existing = transaction.get(reviewRef)
                .toObject(Review::class.java)
            val now = Timestamp.now()
            val review = Review(
                entrantId = uid,
                text = reviewText,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            transaction.set(reviewRef, review)
            true
        }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Returns the current user's review for a campsite.
     * Returns null if they have not posted a review.
     */
    fun getMyReview(
        campsiteId: String,
        onSuccess: (Review?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        campsitesRef.document(campsiteId)
            .collection(Collections.REVIEWS)
            .document(FirebaseProvider.requireUid())
            .get()
            .addOnSuccessListener {
                onSuccess(it.toObject(Review::class.java))
            }
            .addOnFailureListener { onFailure(it) }
    }
    /**
     * Returns all written reviews for a campsite.
     */
    fun getReviews(
        campsiteId: String,
        onSuccess: (List<Review>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        campsitesRef.document(campsiteId)
            .collection(Collections.REVIEWS)
            .get()
            .addOnSuccessListener {
                onSuccess(it.toObjects(Review::class.java))
            }
            .addOnFailureListener { onFailure(it) }
    }
}