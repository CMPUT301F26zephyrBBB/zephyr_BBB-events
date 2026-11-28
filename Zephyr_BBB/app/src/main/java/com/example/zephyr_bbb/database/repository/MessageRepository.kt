package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.OwnerMessage
import com.example.zephyr_bbb.database.model.Reservation
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

/**
 *Stores bidirectional messages between owners and entrants.
 * Messages: messages/{messageId}
 * Call after sign-in.
 * Admin message logs are handled by AdminRepository.
 */
class MessageRepository {

    private val db = FirebaseProvider.db
    private val messagesRef = db.collection(Collections.MESSAGES)

    /**
     * Sends a message between the owner and entrant of a reservation.
     * Only reservation participants can send messages.
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

            val recipientId = when (uid) {
                reservation.ownerId -> reservation.entrantId
                reservation.entrantId -> reservation.ownerId
                else -> throw IllegalStateException(
                    "Only reservation participants can send messages."
                )
            }

            val now = Timestamp.now()

            transaction.set(
                messageRef,
                OwnerMessage(
                    senderId = uid,
                    recipientId = recipientId,
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

    /**
     * Listens in real time for the current user's messages about a reservation:
     * the ones they sent and the ones they received, merged oldest first.
     * Uses two queries filtered by the current user, which the security rules allow
     * for either participant. Waits for both before the first update so the
     * conversation doesn't flicker in half at a time.
     * Call remove() on the listener when leaving the screen.
     */
    fun observeMyMessagesForReservation(
        reservationId: String,
        onSuccess: (List<OwnerMessage>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration {
        val uid = FirebaseProvider.requireUid()
        val forReservation = messagesRef.whereEqualTo("reservationId", reservationId)

        var sent: List<OwnerMessage>? = null
        var received: List<OwnerMessage>? = null

        fun publish() {
            val mine = sent ?: return
            val theirs = received ?: return
            onSuccess(
                (mine + theirs)
                    .distinctBy { it.id }
                    .sortedBy { it.createdAt }
            )
        }

        val sentListener = forReservation
            .whereEqualTo("senderId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    sent = readMessages(snapshot)
                    publish()
                }
            }

        val receivedListener = forReservation
            .whereEqualTo("recipientId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    received = readMessages(snapshot)
                    publish()
                }
            }

        return ListenerRegistration {
            sentListener.remove()
            receivedListener.remove()
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