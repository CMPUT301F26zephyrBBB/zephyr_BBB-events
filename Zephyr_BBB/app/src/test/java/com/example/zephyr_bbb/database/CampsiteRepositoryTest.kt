package com.example.zephyr_bbb.database

import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.repository.CampsiteRepository
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class CampsiteRepositoryTest {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser
    private lateinit var campsitesRef: CollectionReference
    private lateinit var repository: CampsiteRepository

    @Before
    fun setup() {
        mockkObject(FirebaseProvider)

        db = mockk(relaxed = true)
        auth = mockk(relaxed = true)
        user = mockk(relaxed = true)
        campsitesRef = mockk(relaxed = true)

        every { FirebaseProvider.db } returns db
        every { FirebaseProvider.auth } returns auth
        every { auth.currentUser } returns user
        every { user.uid } returns "test_owner"

        every {
            db.collection("campsites")
        } returns campsitesRef

        every {
            campsitesRef.add(any())
        } returns mockk<Task<DocumentReference>>(relaxed = true)

        repository = CampsiteRepository()
    }

    @After
    fun cleanup() {
        unmockkObject(FirebaseProvider)
    }

    private fun validCampsite() = Campsite(
        name = "Jasper Campsite",
        locationName = "Jasper",
        maxOccupancy = 4L,
        pricePerNightCents = 6500L
    )

    private fun assertInvalidCampsite(campsite: Campsite) {
        var success = false
        var error: Exception? = null

        repository.createCampsite(
            campsite,
            { success = true },
            { error = it }
        )

        assertFalse(success)
        assertTrue(error is IllegalArgumentException)

        verify(exactly = 0) {
            campsitesRef.add(any())
        }
    }

    private fun mockSearchResults(campsites: List<Campsite>) {
        val snapshot = mockk<QuerySnapshot>()
        val task = mockk<Task<QuerySnapshot>>(relaxed = true)

        every {
            snapshot.toObjects(Campsite::class.java)
        } returns campsites

        every { campsitesRef.get() } returns task

        every {
            task.addOnSuccessListener(any())
        } answers {
            firstArg<OnSuccessListener<QuerySnapshot>>()
                .onSuccess(snapshot)
            task
        }
    }

    // Campsites must have a valid name
    @Test
    fun testBlankNameRejected() {
        assertInvalidCampsite(
            validCampsite().copy(name = "   ")
        )
    }

    // Campsites must accommodate at least one guest
    @Test
    fun testZeroOccupancyRejected() {
        assertInvalidCampsite(
            validCampsite().copy(maxOccupancy = 0L)
        )
    }

    // Campsite prices cannot be negative
    @Test
    fun testNegativePriceRejected() {
        assertInvalidCampsite(
            validCampsite().copy(pricePerNightCents = -100L)
        )
    }

    // Creation assigns the current owner and initializes ratings
    @Test
    fun testCampsiteCreation() {
        repository.createCampsite(
            validCampsite().copy(
                ownerId = "different_owner",
                ratingSum = 25L,
                ratingCount = 5L
            ),
            {},
            { throw it }
        )

        verify(exactly = 1) {
            campsitesRef.add(
                match<Campsite> {
                    it.ownerId == "test_owner" &&
                            it.name == "Jasper Campsite" &&
                            it.locationName == "Jasper" &&
                            it.maxOccupancy == 4L &&
                            it.pricePerNightCents == 6500L &&
                            it.ratingSum == 0L &&
                            it.ratingCount == 0L
                }
            )
        }
    }

    // Users must be signed in to create campsites
    @Test
    fun testUnauthenticatedCreationRejected() {
        every { auth.currentUser } returns null

        var error: Exception? = null

        repository.createCampsite(
            validCampsite(),
            { fail("Creation should not succeed.") },
            { error = it }
        )

        assertTrue(error is IllegalStateException)

        verify(exactly = 0) {
            campsitesRef.add(any())
        }
    }

    // Checkout must be after check-in
    @Test
    fun testInvalidBookingDatesRejected() {
        var error: Exception? = null

        repository.searchCampsites(
            checkInDate = "2027-05-15",
            checkOutDate = "2027-05-14",
            onSuccess = { fail("Search should fail.") },
            onFailure = { error = it }
        )

        assertTrue(error is IllegalArgumentException)
        assertEquals(
            "Checkout must be after check-in.",
            error?.message
        )
    }

    // Search returns campsites matching the requested location
    @Test
    fun testSearchFiltersByLocation() {
        mockSearchResults(
            listOf(
                Campsite(name = "Camp A", locationName = "Jasper"),
                Campsite(name = "Camp B", locationName = "Banff")
            )
        )

        var results: List<Campsite>? = null

        repository.searchCampsites(
            location = "jasper",
            onSuccess = { results = it },
            onFailure = { throw it }
        )

        assertEquals(
            listOf("Camp A"),
            results?.map { it.name }
        )
    }

    // Search excludes campsites with insufficient capacity
    @Test
    fun testSearchFiltersByOccupancy() {
        mockSearchResults(
            listOf(
                Campsite(name = "Small Camp", maxOccupancy = 2L),
                Campsite(name = "Large Camp", maxOccupancy = 8L)
            )
        )

        var results: List<Campsite>? = null

        repository.searchCampsites(
            guests = 4L,
            onSuccess = { results = it },
            onFailure = { throw it }
        )

        assertEquals(
            listOf("Large Camp"),
            results?.map { it.name }
        )
    }
}