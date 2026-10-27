package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.AppNotification
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.database.model.LotteryEntry
import com.example.zephyr_bbb.database.model.Reservation
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Handles campsite reservations, cancellations and owner rejections.
 * Booking saves the reservation, reserved nights, lottery response
 * and confirmation notification together.
 * Call after sign-in. Dates use YYYY-MM-DD, excluding checkout.
 * This repository records bookings; it does not process payments.
 */
class ReservationRepository {

    private val db = FirebaseProvider.db

    private val reservationsRef =
        db.collection(Collections.RESERVATIONS)

    /**
     * Creates one reservation from a winning lottery entry.
     *
     * The entrant chooses a campsite and available dates.
     * rulesAccepted must be true after displaying the campsite's
     * rules and cancellation policy.
     *
     * Returns the reservation ID.
     */
    fun createReservation(
        lotteryId: String,
        campsiteId: String,
        checkInDate: String,
        checkOutDate: String,
        guestCount: Long,
        rulesAccepted: Boolean,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (!rulesAccepted) {
            onFailure(
                IllegalArgumentException(
                    "Read and accept the campsite rules and policies."
                )
            )
            return
        }

        val nights = try {
            getNights(checkInDate, checkOutDate)
        } catch (error: Exception) {
            onFailure(error)
            return
        }

        val uid = FirebaseProvider.requireUid()

        val lotteryRef = db.collection(Collections.LOTTERIES)
            .document(lotteryId)

        val entryRef = lotteryRef
            .collection(Collections.ENTRIES)
            .document(uid)

        val campsiteRef = db.collection(Collections.CAMPSITES)
            .document(campsiteId)

        val userRef = db.collection(Collections.USERS)
            .document(uid)

        // Each winning entry provides one reservation opportunity.
        val reservationRef = reservationsRef
            .document("${lotteryId}_$uid")

        db.runTransaction { transaction ->
            val lottery = transaction.get(lotteryRef)
                .toObject(Lottery::class.java)!!

            val entry = transaction.get(entryRef)
                .toObject(LotteryEntry::class.java)

            val campsite = transaction.get(campsiteRef)
                .toObject(Campsite::class.java)!!

            val existing = transaction.get(reservationRef)
                .toObject(Reservation::class.java)

            val user = transaction.get(userRef)

            if (existing != null) {
                check(
                    existing.status == "confirmed" &&
                            existing.campsiteId == campsiteId &&
                            existing.checkInDate == checkInDate &&
                            existing.checkOutDate == checkOutDate &&
                            existing.guestCount == guestCount
                ) {
                    "This lottery opportunity has already been used."
                }

                return@runTransaction reservationRef.id
            }

            val dayRefs = nights.map { date ->
                campsiteRef.collection(Collections.AVAILABILITY)
                    .document(date)
            }

            val days = dayRefs.map {
                transaction.get(it)
            }

            val now = Timestamp.now()

            check(
                lottery.status == "drawn" &&
                        entry?.status == "selected"
            ) {
                "Only a lottery winner can reserve."
            }

            check(campsiteId in lottery.campsiteIds) {
                "Choose a campsite included in this lottery."
            }

            check(
                now >= lottery.bookingOpensAt!! &&
                        now < lottery.bookingClosesAt!!
            ) {
                "The booking period is not open."
            }

            check(
                checkInDate >= lottery.stayStartDate &&
                        checkOutDate <= lottery.stayEndDate
            ) {
                "Choose dates within the lottery's stay period."
            }

            check(guestCount in 1L..campsite.maxOccupancy) {
                "Guest count exceeds the campsite's capacity."
            }

            check(days.all { it.getString("status") == "open" }) {
                "One or more nights are no longer available."
            }

            val reservation = Reservation(
                entrantId = uid,
                ownerId = campsite.ownerId,
                campsiteId = campsiteId,
                lotteryId = lotteryId,
                checkInDate = checkInDate,
                checkOutDate = checkOutDate,
                guestCount = guestCount,
                totalPriceCents =
                    campsite.pricePerNightCents * nights.size.toLong(),
                currency = campsite.currency,
                status = "confirmed",
                acceptedPolicyVersion = campsite.policyVersion,
                policiesAcceptedAt = now,
                createdAt = now,
                updatedAt = now
            )

            transaction.set(reservationRef, reservation)

            dayRefs.forEach { dayRef ->
                transaction.update(
                    dayRef,
                    mapOf(
                        "status" to "reserved",
                        "reservationId" to reservationRef.id,
                        "updatedAt" to now
                    )
                )
            }

            transaction.update(
                entryRef,
                mapOf(
                    "status" to "booked",
                    "respondedAt" to now,
                    "updatedAt" to now
                )
            )

            if (user.getBoolean("notificationsEnabled") == true) {
                val notificationRef = db
                    .collection(Collections.NOTIFICATIONS)
                    .document()

                transaction.set(
                    notificationRef,
                    AppNotification(
                        senderId = campsite.ownerId,
                        recipientId = uid,
                        type = "reservation_confirmed",
                        title = "Reservation confirmed",
                        body = "${campsite.name}: " +
                                "$checkInDate to $checkOutDate",
                        lotteryId = lotteryId,
                        reservationId = reservationRef.id,
                        deliveryStatus = "in_app",
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            reservationRef.id
        }
            .addOnSuccessListener { onSuccess(it) }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Reads a reservation for its details or confirmation screen.
     * Returns null if it does not exist.
     */
    fun getReservation(
        reservationId: String,
        onSuccess: (Reservation?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        reservationsRef.document(reservationId)
            .get()
            .addOnSuccessListener {
                onSuccess(it.toObject(Reservation::class.java))
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Returns the current entrant's reservations.
     * Screens can separate active, past and cancelled reservations.
     */
    fun getMyReservations(
        onSuccess: (List<Reservation>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        readReservations(
            query = reservationsRef.whereEqualTo(
                "entrantId",
                FirebaseProvider.requireUid()
            ),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    /**
     * Returns reservations for the current owner's campsites.
     * Includes confirmed, cancelled and rejected reservations.
     */
    fun getOwnerReservations(
        onSuccess: (List<Reservation>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        readReservations(
            query = reservationsRef.whereEqualTo(
                "ownerId",
                FirebaseProvider.requireUid()
            ),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    /**
     * Lets the entrant cancel their reservation.
     * Releases the nights and saves a cancellation notification.
     */
    fun cancelReservation(
        reservationId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        endReservation(
            reservationId = reservationId,
            reason = "Cancelled by entrant.",
            rejectedByOwner = false,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    /**
     * Lets the owner reject a reservation and provide a reason.
     * Releases the nights and notifies the entrant.
     */
    fun rejectReservation(
        reservationId: String,
        reason: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        endReservation(
            reservationId = reservationId,
            reason = reason,
            rejectedByOwner = true,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    private fun endReservation(
        reservationId: String,
        reason: String,
        rejectedByOwner: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = FirebaseProvider.requireUid()
        val reservationRef = reservationsRef.document(reservationId)

        db.runTransaction { transaction ->
            val reservation = transaction.get(reservationRef)
                .toObject(Reservation::class.java)!!

            val expectedUserId = if (rejectedByOwner) {
                reservation.ownerId
            } else {
                reservation.entrantId
            }

            check(uid == expectedUserId) {
                "This reservation does not belong to you."
            }

            check(reservation.status == "confirmed") {
                "This reservation is no longer confirmed."
            }

            val nights = getNights(
                reservation.checkInDate,
                reservation.checkOutDate
            )

            val campsiteRef = db.collection(Collections.CAMPSITES)
                .document(reservation.campsiteId)

            val dayRefs = nights.map { date ->
                campsiteRef.collection(Collections.AVAILABILITY)
                    .document(date)
            }

            val days = dayRefs.map {
                transaction.get(it)
            }

            val userRef = db.collection(Collections.USERS)
                .document(reservation.entrantId)

            val user = transaction.get(userRef)
            val now = Timestamp.now()

            transaction.update(
                reservationRef,
                mapOf(
                    "status" to if (rejectedByOwner) {
                        "rejected"
                    } else {
                        "cancelled"
                    },
                    "cancellationReason" to reason,
                    "cancelledAt" to now,
                    "updatedAt" to now
                )
            )

            dayRefs.forEachIndexed { index, dayRef ->
                if (
                    days[index].getString("reservationId") ==
                    reservationId
                ) {
                    transaction.update(
                        dayRef,
                        mapOf(
                            "status" to "open",
                            "reservationId" to null,
                            "updatedAt" to now
                        )
                    )
                }
            }

            if (user.getBoolean("notificationsEnabled") == true) {
                val notificationRef = db
                    .collection(Collections.NOTIFICATIONS)
                    .document()

                transaction.set(
                    notificationRef,
                    AppNotification(
                        senderId = uid,
                        recipientId = reservation.entrantId,
                        type = if (rejectedByOwner) {
                            "reservation_rejected"
                        } else {
                            "reservation_cancelled"
                        },
                        title = if (rejectedByOwner) {
                            "Reservation rejected"
                        } else {
                            "Reservation cancelled"
                        },
                        body = reason,
                        lotteryId = reservation.lotteryId,
                        reservationId = reservationId,
                        deliveryStatus = "in_app",
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            true
        }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    private fun readReservations(
        query: Query,
        onSuccess: (List<Reservation>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        query.get()
            .addOnSuccessListener {
                onSuccess(
                    it.toObjects(Reservation::class.java)
                        .sortedBy { reservation ->
                            reservation.checkInDate
                        }
                )
            }
            .addOnFailureListener { onFailure(it) }
    }

    private fun getNights(
        checkInDate: String,
        checkOutDate: String
    ): List<String> {
        val utc = TimeZone.getTimeZone("UTC")

        val format = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.ROOT
        ).apply {
            isLenient = false
            timeZone = utc
        }

        val start = format.parse(checkInDate)!!
        val end = format.parse(checkOutDate)!!

        check(end.after(start)) {
            "Checkout must be after check-in."
        }

        val calendar = Calendar.getInstance(utc).apply {
            time = start
        }

        val nights = mutableListOf<String>()

        while (calendar.time.before(end)) {
            nights.add(format.format(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return nights
    }
}