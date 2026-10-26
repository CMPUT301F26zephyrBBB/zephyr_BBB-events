package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.OwnerMessage
import com.example.zephyr_bbb.database.model.Reservation
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

/**
 * Stores owner-to-entrant messages about reservations.
 * Messages: messages/{messageId}
 * Call after sign-in.
 * Admin message logs are handled by AdminRepository.
 */
class MessageRepository {

    private val db = FirebaseProvider.db
    private val messagesRef = db.collection(Collections.MESSAGES)

    /**
     * Sends a message from the reservation's owner to its entrant.
     * Returns the generated message ID.
     */
    fun sendMessage(
        reservationId: String,
        body: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (body.isBlank()) {
            onFailure(
                IllegalArgumentException("Enter a message.")
            )
            return
        }

        val uid = FirebaseProvider.requireUid()
        val messageRef = messagesRef.document()

        val reservationRef = db.collection(Collections.RESERVATIONS)
            .document(reservationId)

        db.runTransaction { transaction ->
            val reservation = transaction.get(reservationRef)
                .toObject(Reservation::class.java)!!

            check(reservation.ownerId == uid) {
                "Only the campsite owner can send this message."
            }

            val now = Timestamp.now()

            transaction.set(
                messageRef,
                OwnerMessage(
                    senderId = uid,
                    recipientId = reservation.entrantId,
                    reservationId = reservationId,
                    body = body.trim(),
                    createdAt = now,
                    updatedAt = now
                )
            )

            messageRef.id
        }
            .addOnSuccessListener { onSuccess(it) }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Returns messages received by the current entrant,
     * newest first.
     */
    fun getMyMessages(
        onSuccess: (List<OwnerMessage>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        messagesRef
            .whereEqualTo(
                "recipientId",
                FirebaseProvider.requireUid()
            )
            .get()
            .addOnSuccessListener {
                onSuccess(
                    readMessages(it).sortedByDescending { message ->
                        message.createdAt
                    }
                )
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Returns messages about one reservation, oldest first.
     */
    fun getMessagesForReservation(
        reservationId: String,
        onSuccess: (List<OwnerMessage>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        messagesRef
            .whereEqualTo("reservationId", reservationId)
            .get()
            .addOnSuccessListener {
                onSuccess(readMessages(it))
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Listens for messages about a reservation in real time.
     * Call remove() on the listener when leaving the screen.
     */
    fun observeMessagesForReservation(
        reservationId: String,
        onSuccess: (List<OwnerMessage>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration {
        return messagesRef
            .whereEqualTo("reservationId", reservationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    onSuccess(readMessages(snapshot))
                }
            }
    }

    private fun readMessages(
        snapshot: QuerySnapshot
    ): List<OwnerMessage> {
        return snapshot
            .toObjects(OwnerMessage::class.java)
            .sortedBy { it.createdAt }
    }
}