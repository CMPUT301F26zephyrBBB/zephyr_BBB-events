package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.AppNotification
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.database.model.LotteryEntry
import com.google.firebase.Timestamp

/**
 * Creates single-campsite lotteries and manages waitlists,
 * draw results and declined winning opportunities.
 * Lotteries: lotteries/{lotteryId}
 * Entries: lotteries/{lotteryId}/entries/{entrantId}
 * Automatic scheduling is handled outside this repository.
 * drawWinners must be called after registration closes.
 */
class LotteryRepository {
    private val auth = FirebaseProvider.auth
    private val db = FirebaseProvider.db
    private val lotteriesRef = db.collection(Collections.LOTTERIES)
    /**
     * Creates a lottery for one campsite owned by the current user.
     * Returns the generated lottery ID.
     */
    fun createLottery(
        lottery: Lottery,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure(
                IllegalStateException("Sign in first.")
            )
            return
        }
        val campsiteId = lottery.campsiteIds.singleOrNull()
        if (campsiteId.isNullOrBlank()) {
            onFailure(
                IllegalArgumentException("Choose one campsite.")
            )
            return
        }

        try {
            require(lottery.winnerCount > 0) {
                "Choose at least one winner."
            }

            val registrationStart =
                requireNotNull(lottery.registrationOpensAt) {
                    "Set the registration start time."
                }

            val registrationEnd =
                requireNotNull(lottery.registrationClosesAt) {
                    "Set the registration end time."
                }

            val bookingStart =
                requireNotNull(lottery.bookingOpensAt) {
                    "Set the booking start time."
                }

            val bookingEnd =
                requireNotNull(lottery.bookingClosesAt) {
                    "Set the booking end time."
                }

            require(registrationStart < registrationEnd) {
                "Registration must end after it starts."
            }

            require(bookingStart < bookingEnd) {
                "Booking must end after it starts."
            }

            require(
                lottery.stayStartDate.isNotBlank() &&
                        lottery.stayEndDate.isNotBlank() &&
                        lottery.stayStartDate < lottery.stayEndDate
            ) {
                "Set a valid stay period using YYYY-MM-DD."
            }
        } catch (error: Exception) {
            onFailure(error)
            return
        }

        val lotteryRef = lotteriesRef.document()

        val campsiteRef = db.collection(Collections.CAMPSITES)
            .document(campsiteId)

