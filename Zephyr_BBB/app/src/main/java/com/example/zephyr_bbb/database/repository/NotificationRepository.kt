package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.AppNotification
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

/**
 * Reads the current user's in-app notifications.
 *
 * LotteryRepository creates winning notifications.
 * ReservationRepository creates reservation confirmations.
 * These records are displayed inside the app, not sent as phone push alerts.
 */
class NotificationRepository {

    private val auth = FirebaseProvider.auth

    private val notificationsRef = FirebaseProvider.db
        .collection(Collections.NOTIFICATIONS)

    /**
     * Returns the current user's notifications, newest first.
     */
    fun getMyNotifications(
        onSuccess: (List<AppNotification>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure(
                IllegalStateException("Sign in first.")
            )
            return
        }

        notificationsRef
            .whereEqualTo("recipientId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                readNotifications(
                    snapshot,
                    onSuccess,
                    onFailure
                )
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    /**
     * Listens for notifications in real time, newest first.
     *
     * Call remove() on the returned listener when leaving the screen.
     * Returns null if the user is not signed in.
     */
    fun observeMyNotifications(
        onSuccess: (List<AppNotification>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration? {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure(
                IllegalStateException("Sign in first.")
            )
            return null
        }

        return notificationsRef
            .whereEqualTo("recipientId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    readNotifications(
                        snapshot,
                        onSuccess,
                        onFailure
                    )
                }
            }
    }

    private fun readNotifications(
        snapshot: QuerySnapshot,
        onSuccess: (List<AppNotification>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val notifications = try {
            snapshot.toObjects(AppNotification::class.java)
        } catch (error: Exception) {
            onFailure(error)
            return
        }

        onSuccess(
            notifications.sortedByDescending { it.createdAt }
        )
    }
}