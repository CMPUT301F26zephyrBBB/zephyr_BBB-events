package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.AppNotification
import com.example.zephyr_bbb.database.model.AvailabilityDay
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.database.model.LotteryEntry
import com.example.zephyr_bbb.database.model.Reservation
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Creates reservations for lottery winners and reads confirmations.
 * Reservations, reserved nights, entry status and confirmation
 * notifications are saved together to prevent double bookings.
 */
class ReservationRepository {

    private val auth = FirebaseProvider.auth
    private val db = FirebaseProvider.db

    private val reservationsRef =
        db.collection(Collections.RESERVATIONS)

    /**
     * Books the lottery's campsite for the selected nights.
     * The current user must have won the lottery.
     * Dates use YYYY-MM-DD, with checkout excluded.
     * Returns the reservation ID.
     */
    fun createReservation(
        lotteryId: String,
        checkInDate: String,
        checkOutDate: String,
        guestCount: Long,
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

        val nights = try {
            getNights(checkInDate, checkOutDate)
        } catch (error: Exception) {
            onFailure(error)
            return
        }

        val lotteryRef = db.collection(Collections.LOTTERIES)
            .document(lotteryId)

        val entryRef = lotteryRef
            .collection(Collections.ENTRIES)
            .document(uid)

        // One reservation per entrant per lottery.
        val reservationRef = reservationsRef
            .document("${lotteryId}_$uid")

        db.runTransaction { transaction ->
            val lottery = transaction.get(lotteryRef)
                .toObject(Lottery::class.java)
                ?: throw IllegalStateException(
                    "Lottery was not found."
                )

            val entry = transaction.get(entryRef)
                .toObject(LotteryEntry::class.java)
                ?: throw IllegalStateException(
                    "Lottery entry was not found."
                )

            val existing = transaction.get(reservationRef)
                .toObject(Reservation::class.java)

            // A repeated tap returns the existing reservation.
            if (existing != null) {
                require(
                    existing.checkInDate == checkInDate &&
                            existing.checkOutDate == checkOutDate &&
                            existing.guestCount == guestCount
                ) {
                    "You already made a reservation for this lottery."
                }

                return@runTransaction reservationRef.id
            }

            val campsiteId = lottery.campsiteIds.singleOrNull()
                ?: throw IllegalStateException(
                    "The lottery must contain one campsite."
                )

            val campsiteRef = db.collection(Collections.CAMPSITES)
                .document(campsiteId)

            val campsite = transaction.get(campsiteRef)
                .toObject(Campsite::class.java)
                ?: throw IllegalStateException(
                    "Campsite was not found."
                )

            val dayRefs = nights.map { date ->
                campsiteRef.collection(Collections.AVAILABILITY)
                    .document(date)
            }

            val days = dayRefs.map { ref ->
                transaction.get(ref)
                    .toObject(AvailabilityDay::class.java)
            }

            val now = Timestamp.now()
            val bookingOpens = lottery.bookingOpensAt
            val bookingCloses = lottery.bookingClosesAt

            require(
                lottery.status == "drawn" &&
                        entry.status == "selected"
            ) {
                "Only a lottery winner can reserve."
            }

            require(
                bookingOpens != null &&
                        bookingCloses != null &&
                        bookingOpens <= now &&
                        now < bookingCloses
            ) {
                "The booking period is not open."
            }

            require(
                checkInDate >= lottery.stayStartDate &&
                        checkOutDate <= lottery.stayEndDate
            ) {
                "Choose dates within the lottery's stay period."
            }

            require(guestCount in 1L..campsite.maxOccupancy) {
                "Guest count must be within the campsite's capacity."
            }

            require(days.all { it?.status == "open" }) {
                "One or more nights are no longer available."
            }

            val totalPrice = Math.multiplyExact(
                campsite.pricePerNightCents,
                nights.size.toLong()
            )

            val reservation = Reservation(
                entrantId = uid,
                ownerId = campsite.ownerId,
                campsiteId = campsiteId,
                lotteryId = lotteryId,
                checkInDate = checkInDate,
                checkOutDate = checkOutDate,
                guestCount = guestCount,
                totalPriceCents = totalPrice,
                currency = campsite.currency,
                status = "confirmed",
                createdAt = now,
                updatedAt = now
            )

            transaction.set(reservationRef, reservation)

            dayRefs.forEach { ref ->
                transaction.update(
                    ref,
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

            reservationRef.id
        }
            .addOnSuccessListener { id ->
                onSuccess(id)
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    /**
     * Reads a saved reservation for the confirmation screen.
     * Returns null if it does not exist.
     */
    fun getReservation(
        reservationId: String,
        onSuccess: (Reservation?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        reservationsRef.document(reservationId)
            .get()
            .addOnSuccessListener { snapshot ->
                val reservation = try {
                    snapshot.toObject(Reservation::class.java)
                } catch (error: Exception) {
                    onFailure(error)
                    return@addOnSuccessListener
                }

                onSuccess(reservation)
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    private fun getNights(
        checkInDate: String,
        checkOutDate: String
    ): List<String> {
        val timeZone = TimeZone.getTimeZone("UTC")

        val format = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.ROOT
        ).apply {
            isLenient = false
            this.timeZone = timeZone
        }

        val start = format.parse(checkInDate)
            ?: throw IllegalArgumentException(
                "Invalid check-in date."
            )

        val end = format.parse(checkOutDate)
            ?: throw IllegalArgumentException(
                "Invalid checkout date."
            )

        require(
            format.format(start) == checkInDate &&
                    format.format(end) == checkOutDate
        ) {
            "Dates must use YYYY-MM-DD."
        }

        require(end.after(start)) {
            "Checkout must be after check-in."
        }

        val calendar = Calendar.getInstance(timeZone).apply {
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