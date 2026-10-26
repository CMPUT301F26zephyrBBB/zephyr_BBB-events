package com.example.zephyr_bbb.database.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
/**
 * Stores a lottery offering one or more campsites.
 * Registration and booking have separate time windows.
 * Stay dates use YYYY-MM-DD, with the end date excluded.
 */
data class Lottery(
    @get:DocumentId
    var id: String = "",

    var ownerId: String = "",
    var title: String = "",
    var campsiteIds: List<String> = emptyList(),

    var registrationOpensAt: Timestamp? = null,
    var registrationClosesAt: Timestamp? = null,

    var bookingOpensAt: Timestamp? = null,
    var bookingClosesAt: Timestamp? = null,

    var stayStartDate: String = "",
    var stayEndDate: String = "",

    var winnerCount: Long = 1,
    var waitlistCapacity: Long? = null,
    var entryCount: Long = 0,

    var status: String = "draft",
    var drawnAt: Timestamp? = null,
    @get:ServerTimestamp
    var createdAt: Timestamp? = null,
    @get:ServerTimestamp
    var updatedAt: Timestamp? = null
)