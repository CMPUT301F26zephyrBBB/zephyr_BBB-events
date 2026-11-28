package com.example.zephyr_bbb.database

import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.Rating
import com.example.zephyr_bbb.database.repository.RatingRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class RatingRepositoryTest {
    private lateinit var db: FirebaseFirestore
    private lateinit var repository: RatingRepository
    private lateinit var transaction: Transaction
    private lateinit var campsiteRef: DocumentReference
    private lateinit var ratingRef: DocumentReference

    @Before
    fun setup() {
        mockkObject(FirebaseProvider)

        db = mockk(relaxed = true)
        transaction = mockk(relaxed = true)
        campsiteRef = mockk(relaxed = true)
        ratingRef = mockk(relaxed = true)

        every { FirebaseProvider.db } returns db
        every { FirebaseProvider.requireUid() } returns "test_user"

        every {
            db.collection("campsites").document("test_campsite")
        } returns campsiteRef

        every {
            campsiteRef.collection("ratings").document("test_user")
        } returns ratingRef

        repository = RatingRepository()
    }
    @After
    fun cleanup() {
        unmockkObject(FirebaseProvider)
    }
    @Test
    fun testRatingBelowMinimum() {
        assertInvalidRating(0L)
    }
    @Test
    fun testRatingAboveMaximum() {
        assertInvalidRating(6L)
    }
    private fun assertInvalidRating(score: Long) {
        var success = false
        var error: Exception? = null
        repository.rateCampsite(
            campsiteId = "test_campsite",
            score = score,
            onSuccess = { success = true },
            onFailure = { error = it }
        )
        assertFalse(success)
        assertTrue(error is IllegalArgumentException)
        assertEquals(
            "Choose a rating from 1 to 5.",
            error?.message
        )
        verify(exactly = 0) {
            FirebaseProvider.requireUid()
        }
    }
    private fun mockTransaction(
        campsite: Campsite,
        existingRating: Rating?
    ) {
        every {
            transaction.get(campsiteRef)
                .toObject(Campsite::class.java)
        } returns campsite

        every {
            transaction.get(ratingRef)
                .toObject(Rating::class.java)
        } returns existingRating
        val task = mockk<Task<Boolean>>(relaxed = true)
        every {
            db.runTransaction<Boolean>(any())
        } answers {
            firstArg<Transaction.Function<Boolean>>()
                .apply(transaction)
            task
        }
    }
    @Test
    fun testNewRatingUpdatesTotals() {
        mockTransaction(
            campsite = Campsite(
                ratingSum = 8L,
                ratingCount = 2L
            ),
            existingRating = null
        )
        repository.rateCampsite(
            "test_campsite", 5L, {}, {}
        )
        verify {
            transaction.set(
                ratingRef,
                match<Rating> {
                    it.entrantId == "test_user" &&
                            it.score == 5L
                }
            )
        }
        verify {
            transaction.update(
                campsiteRef,
                match<Map<String, Any>> {
                    it["ratingSum"] == 13L &&
                            it["ratingCount"] == 3L
                }
            )
        }
    }
    @Test
    fun testUpdatingRatingDoesNotIncreaseCount() {
        mockTransaction(
            campsite = Campsite(
                ratingSum = 12L,
                ratingCount = 3L
            ),
            existingRating = Rating(score = 3L)
        )
        repository.rateCampsite(
            "test_campsite", 5L, {}, {}
        )
        verify {
            transaction.update(
                campsiteRef,
                match<Map<String, Any>> {
                    it["ratingSum"] == 14L &&
                            it["ratingCount"] == 3L
                }
            )
        }
    }
    @Test
    fun testUpdatingRatingToLowerScore() {
        mockTransaction(
            campsite = Campsite(
                ratingSum = 10L,
                ratingCount = 2L
            ),
            existingRating = Rating(score = 5L)
        )
        repository.rateCampsite(
            "test_campsite", 1L, {}, {}
        )
        verify {
            transaction.update(
                campsiteRef,
                match<Map<String, Any>> {
                    it["ratingSum"] == 6L &&
                            it["ratingCount"] == 2L
                }
            )
        }
    }
}