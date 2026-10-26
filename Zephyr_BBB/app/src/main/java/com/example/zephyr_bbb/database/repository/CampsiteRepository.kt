package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.Campsite

/**
 * Creates and reads campsite listings in Firestore.
 * Security rules enforce who can access each listing.
 */
class CampsiteRepository {

    private val campsitesRef =
        FirebaseProvider.db.collection(Collections.CAMPSITES)

    /**
     * Creates a draft owned by the signed-in user.
     * Returns the new document ID.
     */
    fun createCampsite(
        campsite: Campsite,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val newCampsite = campsite.copy(
            id = "",
            ownerId = FirebaseProvider.requireUid(),
            status = "draft",
            ratingSum = 0,
            ratingCount = 0,
            promoImageId = null,
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
     * Reads a campsite using its document ID.
     * Returns null if it does not exist and the read is permitted.
     */
    fun getCampsite(
        campsiteId: String,
        onSuccess: (Campsite?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        campsitesRef.document(campsiteId)
            .get()
            .addOnSuccessListener { document ->
                val campsite = try {
                    document.toObject(Campsite::class.java)
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
}