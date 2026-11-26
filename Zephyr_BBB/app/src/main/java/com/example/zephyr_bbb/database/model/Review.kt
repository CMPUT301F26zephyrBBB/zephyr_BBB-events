package com.example.zephyr_bbb.database.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Stores an entrant's written campsite review.
 * Path: campsites/{campsiteId}/reviews/{entrantId}
 */
data class Review(
    @get:DocumentId
    var id: String = "",

    var entrantId: String = "",
    var text: String = "",

    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)