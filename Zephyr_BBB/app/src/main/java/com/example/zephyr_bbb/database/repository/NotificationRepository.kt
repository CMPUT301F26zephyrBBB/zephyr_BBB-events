package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.AppNotification
import com.example.zephyr_bbb.database.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

/**
 * Reads in-app notifications and sends owner announcements
 * to entrants who joined a lottery.
 *
 * LotteryRepository and ReservationRepository create their
 * own result and confirmation notifications.
 *
 * Call after sign-in. These records are displayed inside the app;
 * they do not send phone push alerts.
 */
class NotificationRepository {

    private val db = FirebaseProvider.db

    private val notificationsRef =
        db.collection(Collections.NOTIFICATIONS)

    /**
     * Returns the current user's notifications, newest first.
     */
    fun getMyNotifications(
        onSuccess: (List<AppNotification>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        notificationsRef
            .whereEqualTo(
                "recipientId",
                FirebaseProvider.requireUid()
            )
            .get()
            .addOnSuccessListener {
                onSuccess(readNotifications(it))
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Listens for incoming notifications in real time.
     * Call remove() on the listener when leaving the screen.
     */
    fun observeMyNotifications(
        onSuccess: (List<AppNotification>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration {
        return notificationsRef
            .whereEqualTo(
                "recipientId",
                FirebaseProvider.requireUid()
            )
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    onSuccess(readNotifications(snapshot))
                }
            }
    }

    /**
     * Returns notifications sent by the current user.
     * Includes owner announcements and automatic notices
     * stored with that owner's sender ID.
     */
    fun getSentNotifications(
        onSuccess: (List<AppNotification>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        notificationsRef
            .whereEqualTo(
                "senderId",
                FirebaseProvider.requireUid()
            )
            .get()
            .addOnSuccessListener {
                onSuccess(readNotifications(it))
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Sends an owner's announcement to the lottery's entrants.
     *
     * Skips removed profiles and users who opted out.
     * Intended for small classroom waitlists.
     */
    fun sendWaitlistNotification(
        lotteryId: String,
        title: String,
        body: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = FirebaseProvider.requireUid()

        val lotteryRef = db.collection(Collections.LOTTERIES)
            .document(lotteryId)

        lotteryRef.collection(Collections.ENTRIES)
            .get()
            .addOnSuccessListener { entries ->
                val userRefs = entries.documents.map { entry ->
                    db.collection(Collections.USERS)
                        .document(entry.id)
                }

                db.runTransaction { transaction ->
                    val lottery = transaction.get(lotteryRef)

                    check(lottery.getString("ownerId") == uid) {
                        "Only the owner can notify this waitlist."
                    }

                    // Finish reading profiles before writing notices.
                    val users = userRefs.mapNotNull { userRef ->
                        transaction.get(userRef)
                            .toObject(User::class.java)
                    }

                    val recipients = users.filter {
                        it.status == "active" &&
                                it.notificationsEnabled
                    }

                    val now = Timestamp.now()

                    recipients.forEach { user ->
                        val notificationRef =
                            notificationsRef.document()

                        transaction.set(
                            notificationRef,
                            AppNotification(
                                senderId = uid,
                                recipientId = user.id,
                                type = "owner_announcement",
                                title = title,
                                body = body,
                                lotteryId = lotteryId,
                                deliveryStatus = "in_app",
                                createdAt = now,
                                updatedAt = now
                            )
                        )
                    }

                    true
                }
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    private fun readNotifications(
        snapshot: QuerySnapshot
    ): List<AppNotification> {
        return snapshot
            .toObjects(AppNotification::class.java)
            .sortedByDescending { it.createdAt }
    }
}