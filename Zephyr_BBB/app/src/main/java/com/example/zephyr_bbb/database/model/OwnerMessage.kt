package com.example.zephyr_bbb.database.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Stores a message between a campsite owner and an entrant
 * regarding their reservation.
 * Path: messages/{messageId}
 */
data class OwnerMessage(
    @get:DocumentId
    var id: String = "",

    var senderId: String = "",
    var recipientId: String = "",
    var reservationId: String = "",

    var body: String = "",
    var readAt: Timestamp? = null,

    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)