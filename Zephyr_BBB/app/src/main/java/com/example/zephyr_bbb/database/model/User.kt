package com.example.zephyr_bbb.database.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Stores a user's profile and roles.
 * The document ID matches the Firebase Authentication UID.
 */
data class User(
    @DocumentId
    var id: String = "",
    var name: String = "",
    var email: String = "",
    var phone: String = "",
    var roles: List<String> = listOf("entrant"),
    var profileImageId: String? = null,
    var notificationsEnabled: Boolean = true,
    var status: String = "active",
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)