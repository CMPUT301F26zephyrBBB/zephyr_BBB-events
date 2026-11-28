package com.example.zephyr_bbb.database

import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.Review
import com.example.zephyr_bbb.database.repository.ReviewRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ReviewRepositoryTest {

    private lateinit var db: FirebaseFirestore
    private lateinit var repository: ReviewRepository
    private lateinit var transaction: Transaction
    private lateinit var campsiteRef: DocumentReference
    private lateinit var reviewRef: DocumentReference

    @Before
    fun setup() {
        mockkObject(FirebaseProvider)

        db = mockk(relaxed = true)
        transaction = mockk(relaxed = true)
        campsiteRef = mockk(relaxed = true)
        reviewRef = mockk(relaxed = true)

        every { FirebaseProvider.db } returns db
        every { FirebaseProvider.requireUid() } returns "test_user"

        every {
            db.collection("campsites").document("test_campsite")
        } returns campsiteRef

        every {
            campsiteRef.collection("reviews").document("test_user")
        } returns reviewRef

        repository = ReviewRepository()
    }

    @After
    fun cleanup() {
        unmockkObject(FirebaseProvider)
    }

    private fun assertInvalidReview(text: String) {
        var success = false
        var error: Exception? = null

        repository.postReview(
            campsiteId = "test_campsite",
            text = text,
            onSuccess = { success = true },
            onFailure = { error = it }
        )

        assertFalse(success)
        assertTrue(error is IllegalArgumentException)

        assertEquals(
            "Review must contain 1 to 2000 characters.",
            error?.message
        )

        verify(exactly = 0) {
            FirebaseProvider.requireUid()
        }
    }

    private fun mockTransaction(existingReview: Review?) {

        every {
            transaction.get(campsiteRef).exists()
        } returns true

        every {
            transaction.get(reviewRef)
                .toObject(Review::class.java)
        } returns existingReview

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
    fun testEmptyReviewRejected() {
        assertInvalidReview("")
    }

    @Test
    fun testWhitespaceReviewRejected() {
        assertInvalidReview("     ")
    }

    @Test
    fun testOversizedReviewRejected() {
        assertInvalidReview("a".repeat(2001))
    }

    @Test
    fun testNewReviewCreated() {
        mockTransaction(null)

        repository.postReview(
            "test_campsite",
            "Great campsite!",
            {},
            {}
        )

        verify(exactly = 1) {
            transaction.set(
                reviewRef,
                match<Review> {
                    it.entrantId == "test_user" &&
                            it.text == "Great campsite!"
                }
            )
        }
    }

    @Test
    fun testReviewWhitespaceTrimmed() {
        mockTransaction(null)

        repository.postReview(
            "test_campsite",
            "   Great campsite!   ",
            {},
            {}
        )

        verify {
            transaction.set(
                reviewRef,
                match<Review> {
                    it.text == "Great campsite!"
                }
            )
        }
    }

    @Test
    fun testMaximumLengthReviewAccepted() {
        mockTransaction(null)

        val text = "a".repeat(2000)

        repository.postReview(
            "test_campsite", text, {}, {}
        )

        verify {
            transaction.set(
                reviewRef,
                match<Review> {
                    it.text == text
                }
            )
        }
    }

    @Test
    fun testUpdatingReviewPreservesCreationTime() {

        val originalTime = Timestamp(1000L, 0)

        mockTransaction(
            Review(
                entrantId = "test_user",
                text = "Old review",
                createdAt = originalTime
            )
        )

        repository.postReview(
            "test_campsite",
            "Updated review",
            {},
            {}
        )

        verify {
            transaction.set(
                reviewRef,
                match<Review> {
                    it.entrantId == "test_user" &&
                            it.text == "Updated review" &&
                            it.createdAt == originalTime
                }
            )
        }
    }
}