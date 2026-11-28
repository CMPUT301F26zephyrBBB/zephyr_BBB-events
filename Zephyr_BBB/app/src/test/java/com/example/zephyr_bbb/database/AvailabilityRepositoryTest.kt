package com.example.zephyr_bbb.database

import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.AvailabilityDay
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.repository.AvailabilityRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AvailabilityRepositoryTest {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser
    private lateinit var repository: AvailabilityRepository
    private lateinit var transaction: Transaction
    private lateinit var campsiteRef: DocumentReference
    private lateinit var availabilityRef: CollectionReference
    private lateinit var dayRef: DocumentReference
    private lateinit var secondDayRef: DocumentReference

    @Before
    fun setup() {
        mockkObject(FirebaseProvider)

        db = mockk(relaxed = true)
        auth = mockk(relaxed = true)
        user = mockk(relaxed = true)
        transaction = mockk(relaxed = true)
        campsiteRef = mockk(relaxed = true)
        availabilityRef = mockk(relaxed = true)
        dayRef = mockk(relaxed = true)
        secondDayRef = mockk(relaxed = true)

        every { FirebaseProvider.db } returns db
        every { FirebaseProvider.auth } returns auth

        every { auth.currentUser } returns user
        every { user.uid } returns "test_owner"

        every {
            db.collection("campsites").document("test_campsite")
        } returns campsiteRef

        every {
            campsiteRef.collection("availability")
        } returns availabilityRef

        every {
            availabilityRef.document("2027-05-14")
        } returns dayRef

        every {
            availabilityRef.document("2027-05-15")
        } returns secondDayRef

        repository = AvailabilityRepository()
    }

    @After
    fun cleanup() {
        unmockkObject(FirebaseProvider)
    }

    private fun mockTransaction(
        ownerId: String = "test_owner",
        existingDay: AvailabilityDay? = null,
        secondDay: AvailabilityDay? = null
    ) {
        every {
            transaction.get(campsiteRef)
                .toObject(Campsite::class.java)
        } returns Campsite(ownerId = ownerId)

        every {
            transaction.get(dayRef)
                .toObject(AvailabilityDay::class.java)
        } returns existingDay

        every {
            transaction.get(secondDayRef)
                .toObject(AvailabilityDay::class.java)
        } returns secondDay

        val task = mockk<Task<Boolean>>(relaxed = true)

        every {
            db.runTransaction<Boolean>(any())
        } answers {
            firstArg<Transaction.Function<Boolean>>()
                .apply(transaction)
            task
        }
    }

    // Invalid availability status must be rejected
    @Test
    fun testInvalidStatusRejected() {
        var error: Exception? = null

        repository.setAvailabilityForDates(
            campsiteId = "test_campsite",
            dates = listOf("2027-05-14"),
            status = "invalid",
            onSuccess = { fail("Invalid status accepted") },
            onFailure = { error = it }
        )

        assertTrue(error is IllegalArgumentException)
        assertEquals(
            "Status must be open or blocked.",
            error?.message
        )

        verify(exactly = 0) {
            db.runTransaction<Boolean>(any())
        }
    }

    // Owner can open a night for booking
    @Test
    fun testOpenNightCreated() {
        mockTransaction()

        repository.setAvailability(
            "test_campsite",
            "2027-05-14",
            "open",
            {},
            { throw it }
        )

        verify(exactly = 1) {
            transaction.set(
                dayRef,
                match<AvailabilityDay> {
                    it.date == "2027-05-14" &&
                            it.status == "open" &&
                            it.reservationId == null
                }
            )
        }
    }

    // Owner can block a night from booking
    @Test
    fun testBlockedNightCreated() {
        mockTransaction()

        repository.setAvailability(
            "test_campsite",
            "2027-05-14",
            "blocked",
            {},
            { throw it }
        )

        verify(exactly = 1) {
            transaction.set(
                dayRef,
                match<AvailabilityDay> {
                    it.status == "blocked"
                }
            )
        }
    }

    // Owner can update multiple dates at once
    @Test
    fun testMultipleDatesUpdated() {
        mockTransaction()

        repository.setAvailabilityForDates(
            "test_campsite",
            listOf("2027-05-14", "2027-05-15"),
            "blocked",
            {},
            { throw it }
        )

        verify(exactly = 1) {
            transaction.set(
                dayRef,
                match<AvailabilityDay> {
                    it.date == "2027-05-14" &&
                            it.status == "blocked"
                }
            )
        }

        verify(exactly = 1) {
            transaction.set(
                secondDayRef,
                match<AvailabilityDay> {
                    it.date == "2027-05-15" &&
                            it.status == "blocked"
                }
            )
        }
    }

    // Users cannot edit another owner's availability
    @Test
    fun testNonOwnerCannotEditAvailability() {
        mockTransaction(ownerId = "different_owner")

        val error = assertThrows(
            IllegalStateException::class.java
        ) {
            repository.setAvailability(
                "test_campsite",
                "2027-05-14",
                "blocked",
                {},
                {}
            )
        }

        assertEquals(
            "Only the owner can edit availability.",
            error.message
        )

        verify(exactly = 0) {
            transaction.set(any<DocumentReference>(), any())
        }
    }

    // Existing reservations cannot be overwritten
    @Test
    fun testReservedNightPreventsEntireBatchUpdate() {
        mockTransaction(
            existingDay = AvailabilityDay(status = "open"),
            secondDay = AvailabilityDay(status = "reserved")
        )

        assertThrows(IllegalStateException::class.java) {
            repository.setAvailabilityForDates(
                "test_campsite",
                listOf("2027-05-14", "2027-05-15"),
                "blocked",
                {},
                {}
            )
        }

        verify(exactly = 0) {
            transaction.set(any<DocumentReference>(), any())
        }
    }
}