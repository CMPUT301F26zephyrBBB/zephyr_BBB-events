package com.example.zephyr_bbb.database.testing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.*
import com.example.zephyr_bbb.database.repository.UserRepository
import com.google.firebase.Timestamp
import java.util.Date
import java.util.UUID

object HalfwaySeeder {
    fun seed(done: (String) -> Unit) {
        // check if user exists
        UserRepository().getOrCreateCurrentUser(
            onSuccess = { user ->
                val uid = FirebaseProvider.auth.currentUser?.uid
                if (uid == null || user.status != "active") {
                    done("Error: No active user")
                    return@getOrCreateCurrentUser
                }
                val db = FirebaseProvider.db
                val prefix = "TEST301_" + UUID.randomUUID().toString().take(8)
                val now = Timestamp.now()
                fun getTime(days: Int): Timestamp {
                    val ms = System.currentTimeMillis() +days.toLong() * 86400000L
                    return Timestamp(Date(ms)) }
                val campIds = (1..3).map { "${prefix}_camp_$it" }
                val camps = listOf(
                    Campsite(
                        ownerId = uid,
                        name = "TEST Jasper Lakeside",
                        description = "Lake camping near Jasper",
                        locationName = "Jasper",
                        maxOccupancy = 4,
                        pricePerNightCents = 6500,
                        status = "published",
                        tags = listOf("lake", "hiking"),
                        createdAt = now,
                        updatedAt = now
                    ),
                    Campsite(
                        ownerId = uid,
                        name = "TEST Banff Mountain",
                        description = "Mountain camping in Banff",
                        locationName = "Banff",
                        maxOccupancy = 6,
                        pricePerNightCents = 8500,
                        status = "published",
                        tags = listOf("mountain", "hiking"),
                        createdAt = now,
                        updatedAt = now
                    ),
                    Campsite(
                        ownerId = uid,
                        name = "TEST Hinton Forest",
                        description = "Forest camping in Hinton",
                        locationName = "Hinton",
                        maxOccupancy = 8,
                        pricePerNightCents = 4500,
                        status = "published",
                        tags = listOf("forest"),
                        createdAt = now,
                        updatedAt = now
                    )
                )
                // first batch adds campsites
                val batch1 = db.batch()
                for (i in camps.indices) {
                    val ref = db.collection(Collections.CAMPSITES).document(campIds[i])
                    batch1.set(ref, camps[i])
                }
                // save campsites first before other records
                batch1.commit().addOnSuccessListener {
                        val batch2 = db.batch()
                        // add availability for each campsite
                        val dates = listOf(
                            "2027-05-14",
                            "2027-05-15",
                            "2027-05-16"
                        )
                        for (i in campIds.indices) {
                            for (j in dates.indices) {
                                var status = "open"
                                if (i == 2 && j == 1) {
                                    status = "blocked"
                                }
                                val day = AvailabilityDay(
                                    date = dates[j],
                                    status = status,
                                    reservationId = null,
                                    createdAt = now,
                                    updatedAt = now
                                )
                                val ref = db.collection(Collections.CAMPSITES)
                                    .document(campIds[i])
                                    .collection(Collections.AVAILABILITY)
                                    .document(dates[j])
                                batch2.set(ref, day)
                            }
                        }
                        // create two lotteries
                        val lotteries = listOf(
                            Lottery(
                                ownerId = uid,
                                title = "TEST Open Lottery",
                                campsiteIds = listOf(campIds[0]),
                                registrationOpensAt = getTime(-1),
                                registrationClosesAt = getTime(7),
                                bookingOpensAt = getTime(7),
                                bookingClosesAt = getTime(14),
                                stayStartDate = "2027-05-14",
                                stayEndDate = "2027-05-17",
                                winnerCount = 2,
                                waitlistCapacity = 10,
                                entryCount = 0,
                                status = "open",
                                createdAt = now,
                                updatedAt = now
                            ),
                            Lottery(
                                ownerId = uid,
                                title = "TEST Multiple Campsites",
                                campsiteIds = listOf(campIds[1], campIds[2]),
                                registrationOpensAt = getTime(-1),
                                registrationClosesAt = getTime(7),
                                bookingOpensAt = getTime(7),
                                bookingClosesAt = getTime(14),
                                stayStartDate = "2027-05-14",
                                stayEndDate = "2027-05-17",
                                winnerCount = 1,
                                waitlistCapacity = 5,
                                entryCount = 0,
                                status = "open",
                                createdAt = now,
                                updatedAt = now
                            )
                        )
                        for (i in lotteries.indices) {
                            val id = "${prefix}_lottery_${i + 1}"
                            val ref = db.collection(Collections.LOTTERIES).document(id)
                            batch2.set(ref, lotteries[i])
                        }
                        // second batch adds availability and lotteries
                        batch2.commit().addOnSuccessListener { done("SUCCESS: 14 documents added. Prefix: $prefix") }
                            .addOnFailureListener {
                                done("Campsites saved but second batch failed: " + "${it.message}. Prefix: $prefix") } }.addOnFailureListener { done("Failed to add campsites: ${it.message}") } },
            onFailure = {
                done("User setup failed: ${it.message}")
            }
        )
    }
}

@Composable
fun HalfwaySeedPanel() {
    var msg by remember {
        mutableStateOf("Ready to add test data")
    }
    var loading by remember {
        mutableStateOf(false)
    }
    Column(modifier = Modifier.padding(16.dp)) {
        Button(
            enabled = !loading,
            onClick = {
                loading = true
                msg = "Adding data..."
                HalfwaySeeder.seed {
                    msg = it
                    loading = false
                }
            }
        ) {
            Text("Seed Test Data")
        }
        Text(msg)
    }
}
