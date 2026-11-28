package com.example.zephyr_bbb.database

import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.database.model.LotteryEntry
import com.example.zephyr_bbb.database.repository.LotteryRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class LotteryRepositoryTest {

    private lateinit var db: FirebaseFirestore
    private lateinit var repository: LotteryRepository
    private lateinit var transaction: Transaction
    private lateinit var lotteriesRef: CollectionReference
    private lateinit var lotteryRef: DocumentReference
    private lateinit var entryRef: DocumentReference
    private lateinit var validLottery: Lottery

    @Before
    fun setup() {
        mockkObject(FirebaseProvider)

        db = mockk(relaxed = true)
        transaction = mockk(relaxed = true)
        lotteriesRef = mockk(relaxed = true)
        lotteryRef = mockk(relaxed = true)
        entryRef = mockk(relaxed = true)

        every { FirebaseProvider.db } returns db
        every { FirebaseProvider.requireUid() } returns "test_user"

        every {
            db.collection("lotteries")
        } returns lotteriesRef

        every {
            lotteriesRef.document("test_lottery")
        } returns lotteryRef

        every {
            lotteryRef.collection("entries")
                .document("test_user")
        } returns entryRef

        val now = Timestamp.now().seconds

        validLottery = Lottery(
            id = "test_lottery",
            ownerId = "test_user",
            title = "Summer Lottery",
            campsiteIds = listOf("test_campsite"),
            registrationOpensAt = Timestamp(now - 3600, 0),
            registrationClosesAt = Timestamp(now + 3600, 0),
            bookingOpensAt = Timestamp(now + 3600, 0),
            bookingClosesAt = Timestamp(now + 7200, 0),
            stayStartDate = "2027-07-01",
            stayEndDate = "2027-07-05",
            winnerCount = 2,
            waitlistCapacity = 10,
            entryCount = 3,
            status = "open"
        )

        repository = LotteryRepository()
    }

    @After
    fun cleanup() {
        unmockkObject(FirebaseProvider)
    }

    private fun mockTransaction(
        lottery: Lottery = validLottery,
        entryExists: Boolean = false
    ) {
        every {
            transaction.get(lotteryRef)
                .toObject(Lottery::class.java)
        } returns lottery

        every {
            transaction.get(entryRef).exists()
        } returns entryExists

        val task = mockk<Task<Boolean>>(relaxed = true)

        every {
            db.runTransaction<Boolean>(any())
        } answers {
            firstArg<Transaction.Function<Boolean>>()
                .apply(transaction)
            task
        }
    }

    private fun assertInvalidLottery(
        lottery: Lottery,
        expectedMessage: String
    ) {
        var error: Exception? = null

        repository.createLottery(
            lottery,
            { fail("Invalid lottery was accepted") },
            { error = it }
        )

        assertTrue(error is IllegalArgumentException)
        assertEquals(expectedMessage, error?.message)

        verify(exactly = 0) {
            lotteriesRef.add(any())
        }
    }

    // Lottery must include at least one campsite
    @Test
    fun testLotteryRequiresCampsite() {
        assertInvalidLottery(
            validLottery.copy(campsiteIds = emptyList()),
            "Choose at least one campsite."
        )
    }

    // Registration must have a valid start and end time
    @Test
    fun testInvalidRegistrationPeriodRejected() {
        assertInvalidLottery(
            validLottery.copy(
                registrationClosesAt =
                    validLottery.registrationOpensAt
            ),
            "Registration must end after it starts."
        )
    }

    // New lotteries have the correct owner and initial state
    @Test
    fun testCreateLotterySetsCorrectDefaults() {
        every {
            lotteriesRef.add(any())
        } returns mockk<Task<DocumentReference>>(relaxed = true)

        repository.createLottery(
            validLottery.copy(
                ownerId = "different_user",
                status = "drawn",
                entryCount = 20
            ),
            {},
            { throw it }
        )

        verify(exactly = 1) {
            lotteriesRef.add(
                match<Lottery> {
                    it.ownerId == "test_user" &&
                            it.status == "open" &&
                            it.entryCount == 0L &&
                            it.id == "" &&
                            it.drawnAt == null &&
                            it.createdAt != null
                }
            )
        }
    }

    // Owner can edit an open lottery without losing existing data
    @Test
    fun testOwnerCanEditOpenLottery() {
        mockTransaction()

        repository.updateLottery(
            validLottery.copy(
                title = "Updated Lottery",
                winnerCount = 4
            ),
            {},
            { throw it }
        )

        verify(exactly = 1) {
            transaction.update(
                lotteryRef,
                match<Map<String, Any?>> {
                    it["title"] == "Updated Lottery" &&
                            it["winnerCount"] == 4L &&
                            !it.containsKey("entryCount") &&
                            !it.containsKey("createdAt") &&
                            !it.containsKey("ownerId") &&
                            !it.containsKey("status")
                }
            )
        }
    }

    // Users cannot edit another owner's lottery
    @Test
    fun testNonOwnerCannotEditLottery() {
        mockTransaction(
            validLottery.copy(ownerId = "different_owner")
        )

        val error = assertThrows(
            IllegalStateException::class.java
        ) {
            repository.updateLottery(validLottery, {}, {})
        }

        assertEquals(
            "Only the owner can edit this lottery.",
            error.message
        )

        verify(exactly = 0) {
            transaction.update(
                lotteryRef,
                any<Map<String, Any?>>()
            )
        }
    }

    // Completed lotteries cannot be edited
    @Test
    fun testDrawnLotteryCannotBeEdited() {
        mockTransaction(
            validLottery.copy(status = "drawn")
        )

        val error = assertThrows(
            IllegalStateException::class.java
        ) {
            repository.updateLottery(validLottery, {}, {})
        }

        assertEquals(
            "The lottery has already been drawn.",
            error.message
        )
    }

    // Joining creates a waitlist entry and increments the count
    @Test
    fun testJoinWaitlistCreatesEntryAndIncrementsCount() {
        mockTransaction()

        repository.joinWaitlist(
            "test_lottery",
            {},
            { throw it }
        )

        verify(exactly = 1) {
            transaction.set(
                entryRef,
                match<LotteryEntry> {
                    it.entrantId == "test_user" &&
                            it.lotteryId == "test_lottery" &&
                            it.status == "waiting"
                }
            )
        }

        verify(exactly = 1) {
            transaction.update(
                lotteryRef,
                match<Map<String, Any?>> {
                    it["entryCount"] == 4L
                }
            )
        }
    }

    // Users cannot join the same waitlist twice
    @Test
    fun testDuplicateWaitlistEntryIsNotCreated() {
        mockTransaction(entryExists = true)

        repository.joinWaitlist(
            "test_lottery",
            {},
            { throw it }
        )

        verify(exactly = 0) {
            transaction.set(entryRef, any<LotteryEntry>())
        }

        verify(exactly = 0) {
            transaction.update(
                lotteryRef,
                any<Map<String, Any?>>()
            )
        }
    }

    // Full waitlists cannot accept additional entrants
    @Test
    fun testFullWaitlistRejectsNewEntrants() {
        mockTransaction(
            validLottery.copy(
                entryCount = 10,
                waitlistCapacity = 10
            )
        )

        val error = assertThrows(
            IllegalStateException::class.java
        ) {
            repository.joinWaitlist("test_lottery", {}, {})
        }

        assertEquals("The waitlist is full.", error.message)

        verify(exactly = 0) {
            transaction.set(entryRef, any<LotteryEntry>())
        }
    }

    // Leaving removes the waitlist entry and decrements the count
    @Test
    fun testLeaveWaitlistDeletesEntryAndDecrementsCount() {
        mockTransaction(entryExists = true)

        repository.leaveWaitlist(
            "test_lottery",
            {},
            { throw it }
        )

        verify(exactly = 1) {
            transaction.delete(entryRef)
        }

        verify(exactly = 1) {
            transaction.update(
                lotteryRef,
                match<Map<String, Any?>> {
                    it["entryCount"] == 2L
                }
            )
        }
    }
}
