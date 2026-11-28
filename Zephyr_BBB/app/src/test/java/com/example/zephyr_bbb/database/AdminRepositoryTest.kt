package com.example.zephyr_bbb.database

import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.*
import com.example.zephyr_bbb.database.repository.AdminRepository
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AdminRepositoryTest {

    private lateinit var db: FirebaseFirestore
    private lateinit var repository: AdminRepository
    private lateinit var adminRef: DocumentReference
    private lateinit var campsitesRef: CollectionReference
    private lateinit var campsiteRef: DocumentReference
    private lateinit var ratingRef: DocumentReference
    private lateinit var reviewRef: DocumentReference
    private lateinit var transaction: Transaction

    @Before
    fun setup() {
        mockkObject(FirebaseProvider)

        db = mockk(relaxed = true)
        adminRef = mockk(relaxed = true)
        campsitesRef = mockk(relaxed = true)
        campsiteRef = mockk(relaxed = true)
        ratingRef = mockk(relaxed = true)
        reviewRef = mockk(relaxed = true)
        transaction = mockk(relaxed = true)

        every { FirebaseProvider.db } returns db
        every { FirebaseProvider.requireUid() } returns "test_admin"

        every {
            db.collection("users").document("test_admin")
        } returns adminRef

        every {
            db.collection("campsites")
        } returns campsitesRef

        every {
            campsitesRef.document("test_campsite")
        } returns campsiteRef

        every {
            campsiteRef.collection("ratings").document("test_entrant")
        } returns ratingRef

        every {
            campsiteRef.collection("reviews").document("test_entrant")
        } returns reviewRef

        mockAdmin()

        repository = AdminRepository()
    }

    @After
    fun cleanup() {
        unmockkObject(FirebaseProvider)
    }

    private fun <T> successfulTask(result: T): Task<T> {
        val task = mockk<Task<T>>(relaxed = true)

        every { task.addOnSuccessListener(any()) } answers {
            firstArg<OnSuccessListener<T>>().onSuccess(result)
            task
        }

        return task
    }

    private fun mockAdmin(
        roles: List<String> = listOf("entrant", "admin")
    ) {
        val snapshot = mockk<DocumentSnapshot>()

        every {
            snapshot.toObject(User::class.java)
        } returns User(
            id = "test_admin",
            roles = roles,
            status = "active"
        )

        every { adminRef.get() } returns successfulTask(snapshot)
    }

    // Admin can retrieve registered campsites
    @Test
    fun testAdminCanGetCampsites() {
        val campsites = listOf(
            Campsite(id = "test_campsite", name = "Jasper Campsite")
        )

        val snapshot = mockk<QuerySnapshot>()

        every {
            snapshot.toObjects(Campsite::class.java)
        } returns campsites

        every {
            campsitesRef.get()
        } returns successfulTask(snapshot)

        var result: List<Campsite>? = null

        repository.getCampsites(
            { result = it },
            { throw it }
        )

        assertEquals(campsites, result)

        verify(exactly = 1) {
            campsitesRef.get()
        }
    }

    // Users without the admin role cannot access admin functions
    @Test
    fun testNonAdminAccessRejected() {
        mockAdmin(roles = listOf("entrant"))

        var error: Exception? = null

        repository.getCampsites(
            { fail("Non-admin access should be rejected.") },
            { error = it }
        )

        assertTrue(error is IllegalStateException)
        assertEquals("Admin role required.", error?.message)

        verify(exactly = 0) {
            campsitesRef.get()
        }
    }

    // Removing a campsite cancels lotteries with no remaining campsites
    @Test
    fun testRemoveCampsite() {
        val lotteriesRef = mockk<CollectionReference>()
        val lotteryQuery = mockk<Query>()
        val lotteryRef = mockk<DocumentReference>()
        val lotteryDocument = mockk<DocumentSnapshot>()
        val snapshot = mockk<QuerySnapshot>()
        val batch = mockk<WriteBatch>(relaxed = true)

        every {
            db.collection("lotteries")
        } returns lotteriesRef

        every {
            lotteriesRef.whereArrayContains(
                "campsiteIds", "test_campsite"
            )
        } returns lotteryQuery

        every {
            lotteryQuery.get()
        } returns successfulTask(snapshot)

        every {
            snapshot.documents
        } returns listOf(lotteryDocument)

        every {
            lotteryDocument.reference
        } returns lotteryRef

        every {
            lotteryDocument.toObject(Lottery::class.java)
        } returns Lottery(
            campsiteIds = listOf("test_campsite"),
            status = "open"
        )

        every { db.batch() } returns batch

        every {
            batch.commit()
        } returns successfulTask<Void?>(null)

        repository.removeCampsite(
            "test_campsite",
            {},
            { throw it }
        )

        verify(exactly = 1) {
            batch.delete(campsiteRef)
        }

        verify(exactly = 1) {
            batch.update(
                lotteryRef,
                match<Map<String, Any?>> {
                    it["campsiteIds"] == emptyList<String>() &&
                            it["status"] == "cancelled"
                }
            )
        }

        verify(exactly = 1) {
            batch.commit()
        }
    }

    // Removing a rating also updates the campsite's rating totals
    @Test
    fun testRemoveRatingUpdatesTotals() {
        every {
            transaction.get(campsiteRef)
                .toObject(Campsite::class.java)
        } returns Campsite(
            ratingSum = 12L,
            ratingCount = 3L
        )

        every {
            transaction.get(ratingRef)
                .toObject(Rating::class.java)
        } returns Rating(score = 4L)

        every { db.runTransaction<Boolean>(any()) } answers {
            val result = firstArg<Transaction.Function<Boolean>>()
                .apply(transaction)

            successfulTask(result)
        }

        repository.removeRating(
            "test_campsite",
            "test_entrant",
            {},
            { throw it }
        )

        verify(exactly = 1) {
            transaction.delete(ratingRef)
        }

        verify(exactly = 1) {
            transaction.update(
                campsiteRef,
                match<Map<String, Any?>> {
                    it["ratingSum"] == 8L &&
                            it["ratingCount"] == 2L
                }
            )
        }
    }

    // Removing a written review does not change numerical ratings
    @Test
    fun testRemoveReview() {
        every {
            reviewRef.delete()
        } returns successfulTask<Void?>(null)

        repository.removeReview(
            "test_campsite",
            "test_entrant",
            {},
            { throw it }
        )

        verify(exactly = 1) {
            reviewRef.delete()
        }

        verify(exactly = 0) {
            ratingRef.delete()
        }

        verify(exactly = 0) {
            db.runTransaction<Any>(any())
        }
    }
}
