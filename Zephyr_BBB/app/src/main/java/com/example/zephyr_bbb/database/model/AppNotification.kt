package com.example.zephyr_bbb.database.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Stores a user's notification and its delivery/read status.
 * Path: notifications/{notificationId}
 * Saving this record alone does not send a push notification.
 */
data class AppNotification(
    @get:DocumentId
    var id: String = "",

    var senderId: String = "",
    var recipientId: String = "",

    var type: String = "general",
    var title: String = "",
    var body: String = "",

    var lotteryId: String? = null,
    var reservationId: String? = null,

    var readAt: Timestamp? = null,
    var deliveryStatus: String = "pending",

    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)