package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.AppNotification
import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.database.model.LotteryEntry
import com.example.zephyr_bbb.database.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query

/**
 * Handles lottery creation, editing, waitlists, results and declines.
 *
 * Lotteries: lotteries/{lotteryId}
 * Entries: lotteries/{lotteryId}/entries/{entrantId}
 *
 * Call after sign-in. Dates supplied by screens use YYYY-MM-DD.
 * Automatic scheduling is handled separately.
 * Draw transactions are intended for small classroom waitlists.
 */
class LotteryRepository {

    private val db = FirebaseProvider.db
    private val lotteriesRef = db.collection(Collections.LOTTERIES)

    /**
     * Creates a lottery for the owner's selected campsites.
     * A null waitlistCapacity means no application-defined limit.
     * Returns the generated lottery ID.
     */
    fun createLottery(
        lottery: Lottery,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val error = validateLottery(lottery)

        if (error != null) {
            onFailure(IllegalArgumentException(error))
            return
        }

        val now = Timestamp.now()

        val newLottery = lottery.copy(
            id = "",
            ownerId = FirebaseProvider.requireUid(),
            entryCount = 0,
            status = "open",
            drawnAt = null,
            createdAt = now,
            updatedAt = now
        )

        lotteriesRef.add(newLottery)
            .addOnSuccessListener { onSuccess(it.id) }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Updates an owner's lottery before it has been drawn.
     * Preserves existing entries and creation time.
     */
    fun updateLottery(
        lottery: Lottery,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val error = validateLottery(lottery)

        if (error != null) {
            onFailure(IllegalArgumentException(error))
            return
        }

        val uid = FirebaseProvider.requireUid()
        val lotteryRef = lotteriesRef.document(lottery.id)

        db.runTransaction { transaction ->
            val existing = transaction.get(lotteryRef)
                .toObject(Lottery::class.java)!!

            check(existing.ownerId == uid) {
                "Only the owner can edit this lottery."
            }

            check(existing.status == "open") {
                "The lottery has already been drawn."
            }

            val capacity = lottery.waitlistCapacity

            check(
                capacity == null ||
                        capacity >= existing.entryCount
            ) {
                "Capacity cannot be below the current waitlist size."
            }

            transaction.update(
                lotteryRef,
                mapOf(
                    "title" to lottery.title,
                    "campsiteIds" to lottery.campsiteIds,
                    "registrationOpensAt" to lottery.registrationOpensAt,
                    "registrationClosesAt" to lottery.registrationClosesAt,
                    "bookingOpensAt" to lottery.bookingOpensAt,
                    "bookingClosesAt" to lottery.bookingClosesAt,
                    "stayStartDate" to lottery.stayStartDate,
                    "stayEndDate" to lottery.stayEndDate,
                    "winnerCount" to lottery.winnerCount,
                    "waitlistCapacity" to lottery.waitlistCapacity,
                    "updatedAt" to Timestamp.now()
                )
            )

            true
        }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Reads a lottery by ID.
     * Returns null if it does not exist.
     */
    fun getLottery(
        lotteryId: String,
        onSuccess: (Lottery?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        lotteriesRef.document(lotteryId)
            .get()
            .addOnSuccessListener {
                onSuccess(it.toObject(Lottery::class.java))
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Returns the current owner's lotteries.
     */
    fun getMyLotteries(
        onSuccess: (List<Lottery>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        readLotteries(
            query = lotteriesRef.whereEqualTo(
                "ownerId",
                FirebaseProvider.requireUid()
            ),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    /**
     * Returns lotteries accepting registrations for a campsite.
     */
    fun getActiveLotteriesForCampsite(
        campsiteId: String,
        onSuccess: (List<Lottery>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        readLotteries(
            query = lotteriesRef.whereArrayContains(
                "campsiteIds",
                campsiteId
            ),
            onSuccess = { lotteries ->
                val now = Timestamp.now()

                onSuccess(
                    lotteries.filter {
                        it.status == "open" &&
                                now >= it.registrationOpensAt!! &&
                                now < it.registrationClosesAt!!
                    }
                )
            },
            onFailure = onFailure
        )
    }

    /**
     * Returns a lottery's entries, ordered by join time.
     * Use entrantId with UserRepository to display entrant profiles.
     */
    fun getWaitlist(
        lotteryId: String,
        onSuccess: (List<LotteryEntry>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        lotteriesRef.document(lotteryId)
            .collection(Collections.ENTRIES)
            .get()
            .addOnSuccessListener {
                onSuccess(
                    it.toObjects(LotteryEntry::class.java)
                        .sortedBy { entry -> entry.joinedAt }
                )
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Reads the current user's entry and result.
     * Returns null if the user has not joined.
     */
    fun getMyEntry(
        lotteryId: String,
        onSuccess: (LotteryEntry?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        lotteriesRef.document(lotteryId)
            .collection(Collections.ENTRIES)
            .document(FirebaseProvider.requireUid())
            .get()
            .addOnSuccessListener {
                onSuccess(it.toObject(LotteryEntry::class.java))
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Joins during registration if waitlist capacity allows.
     * Using the user's ID prevents duplicate entries.
     */
    fun joinWaitlist(
        lotteryId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = FirebaseProvider.requireUid()
        val lotteryRef = lotteriesRef.document(lotteryId)

        val entryRef = lotteryRef
            .collection(Collections.ENTRIES)
            .document(uid)

        db.runTransaction { transaction ->
            val lottery = transaction.get(lotteryRef)
                .toObject(Lottery::class.java)!!

            val existingEntry = transaction.get(entryRef)
            val now = Timestamp.now()

            check(
                lottery.status == "open" &&
                        now >= lottery.registrationOpensAt!! &&
                        now < lottery.registrationClosesAt!!
            ) {
                "Registration is not open."
            }

            if (!existingEntry.exists()) {
                val capacity = lottery.waitlistCapacity

                check(
                    capacity == null ||
                            lottery.entryCount < capacity
                ) {
                    "The waitlist is full."
                }

                transaction.set(
                    entryRef,
                    LotteryEntry(
                        entrantId = uid,
                        lotteryId = lotteryId,
                        status = "waiting",
                        joinedAt = now,
                        createdAt = now,
                        updatedAt = now
                    )
                )

                transaction.update(
                    lotteryRef,
                    mapOf(
                        "entryCount" to lottery.entryCount + 1,
                        "updatedAt" to now
                    )
                )
            }

            true
        }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Removes the current user from an undrawn waitlist.
     */
    fun leaveWaitlist(
        lotteryId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val lotteryRef = lotteriesRef.document(lotteryId)

        val entryRef = lotteryRef
            .collection(Collections.ENTRIES)
            .document(FirebaseProvider.requireUid())

        db.runTransaction { transaction ->
            val lottery = transaction.get(lotteryRef)
                .toObject(Lottery::class.java)!!

            val entry = transaction.get(entryRef)

            check(lottery.status == "open") {
                "The lottery has already been drawn."
            }

            if (entry.exists()) {
                transaction.delete(entryRef)

                transaction.update(
                    lotteryRef,
                    mapOf(
                        "entryCount" to lottery.entryCount - 1,
                        "updatedAt" to Timestamp.now()
                    )
                )
            }

            true
        }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Draws winners after registration closes.
     *
     * Saves entry results and win/loss notifications together.
     * Respects notification preferences and excludes removed profiles.
     * Repeated calls return the existing winners.
     */
    fun drawWinners(
        lotteryId: String,
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val lotteryRef = lotteriesRef.document(lotteryId)

        lotteryRef.collection(Collections.ENTRIES)
            .get()
            .addOnSuccessListener { snapshot ->
                val entryRefs = snapshot.documents
                    .map { it.reference }
                    .shuffled()

                db.runTransaction { transaction ->
                    val lottery = transaction.get(lotteryRef)
                        .toObject(Lottery::class.java)!!

                    val entryResults = entryRefs.map {
                        transaction.get(it)
                            .toObject(LotteryEntry::class.java)
                    }

                    check(
                        entryResults.none { it == null } &&
                                entryResults.size.toLong() == lottery.entryCount
                    ) {
                        "The waitlist changed. Try the draw again."
                    }

                    val entries = entryResults.filterNotNull()

                    if (lottery.status == "drawn") {
                        entries.filter {
                            it.status in listOf(
                                "selected",
                                "booked",
                                "declined"
                            )
                        }.map { it.entrantId }
                    } else {
                        val now = Timestamp.now()

                        check(
                            lottery.status == "open" &&
                                    now >= lottery.registrationClosesAt!!
                        ) {
                            "Registration has not ended."
                        }

                        // Read profiles before writing any results.
                        val users = entries.associate { entry ->
                            val userRef = db.collection(Collections.USERS)
                                .document(entry.entrantId)

                            entry.entrantId to transaction.get(userRef)
                                .toObject(User::class.java)
                        }

                        val eligibleEntries = entries.filter {
                            users[it.entrantId]?.status == "active"
                        }

                        val winnerCount = minOf(
                            lottery.winnerCount,
                            eligibleEntries.size.toLong()
                        ).toInt()

                        val winners = eligibleEntries
                            .take(winnerCount)
                            .map { it.entrantId }
                            .toSet()

                        entries.forEach { entry ->
                            val won = entry.entrantId in winners
                            val user = users[entry.entrantId]

                            val entryRef = lotteryRef
                                .collection(Collections.ENTRIES)
                                .document(entry.entrantId)

                            transaction.update(
                                entryRef,
                                mapOf(
                                    "status" to if (won) {
                                        "selected"
                                    } else {
                                        "not_selected"
                                    },
                                    "selectedAt" to if (won) now else null,
                                    "updatedAt" to now
                                )
                            )

                            if (
                                user != null &&
                                user.status == "active" &&
                                user.notificationsEnabled
                            ) {
                                val notificationRef = db
                                    .collection(Collections.NOTIFICATIONS)
                                    .document()

                                transaction.set(
                                    notificationRef,
                                    AppNotification(
                                        senderId = lottery.ownerId,
                                        recipientId = entry.entrantId,
                                        type = if (won) {
                                            "lottery_won"
                                        } else {
                                            "lottery_lost"
                                        },
                                        title = if (won) {
                                            "You won the lottery!"
                                        } else {
                                            "Lottery result"
                                        },
                                        body = if (won) {
                                            "You can reserve a stay from ${lottery.title}."
                                        } else {
                                            "You were not selected for ${lottery.title}."
                                        },
                                        lotteryId = lotteryId,
                                        deliveryStatus = "in_app",
                                        createdAt = now,
                                        updatedAt = now
                                    )
                                )
                            }
                        }

                        transaction.update(
                            lotteryRef,
                            mapOf(
                                "status" to "drawn",
                                "drawnAt" to now,
                                "updatedAt" to now
                            )
                        )

                        winners.toList()
                    }
                }
                    .addOnSuccessListener { onSuccess(it) }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Declines an unused winning reservation opportunity.
     */
    fun decline(
        lotteryId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val entryRef = lotteriesRef.document(lotteryId)
            .collection(Collections.ENTRIES)
            .document(FirebaseProvider.requireUid())

        db.runTransaction { transaction ->
            val entry = transaction.get(entryRef)
                .toObject(LotteryEntry::class.java)!!

            check(entry.status == "selected") {
                "Only an unused winning opportunity can be declined."
            }

            val now = Timestamp.now()

            transaction.update(
                entryRef,
                mapOf(
                    "status" to "declined",
                    "respondedAt" to now,
                    "updatedAt" to now
                )
            )

            true
        }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    private fun readLotteries(
        query: Query,
        onSuccess: (List<Lottery>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        query.get()
            .addOnSuccessListener {
                onSuccess(it.toObjects(Lottery::class.java))
            }
            .addOnFailureListener { onFailure(it) }
    }

    private fun validateLottery(lottery: Lottery): String? {
        if (lottery.campsiteIds.isEmpty()) {
            return "Choose at least one campsite."
        }

        if (lottery.winnerCount < 1) {
            return "Choose at least one winner."
        }

        val capacity = lottery.waitlistCapacity

        if (capacity != null && capacity < 1) {
            return "Capacity must be positive or left unset."
        }

        val registrationStart = lottery.registrationOpensAt
            ?: return "Set the registration start time."

        val registrationEnd = lottery.registrationClosesAt
            ?: return "Set the registration end time."

        val bookingStart = lottery.bookingOpensAt
            ?: return "Set the booking start time."

        val bookingEnd = lottery.bookingClosesAt
            ?: return "Set the booking end time."

        if (registrationStart >= registrationEnd) {
            return "Registration must end after it starts."
        }

        if (bookingStart >= bookingEnd) {
            return "Booking must end after it starts."
        }

        if (bookingEnd <= registrationEnd) {
            return "Booking must remain open after registration ends."
        }

        if (
            lottery.stayStartDate.isBlank() ||
            lottery.stayEndDate.isBlank() ||
            lottery.stayStartDate >= lottery.stayEndDate
        ) {
            return "Set the stay dates using YYYY-MM-DD, with the end after the start."
        }

        return null
    }
}