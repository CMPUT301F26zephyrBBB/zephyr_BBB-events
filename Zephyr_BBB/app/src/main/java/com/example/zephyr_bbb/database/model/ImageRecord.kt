package com.example.zephyr_bbb.database.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Stores image metadata for profiles, campsites and admin moderation.
 * The actual image file is stored separately.
 */
data class ImageRecord(
    @get:DocumentId
    var id: String = "",

    var uploaderId: String = "",
    var targetType: String = "profile",
    var targetId: String = "",

    var storagePath: String = "",
    var contentType: String = "image/jpeg",
    var status: String = "active",

    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)