        db.runTransaction { transaction ->
            val campsite = transaction.get(campsiteRef)
                .toObject(Campsite::class.java)
                ?: throw IllegalStateException(
                    "Campsite was not found."
                )

            if (campsite.ownerId != uid) {
                throw IllegalStateException(
                    "You can only create lotteries for your own campsite."
                )
            }

            val now = Timestamp.now()

            transaction.set(
                lotteryRef,
                lottery.copy(
                    id = "",
                    ownerId = uid,
                    entryCount = 0,
                    status = "open",
                    drawnAt = null,
                    createdAt = now,
                    updatedAt = now
                )
            )

            lotteryRef.id
        }
            .addOnSuccessListener { id ->
                onSuccess(id)
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
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
            .addOnSuccessListener { snapshot ->
                val lottery = try {
                    snapshot.toObject(Lottery::class.java)
                } catch (error: Exception) {
                    onFailure(error)
                    return@addOnSuccessListener
                }

                onSuccess(lottery)
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    /**
     * Returns lotteries currently accepting registrations
     * for the specified campsite.
     */
    fun getActiveLotteriesForCampsite(
        campsiteId: String,
        onSuccess: (List<Lottery>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        lotteriesRef
            .whereArrayContains("campsiteIds", campsiteId)
            .get()
            .addOnSuccessListener { snapshot ->
                val lotteries = try {
                    snapshot.toObjects(Lottery::class.java)
                } catch (error: Exception) {
                    onFailure(error)
                    return@addOnSuccessListener
                }

                val now = Timestamp.now()

                val active = lotteries.filter { lottery ->
                    val opens = lottery.registrationOpensAt
                    val closes = lottery.registrationClosesAt

                    lottery.status == "open" &&
                            opens != null &&
                            closes != null &&
                            opens <= now &&
                            now < closes
                }

                onSuccess(active)
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    /**
     * Reads the current user's entry in a lottery.
     * Returns null if the user has not joined.
     */
    fun getMyEntry(
        lotteryId: String,
        onSuccess: (LotteryEntry?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure(
                IllegalStateException("Sign in first.")
            )
            return
        }

        lotteriesRef.document(lotteryId)
            .collection(Collections.ENTRIES)
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val entry = try {
                    snapshot.toObject(LotteryEntry::class.java)
                } catch (error: Exception) {
                    onFailure(error)
                    return@addOnSuccessListener
                }

                onSuccess(entry)
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    /**
     * Joins a lottery during its registration period.
     * Repeated calls do not create duplicate entries.
     */
    fun joinWaitlist(
        lotteryId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure(
                IllegalStateException("Sign in first.")
            )
            return
        }

        val lotteryRef = lotteriesRef.document(lotteryId)

        val entryRef = lotteryRef
            .collection(Collections.ENTRIES)
            .document(uid)

        db.runTransaction { transaction ->
            val lottery = transaction.get(lotteryRef)
                .toObject(Lottery::class.java)
                ?: throw IllegalStateException(
                    "Lottery was not found."
                )

            val existingEntry = transaction.get(entryRef)
            val now = Timestamp.now()
            val opens = lottery.registrationOpensAt
            val closes = lottery.registrationClosesAt

            if (
                lottery.status != "open" ||
                opens == null ||
                closes == null ||
                now < opens ||
                now >= closes
            ) {
                throw IllegalStateException(
                    "Registration is not open."
                )
            }

            if (!existingEntry.exists()) {
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
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    /**
     * Removes the current user from an undrawn waitlist.
     * Selected entrants should use decline instead.
     */
    fun leaveWaitlist(
        lotteryId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure(
                IllegalStateException("Sign in first.")
            )
            return
        }

        val lotteryRef = lotteriesRef.document(lotteryId)

        val entryRef = lotteryRef
            .collection(Collections.ENTRIES)
            .document(uid)

        db.runTransaction { transaction ->
            val lottery = transaction.get(lotteryRef)
                .toObject(Lottery::class.java)
                ?: throw IllegalStateException(
                    "Lottery was not found."
                )

            val entry = transaction.get(entryRef)

            if (lottery.status != "open") {
                throw IllegalStateException(
                    "The lottery has already been drawn."
                )
            }

            if (entry.exists()) {
                transaction.delete(entryRef)

                transaction.update(
                    lotteryRef,
                    mapOf(
                        "entryCount" to maxOf(
                            0L,
                            lottery.entryCount - 1
                        ),
                        "updatedAt" to Timestamp.now()
                    )
                )
            }

            true
        }
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    /**
     * Randomly draws winners after registration closes.
     *
     * Saves entry results and winning notifications together.
     * Repeating the call returns the existing winners.
     *
     * This transaction is intended for small class-demo waitlists.
     * Large waitlists would need a server-side draw implementation.
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
                        .toObject(Lottery::class.java)
                        ?: throw IllegalStateException(
                            "Lottery was not found."
                        )

                    val entries = entryRefs.map { ref ->
                        transaction.get(ref)
                            .toObject(LotteryEntry::class.java)
                            ?: throw IllegalStateException(
                                "The waitlist changed. Try again."
                            )
                    }

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
                        val closes = lottery.registrationClosesAt

                        if (
                            lottery.status != "open" ||
                            closes == null ||
                            now < closes
                        ) {
                            throw IllegalStateException(
                                "Registration has not ended."
                            )
                        }

                        if (
                            entries.size.toLong() != lottery.entryCount ||
                            entries.any { it.status != "waiting" }
                        ) {
                            throw IllegalStateException(
                                "The waitlist changed. Try again."
                            )
                        }

                        val numberToSelect = minOf(
                            lottery.winnerCount,
                            entries.size.toLong()
                        ).toInt()

                        val winners = entries
                            .take(numberToSelect)
                            .map { it.entrantId }
                            .toSet()

                        entries.forEach { entry ->
                            val won = entry.entrantId in winners

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

                            if (won) {
                                val notificationRef = db
                                    .collection(Collections.NOTIFICATIONS)
                                    .document()

                                transaction.set(
                                    notificationRef,
                                    AppNotification(
                                        senderId = lottery.ownerId,
                                        recipientId = entry.entrantId,
                                        type = "lottery_won",
                                        title = "You won the lottery!",
                                        body = "You can reserve a stay from ${lottery.title}.",
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
                    .addOnSuccessListener { winners ->
                        onSuccess(winners)
                    }
                    .addOnFailureListener { error ->
                        onFailure(error)
                    }
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    /**
     * Declines an unused winning reservation opportunity.
     */
    fun decline(
        lotteryId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure(
                IllegalStateException("Sign in first.")
            )
            return
        }

        val entryRef = lotteriesRef.document(lotteryId)
            .collection(Collections.ENTRIES)
            .document(uid)

        db.runTransaction { transaction ->
            val entry = transaction.get(entryRef)
                .toObject(LotteryEntry::class.java)
                ?: throw IllegalStateException(
                    "Lottery entry was not found."
                )

            if (
                entry.status != "selected" &&
                entry.status != "declined"
            ) {
                throw IllegalStateException(
                    "Only an unused winning opportunity can be declined."
                )
            }

            if (entry.status == "selected") {
                val now = Timestamp.now()

                transaction.update(
                    entryRef,
                    mapOf(
                        "status" to "declined",
                        "respondedAt" to now,
                        "updatedAt" to now
                    )
                )
            }

            true
        }
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }
}