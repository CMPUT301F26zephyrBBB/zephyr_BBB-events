package com.example.zephyr_bbb.database.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp

/**
 * Stores a campsite's details.
 * Prices are stored in cents: 6500 means $65.00.
 */
data class Campsite(
    @get:DocumentId
    var id: String = "",

    var ownerId: String = "",
    var name: String = "",
    var description: String = "",
    var locationName: String = "",
    var coordinates: GeoPoint? = null,
    var maxOccupancy: Long = 1,
    var pricePerNightCents: Long = 0,
    var currency: String = "CAD",
    var tags: List<String> = emptyList(),
    var rules: String = "",
    var cancellationPolicy: String = "",
    var policyVersion: Long = 1,
    var promoImageId: String? = null,
    var ratingSum: Long = 0,
    var ratingCount: Long = 0,
    var status: String = "draft",
    @get:ServerTimestamp
    var createdAt: Timestamp? = null,
    @get:ServerTimestamp
    var updatedAt: Timestamp? = null
)