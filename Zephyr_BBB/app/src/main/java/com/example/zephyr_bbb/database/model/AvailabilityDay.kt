package com.example.zephyr_bbb.database.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Stores availability for one campsite night.
 * Path: campsites/{campsiteId}/availability/{date}
 * Dates use YYYY-MM-DD
 * 3 statuses: open , blocked , reserved
 */
data class AvailabilityDay(
    @get:DocumentId
    var id: String = "",

    var date: String = "",
    var status: String = "open",
    var reservationId: String? = null,

    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)