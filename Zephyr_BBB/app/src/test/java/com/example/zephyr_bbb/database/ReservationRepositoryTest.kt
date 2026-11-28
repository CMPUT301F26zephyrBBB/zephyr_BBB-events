package com.example.zephyr_bbb.database

import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.*
import com.example.zephyr_bbb.database.repository.ReservationRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ReservationRepositoryTest {

    private lateinit var db: FirebaseFirestore
    private lateinit var repository: ReservationRepository
    private lateinit var transaction: Transaction
    private lateinit var reservationRef: DocumentReference
    private lateinit var lotteryRef: DocumentReference
    private lateinit var entryRef: DocumentReference
    private lateinit var campsiteRef: DocumentReference
    private lateinit var userRef: DocumentReference
    private lateinit var notificationRef: DocumentReference
    private lateinit var firstDayRef: DocumentReference
    private lateinit var secondDayRef: DocumentReference

    private lateinit var lottery: Lottery
    private lateinit var campsite: Campsite

    @Before
    fun setup() {
        mockkObject(FirebaseProvider)

        db = mockk(relaxed = true)
        transaction = mockk(relaxed = true)
        reservationRef = mockk(relaxed = true)
        lotteryRef = mockk(relaxed = true)
        entryRef = mockk(relaxed = true)
        campsiteRef = mockk(relaxed = true)
        userRef = mockk(relaxed = true)
        notificationRef = mockk(relaxed = true)
        firstDayRef = mockk(relaxed = true)
        secondDayRef = mockk(relaxed = true)

        every { FirebaseProvider.db } returns db
        every { FirebaseProvider.requireUid() } returns "test_user"

        every {
            db.collection("reservations").document("test_lottery_test_user")
        } returns reservationRef

        every { reservationRef.id } returns "test_lottery_test_user"

        every {
            db.collection("lotteries").document("test_lottery")
        } returns lotteryRef

        every {
            lotteryRef.collection("entries").document("test_user")
        } returns entryRef

        every {
            db.collection("campsites").document("test_campsite")
        } returns campsiteRef

        every {
            db.collection("users").document("test_user")
        } returns userRef

        every {
            db.collection("notifications").document()
        } returns notificationRef

        every {
            campsiteRef.collection("availability").document("2027-07-01")
        } returns firstDayRef

        every {
            campsiteRef.collection("availability").document("2027-07-02")
        } returns secondDayRef

        val now = Timestamp.now().seconds

        lottery = Lottery(
            id = "test_lottery",
            status = "drawn",
            campsiteIds = listOf("test_campsite"),
            bookingOpensAt = Timestamp(now - 3600, 0),
            bookingClosesAt = Timestamp(now + 3600, 0),
            stayStartDate = "2027-07-01",
            stayEndDate = "2027-07-05"
        )

        campsite = Campsite(
            id = "test_campsite",
            ownerId = "test_owner",
            name = "Jasper Campsite",
            maxOccupancy = 4,
            pricePerNightCents = 6500,
            currency = "CAD",
            policyVersion = 2
        )

        repository = ReservationRepository()
    }

    @After
    fun cleanup() {
        unmockkObject(FirebaseProvider)
    }

    private fun mockBooking(
        entryStatus: String = "selected",
        firstNightStatus: String = "open",
        existing: Reservation? = null
    ) {
        every {
            transaction.get(lotteryRef).toObject(Lottery::class.java)
        } returns lottery

        every {
            transaction.get(entryRef).toObject(LotteryEntry::class.java)
        } returns LotteryEntry(
            entrantId = "test_user",
            lotteryId = "test_lottery",
            status = entryStatus
        )

        every {
            transaction.get(campsiteRef).toObject(Campsite::class.java)
        } returns campsite

        every {
            transaction.get(reservationRef).toObject(Reservation::class.java)
        } returns existing

        every {
            transaction.get(userRef).getBoolean("notificationsEnabled")
        } returns true

        every { transaction.get(firstDayRef).exists() } returns true
        every { transaction.get(firstDayRef).getString("status") } returns firstNightStatus
        every { transaction.get(secondDayRef).exists() } returns false

        every { db.runTransaction<String>(any()) } answers {
            firstArg<Transaction.Function<String>>().apply(transaction)
            mockk<Task<String>>(relaxed = true)
        }
    }

    private fun book(
        guests: Long = 2,
        accepted: Boolean = true,
        checkIn: String = "2027-07-01",
        checkOut: String = "2027-07-03",
        onFailure: (Exception) -> Unit = { throw it }
    ) {
        repository.createReservation(
            lotteryId = "test_lottery",
            campsiteId = "test_campsite",
            checkInDate = checkIn,
            checkOutDate = checkOut,
            guestCount = guests,
            rulesAccepted = accepted,
            onSuccess = {},
            onFailure = onFailure
        )
    }

    private fun verifyNoReservationCreated() {
        verify(exactly = 0) {
            transaction.set(reservationRef, any<Reservation>())
        }
    }

    // Entrants must accept campsite policies
    @Test
    fun testPoliciesMustBeAccepted() {
        var error: Exception? = null
        book(accepted = false, onFailure = { error = it })

        assertEquals(
            "Read and accept the campsite rules and policies.",
            error?.message
        )

        verify(exactly = 0) { db.runTransaction<String>(any()) }
    }

    // Checkout must follow check-in
    @Test
    fun testInvalidBookingDatesRejected() {
        var error: Exception? = null
        book("".let { 2L },
            checkIn = "2027-07-03",
            checkOut = "2027-07-01",
            onFailure = { error = it })

        assertEquals("Checkout must be after check-in.", error?.message)
        verify(exactly = 0) { db.runTransaction<String>(any()) }
    }

    // Only selected lottery entrants can book
    @Test
    fun testOnlyLotteryWinnerCanBook() {
        mockBooking(entryStatus = "waiting")
        val error = assertThrows(IllegalStateException::class.java) { book() }

        assertEquals("Only a lottery winner can reserve.", error.message)
        verifyNoReservationCreated()
    }

    // Guest count cannot exceed campsite capacity
    @Test
    fun testGuestCountCannotExceedCapacity() {
        mockBooking()
        val error = assertThrows(IllegalStateException::class.java) {
            book(guests = 5)
        }

        assertEquals("Guest count exceeds the campsite's capacity.", error.message)
        verifyNoReservationCreated()
    }

    // Already reserved nights cannot be booked again
    @Test
    fun testReservedNightPreventsBooking() {
        mockBooking(firstNightStatus = "reserved")
        val error = assertThrows(IllegalStateException::class.java) { book() }

        assertEquals("One or more nights are no longer available.", error.message)
        verifyNoReservationCreated()
    }

    // Successful booking creates the reservation and updates related records
    @Test
    fun testSuccessfulReservation() {
        mockBooking()
        book()

        verify(exactly = 1) {
            transaction.set(reservationRef, match<Reservation> {
                it.entrantId == "test_user" &&
                        it.ownerId == "test_owner" &&
                        it.campsiteId == "test_campsite" &&
                        it.lotteryId == "test_lottery" &&
                        it.guestCount == 2L &&
                        it.totalPriceCents == 13000L &&
                        it.currency == "CAD" &&
                        it.status == "confirmed" &&
                        it.acceptedPolicyVersion == 2L &&
                        it.policiesAcceptedAt != null
            })
        }

        for (dayRef in listOf(firstDayRef, secondDayRef)) {
            verify(exactly = 1) {
                transaction.set(dayRef, match<Map<String, Any?>> {
                    it["status"] == "reserved" &&
                            it["reservationId"] == "test_lottery_test_user"
                }, any<SetOptions>())
            }
        }

        verify(exactly = 1) {
            transaction.update(entryRef, match<Map<String, Any?>> {
                it["status"] == "booked"
            })
        }

        verify(exactly = 1) {
            transaction.set(notificationRef, match<AppNotification> {
                it.senderId == "test_owner" &&
                        it.recipientId == "test_user" &&
                        it.type == "reservation_confirmed" &&
                        it.reservationId == "test_lottery_test_user"
            })
        }
    }

    // A lottery opportunity cannot be used twice
    @Test
    fun testDuplicateReservationRejected() {
        mockBooking(existing = Reservation(
            campsiteId = "test_campsite",
            checkInDate = "2027-07-01",
            checkOutDate = "2027-07-03",
            guestCount = 3,
            status = "confirmed"
        ))

        val error = assertThrows(IllegalStateException::class.java) { book() }
        assertEquals("This lottery opportunity has already been used.", error.message)
        verifyNoReservationCreated()
    }

    private fun mockCancellation(entrantId: String = "test_user") {
        every {
            transaction.get(reservationRef).toObject(Reservation::class.java)
        } returns Reservation(
            entrantId = entrantId,
            ownerId = "test_owner",
            campsiteId = "test_campsite",
            lotteryId = "test_lottery",
            checkInDate = "2027-07-01",
            checkOutDate = "2027-07-03",
            status = "confirmed"
        )

        every {
            transaction.get(firstDayRef).getString("reservationId")
        } returns "test_lottery_test_user"

        every {
            transaction.get(secondDayRef).getString("reservationId")
        } returns "test_lottery_test_user"

        every {
            transaction.get(userRef).getBoolean("notificationsEnabled")
        } returns true

        every { db.runTransaction<Boolean>(any()) } answers {
            firstArg<Transaction.Function<Boolean>>().apply(transaction)
            mockk<Task<Boolean>>(relaxed = true)
        }
    }

    // Cancellation releases the reserved nights
    @Test
    fun testEntrantCanCancelReservation() {
        mockCancellation()
        repository.cancelReservation("test_lottery_test_user", {}, { throw it })

        verify {
            transaction.update(reservationRef, match<Map<String, Any?>> {
                it["status"] == "cancelled"
            })
        }

        for (dayRef in listOf(firstDayRef, secondDayRef)) {
            verify {
                transaction.update(dayRef, match<Map<String, Any?>> {
                    it["status"] == "open" && it["reservationId"] == null
                })
            }
        }
    }

    // Entrants cannot cancel another user's reservation
    @Test
    fun testOtherUserCannotCancelReservation() {
        mockCancellation(entrantId = "different_user")

        val error = assertThrows(IllegalStateException::class.java) {
            repository.cancelReservation("test_lottery_test_user", {}, {})
        }

        assertEquals("This reservation does not belong to you.", error.message)

        verify(exactly = 0) {
            transaction.update(reservationRef, any<Map<String, Any?>>())
        }
    }

    // Owners can reject reservations and notify entrants
    @Test
    fun testOwnerCanRejectReservation() {
        mockCancellation()
        every { FirebaseProvider.requireUid() } returns "test_owner"

        repository.rejectReservation(
            "test_lottery_test_user", "Campsite maintenance", {}, { throw it }
        )

        verify {
            transaction.update(reservationRef, match<Map<String, Any?>> {
                it["status"] == "rejected" &&
                        it["cancellationReason"] == "Campsite maintenance"
            })
        }

        verify {
            transaction.set(notificationRef, match<AppNotification> {
                it.type == "reservation_rejected" &&
                        it.recipientId == "test_user" &&
                        it.senderId == "test_owner"
            })
        }
    }
}