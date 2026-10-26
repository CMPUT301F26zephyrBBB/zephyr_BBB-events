package com.example.zephyr_bbb.database.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Stores an owner's message to an entrant about a reservation.
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