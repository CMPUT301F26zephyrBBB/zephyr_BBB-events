package com.example.zephyr_bbb.database

import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.OwnerMessage
import com.example.zephyr_bbb.database.model.Reservation
import com.example.zephyr_bbb.database.repository.MessageRepository
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class MessageRepositoryTest {

    private lateinit var db: FirebaseFirestore
    private lateinit var repository: MessageRepository
    private lateinit var transaction: Transaction
    private lateinit var messagesRef: CollectionReference
    private lateinit var messageRef: DocumentReference
    private lateinit var reservationRef: DocumentReference

    @Before
    fun setup() {
        mockkObject(FirebaseProvider)

        db = mockk(relaxed = true)
        transaction = mockk(relaxed = true)
        messagesRef = mockk(relaxed = true)
        messageRef = mockk(relaxed = true)
        reservationRef = mockk(relaxed = true)

        every { FirebaseProvider.db } returns db
        every { FirebaseProvider.requireUid() } returns "test_owner"

        every { db.collection("messages") } returns messagesRef
        every { messagesRef.document() } returns messageRef
        every { messageRef.id } returns "test_message"

        every {
            db.collection("reservations").document("test_reservation")
        } returns reservationRef

        repository = MessageRepository()
    }

    @After
    fun cleanup() {
        unmockkObject(FirebaseProvider)
    }

    private fun mockTransaction() {
        every {
            transaction.get(reservationRef)
                .toObject(Reservation::class.java)
        } returns Reservation(
            id = "test_reservation",
            ownerId = "test_owner",
            entrantId = "test_entrant"
        )

        val task = mockk<Task<String>>(relaxed = true)

        every { db.runTransaction<String>(any()) } answers {
            val result = firstArg<Transaction.Function<String>>()
                .apply(transaction)

            every {
                task.addOnSuccessListener(any())
            } answers {
                firstArg<OnSuccessListener<String>>()
                    .onSuccess(result)
                task
            }

            task
        }
    }

    private fun mockMessageQuery(
        field: String,
        value: String,
        messages: List<OwnerMessage>
    ) {
        val query = mockk<Query>(relaxed = true)
        val snapshot = mockk<QuerySnapshot>()
        val task = mockk<Task<QuerySnapshot>>(relaxed = true)

        every {
            messagesRef.whereEqualTo(field, value)
        } returns query

        every { query.get() } returns task

        every {
            snapshot.toObjects(OwnerMessage::class.java)
        } returns messages

        every {
            task.addOnSuccessListener(any())
        } answers {
            firstArg<OnSuccessListener<QuerySnapshot>>()
                .onSuccess(snapshot)
            task
        }
    }

    // Owner can send a message to the reservation's entrant
    @Test
    fun testOwnerCanSendMessage() {
        mockTransaction()

        var result: String? = null

        repository.sendMessage(
            "test_reservation",
            "  Your campsite is ready.  ",
            { result = it },
            { throw it }
        )

        assertEquals("test_message", result)

        verify(exactly = 1) {
            transaction.set(
                messageRef,
                match<OwnerMessage> {
                    it.senderId == "test_owner" &&
                            it.recipientId == "test_entrant" &&
                            it.reservationId == "test_reservation" &&
                            it.body == "Your campsite is ready."
                }
            )
        }
    }

    // Entrant can reply to the reservation's owner
    @Test
    fun testEntrantCanSendMessage() {
        mockTransaction()

        every {
            FirebaseProvider.requireUid()
        } returns "test_entrant"

        repository.sendMessage(
            "test_reservation",
            "Thank you!",
            {},
            { throw it }
        )

        verify(exactly = 1) {
            transaction.set(
                messageRef,
                match<OwnerMessage> {
                    it.senderId == "test_entrant" &&
                            it.recipientId == "test_owner" &&
                            it.reservationId == "test_reservation" &&
                            it.body == "Thank you!"
                }
            )
        }
    }

    // Only reservation participants can send messages
    @Test
    fun testUnauthorizedUserCannotSendMessage() {
        mockTransaction()

        every {
            FirebaseProvider.requireUid()
        } returns "different_user"

        val error = assertThrows(
            IllegalStateException::class.java
        ) {
            repository.sendMessage(
                "test_reservation",
                "Unauthorized message",
                {},
                {}
            )
        }

        assertEquals(
            "Only reservation participants can send messages.",
            error.message
        )

        verify(exactly = 0) {
            transaction.set(messageRef, any<OwnerMessage>())
        }
    }

    // Received messages are returned newest first
    @Test
    fun testGetMyMessages() {
        val older = OwnerMessage(
            id = "message_1",
            recipientId = "test_owner",
            createdAt = Timestamp(1000L, 0)
        )

        val newer = OwnerMessage(
            id = "message_2",
            recipientId = "test_owner",
            createdAt = Timestamp(2000L, 0)
        )

        mockMessageQuery(
            "recipientId",
            "test_owner",
            listOf(older, newer)
        )

        var result: List<OwnerMessage>? = null

        repository.getMyMessages(
            { result = it },
            { throw it }
        )

        assertEquals(listOf(newer, older), result)

        verify(exactly = 1) {
            messagesRef.whereEqualTo("recipientId", "test_owner")
        }
    }

    // Reservation conversations are returned oldest first
    @Test
    fun testGetMessagesForReservation() {
        val older = OwnerMessage(
            id = "message_1",
            reservationId = "test_reservation",
            createdAt = Timestamp(1000L, 0)
        )

        val newer = OwnerMessage(
            id = "message_2",
            reservationId = "test_reservation",
            createdAt = Timestamp(2000L, 0)
        )

        mockMessageQuery(
            "reservationId",
            "test_reservation",
            listOf(newer, older)
        )

        var result: List<OwnerMessage>? = null

        repository.getMessagesForReservation(
            "test_reservation",
            { result = it },
            { throw it }
        )

        assertEquals(listOf(older, newer), result)

        verify(exactly = 1) {
            messagesRef.whereEqualTo(
                "reservationId",
                "test_reservation"
            )
        }
    }
}
