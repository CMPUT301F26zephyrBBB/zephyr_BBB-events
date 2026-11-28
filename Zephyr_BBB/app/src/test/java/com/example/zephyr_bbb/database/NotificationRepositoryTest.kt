package com.example.zephyr_bbb.database

import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.AppNotification
import com.example.zephyr_bbb.database.model.User
import com.example.zephyr_bbb.database.repository.NotificationRepository
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class NotificationRepositoryTest {

    private lateinit var db: FirebaseFirestore
    private lateinit var repository: NotificationRepository
    private lateinit var notificationsRef: CollectionReference
    private lateinit var lotteryRef: DocumentReference
    private lateinit var entriesRef: CollectionReference
    private lateinit var usersRef: CollectionReference
    private lateinit var notificationRef: DocumentReference
    private lateinit var transaction: Transaction

    @Before
    fun setup() {
        mockkObject(FirebaseProvider)

        db = mockk(relaxed = true)
        notificationsRef = mockk(relaxed = true)
        lotteryRef = mockk(relaxed = true)
        entriesRef = mockk(relaxed = true)
        usersRef = mockk(relaxed = true)
        notificationRef = mockk(relaxed = true)
        transaction = mockk(relaxed = true)

        every { FirebaseProvider.db } returns db
        every { FirebaseProvider.requireUid() } returns "test_owner"

        every {
            db.collection("notifications")
        } returns notificationsRef

        every {
            db.collection("lotteries").document("test_lottery")
        } returns lotteryRef

        every {
            lotteryRef.collection("entries")
        } returns entriesRef

        every {
            db.collection("users")
        } returns usersRef

        every {
            notificationsRef.document()
        } returns notificationRef

        repository = NotificationRepository()
    }

    @After
    fun cleanup() {
        unmockkObject(FirebaseProvider)
    }

    private fun mockNotificationQuery(
        field: String,
        notifications: List<AppNotification>
    ) {
        val query = mockk<Query>(relaxed = true)
        val snapshot = mockk<QuerySnapshot>(relaxed = true)
        val task = mockk<Task<QuerySnapshot>>(relaxed = true)

        every {
            notificationsRef.whereEqualTo(field, "test_owner")
        } returns query

        every { query.get() } returns task

        every {
            snapshot.toObjects(AppNotification::class.java)
        } returns notifications

        every {
            task.addOnSuccessListener(any())
        } answers {
            firstArg<OnSuccessListener<QuerySnapshot>>()
                .onSuccess(snapshot)
            task
        }
    }

    private fun mockWaitlist(
        users: List<User>,
        ownerId: String = "test_owner"
    ) {
        val snapshot = mockk<QuerySnapshot>(relaxed = true)
        val task = mockk<Task<QuerySnapshot>>(relaxed = true)

        val entries = users.map { user ->
            val entry = mockk<DocumentSnapshot>(relaxed = true)
            val userRef = mockk<DocumentReference>(relaxed = true)
            val userSnapshot = mockk<DocumentSnapshot>(relaxed = true)

            every { entry.id } returns user.id

            every {
                usersRef.document(user.id)
            } returns userRef

            every {
                transaction.get(userRef)
            } returns userSnapshot

            every {
                userSnapshot.toObject(User::class.java)
            } returns user

            entry
        }

        every { snapshot.documents } returns entries
        every { entriesRef.get() } returns task

        every {
            task.addOnSuccessListener(any())
        } answers {
            firstArg<OnSuccessListener<QuerySnapshot>>()
                .onSuccess(snapshot)
            task
        }

        every {
            transaction.get(lotteryRef).getString("ownerId")
        } returns ownerId

        every {
            db.runTransaction<Boolean>(any())
        } answers {
            firstArg<Transaction.Function<Boolean>>()
                .apply(transaction)

            mockk<Task<Boolean>>(relaxed = true)
        }
    }

    // Users can retrieve their received notifications
    @Test
    fun testGetMyNotifications() {
        val notification = AppNotification(
            id = "notification_1",
            recipientId = "test_owner",
            title = "Lottery Update"
        )

        mockNotificationQuery(
            "recipientId",
            listOf(notification)
        )

        var result: List<AppNotification>? = null

        repository.getMyNotifications(
            { result = it },
            { throw it }
        )

        assertEquals(listOf(notification), result)

        verify(exactly = 1) {
            notificationsRef.whereEqualTo(
                "recipientId",
                "test_owner"
            )
        }
    }

    // Owners can retrieve their sent notifications
    @Test
    fun testGetSentNotifications() {
        val notification = AppNotification(
            id = "notification_2",
            senderId = "test_owner",
            title = "Owner Announcement"
        )

        mockNotificationQuery(
            "senderId",
            listOf(notification)
        )

        var result: List<AppNotification>? = null

        repository.getSentNotifications(
            { result = it },
            { throw it }
        )

        assertEquals(listOf(notification), result)

        verify(exactly = 1) {
            notificationsRef.whereEqualTo(
                "senderId",
                "test_owner"
            )
        }
    }

    // Owners can send announcements to waitlist entrants
    @Test
    fun testOwnerAnnouncementContainsCorrectDetails() {
        mockWaitlist(
            listOf(
                User(
                    id = "entrant_1",
                    status = "active",
                    notificationsEnabled = true
                )
            )
        )

        repository.sendWaitlistNotification(
            "test_lottery",
            "Lottery Update",
            "Registration closes soon.",
            {},
            { throw it }
        )

        verify(exactly = 1) {
            transaction.set(
                notificationRef,
                match<AppNotification> {
                    it.senderId == "test_owner" &&
                            it.recipientId == "entrant_1" &&
                            it.lotteryId == "test_lottery" &&
                            it.type == "owner_announcement" &&
                            it.title == "Lottery Update" &&
                            it.body == "Registration closes soon." &&
                            it.deliveryStatus == "in_app"
                }
            )
        }
    }

    // Only active users with notifications enabled receive announcements
    @Test
    fun testOnlyEligibleEntrantsReceiveNotifications() {
        mockWaitlist(
            listOf(
                User(
                    id = "eligible_user",
                    status = "active",
                    notificationsEnabled = true
                ),
                User(
                    id = "disabled_user",
                    status = "active",
                    notificationsEnabled = false
                ),
                User(
                    id = "removed_user",
                    status = "removed",
                    notificationsEnabled = true
                )
            )
        )

        repository.sendWaitlistNotification(
            "test_lottery",
            "Update",
            "Lottery announcement",
            {},
            { throw it }
        )

        verify(exactly = 1) {
            transaction.set(
                any<DocumentReference>(),
                any<AppNotification>()
            )
        }

        verify(exactly = 1) {
            transaction.set(
                notificationRef,
                match<AppNotification> {
                    it.recipientId == "eligible_user"
                }
            )
        }
    }

    // Users cannot notify another owner's waitlist
    @Test
    fun testNonOwnerCannotNotifyWaitlist() {
        mockWaitlist(
            users = listOf(User(id = "entrant_1")),
            ownerId = "different_owner"
        )

        val error = assertThrows(
            IllegalStateException::class.java
        ) {
            repository.sendWaitlistNotification(
                "test_lottery",
                "Update",
                "Unauthorized announcement",
                {},
                {}
            )
        }

        assertEquals(
            "Only the owner can notify this waitlist.",
            error.message
        )

        verify(exactly = 0) {
            transaction.set(
                any<DocumentReference>(),
                any<AppNotification>()
            )
        }
    }
}
