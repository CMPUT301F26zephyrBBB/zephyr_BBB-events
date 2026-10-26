package com.example.zephyr_bbb.database.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Stores a campsite booking, separate from a lottery win.
 * Stay dates use YYYY-MM-DD; checkout is excluded.
 * Prices are stored in cents.
 */
data class Reservation(
    @get:DocumentId
    var id: String = "",

    var entrantId: String = "",
    var ownerId: String = "",
    var campsiteId: String = "",
    var lotteryId: String = "",

    var checkInDate: String = "",
    var checkOutDate: String = "",
    var guestCount: Long = 1,

    var totalPriceCents: Long = 0,
    var currency: String = "CAD",
    var status: String = "confirmed",

    var acceptedPolicyVersion: Long = 1,
    var policiesAcceptedAt: Timestamp? = null,

    var cancellationReason: String? = null,
    var cancelledAt: Timestamp? = null,

    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)