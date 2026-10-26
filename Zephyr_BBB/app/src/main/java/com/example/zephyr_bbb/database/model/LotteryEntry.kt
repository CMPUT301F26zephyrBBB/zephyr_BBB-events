package com.example.zephyr_bbb.database.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Stores one entrant's participation in a lottery.
 * Path: lotteries/{lotteryId}/entries/{entrantId}
 */
data class LotteryEntry(
    @get:DocumentId
    var id: String = "",

    var entrantId: String = "",
    var lotteryId: String = "",
    var status: String = "waiting",

    var joinedAt: Timestamp? = null,
    var selectedAt: Timestamp? = null,
    var respondedAt: Timestamp? = null,

    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)