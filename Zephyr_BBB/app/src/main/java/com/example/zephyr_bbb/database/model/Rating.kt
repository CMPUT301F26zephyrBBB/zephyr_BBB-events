package com.example.zephyr_bbb.database.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Stores an entrant's campsite rating linked to a reservation.
 * Path: campsites/{campsiteId}/ratings/{entrantId}
 */
data class Rating(
    @get:DocumentId
    var id: String = "",

    var entrantId: String = "",
    var reservationId: String = "",
    var score: Long = 1,

    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)