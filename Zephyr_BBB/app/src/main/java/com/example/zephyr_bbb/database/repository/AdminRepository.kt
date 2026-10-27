package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.AppNotification
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.ImageRecord
import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.database.model.OwnerMessage
import com.example.zephyr_bbb.database.model.User
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.QuerySnapshot

/**
 * Handles admin browsing, moderation and communication logs.
 * Removed profiles are marked "removed".
 * Reservation and communication history is preserved.
 * Image removal only clears Firestore metadata/references.
 * Actual image-file removal remains deferred with Storage setup.
 */
class AdminRepository {

    private val db = FirebaseProvider.db

    /**
     * Returns registered campsites.
     */
    fun getCampsites(
        onSuccess: (List<Campsite>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        asAdmin(onFailure) {
            db.collection(Collections.CAMPSITES)
                .get()
                .addOnSuccessListener {
                    onSuccess(it.toObjects(Campsite::class.java))
                }
                .addOnFailureListener { onFailure(it) }
        }
    }

    /**
     * Returns active user profiles.
     * Profiles may contain entrant, owner and admin roles together.
     */
    fun getUsers(
        onSuccess: (List<User>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        asAdmin(onFailure) {
            db.collection(Collections.USERS)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener {
                    onSuccess(it.toObjects(User::class.java))
                }
                .addOnFailureListener { onFailure(it) }
        }
    }

    /**
     * Returns image metadata for the admin image browser.
     */
    fun getImages(
        onSuccess: (List<ImageRecord>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        asAdmin(onFailure) {
            db.collection(Collections.IMAGES)
                .get()
                .addOnSuccessListener {
                    onSuccess(it.toObjects(ImageRecord::class.java))
                }
                .addOnFailureListener { onFailure(it) }
        }
    }

    /**
     * Returns all saved notification records, newest first.
     */
    fun getNotificationLogs(
        onSuccess: (List<AppNotification>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        asAdmin(onFailure) {
            db.collection(Collections.NOTIFICATIONS)
                .get()
                .addOnSuccessListener {
                    onSuccess(
                        it.toObjects(AppNotification::class.java)
                            .sortedByDescending { notice ->
                                notice.createdAt
                            }
                    )
                }
                .addOnFailureListener { onFailure(it) }
        }
    }

    /**
     * Returns all saved owner messages, newest first.
     */
    fun getMessageLogs(
        onSuccess: (List<OwnerMessage>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        asAdmin(onFailure) {
            db.collection(Collections.MESSAGES)
                .get()
                .addOnSuccessListener {
                    onSuccess(
                        it.toObjects(OwnerMessage::class.java)
                            .sortedByDescending { message ->
                                message.createdAt
                            }
                    )
                }
                .addOnFailureListener { onFailure(it) }
        }
    }

    /**
     * Removes a campsite listing and removes it from lotteries.
     * A lottery with no remaining campsites is cancelled.
     * Existing reservations and historical subcollections remain.
     */
    fun removeCampsite(
        campsiteId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        asAdmin(onFailure) {
            db.collection(Collections.LOTTERIES)
                .whereArrayContains("campsiteIds", campsiteId)
                .get()
                .addOnSuccessListener { lotteries ->
                    val batch = db.batch()

                    batch.delete(
                        db.collection(Collections.CAMPSITES)
                            .document(campsiteId)
                    )

                    lotteries.documents.forEach { document ->
                        val lottery =
                            document.toObject(Lottery::class.java)!!

                        val remaining = lottery.campsiteIds.filter {
                            it != campsiteId
                        }

                        batch.update(
                            document.reference,
                            mapOf(
                                "campsiteIds" to remaining,
                                "status" to if (remaining.isEmpty()) {
                                    "cancelled"
                                } else {
                                    lottery.status
                                },
                                "updatedAt" to Timestamp.now()
                            )
                        )
                    }

                    batch.commit()
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it) }
                }
                .addOnFailureListener { onFailure(it) }
        }
    }

    /**
     * Removes personal information and marks a profile removed.
     * Also removes owned campsite listings and cancels owned lotteries.
     *
     * Reservations, lottery entries and logs remain as history.
     */
    fun removeProfile(
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        asAdmin(onFailure) {
            val campsiteTask = db.collection(Collections.CAMPSITES)
                .whereEqualTo("ownerId", userId)
                .get()

            val lotteryTask = db.collection(Collections.LOTTERIES)
                .whereEqualTo("ownerId", userId)
                .get()

            Tasks.whenAllSuccess<QuerySnapshot>(
                listOf(campsiteTask, lotteryTask)
            )
                .addOnSuccessListener { results ->
                    val batch = db.batch()
                    val now = Timestamp.now()

                    batch.update(
                        db.collection(Collections.USERS)
                            .document(userId),
                        mapOf(
                            "name" to "",
                            "email" to "",
                            "phone" to "",
                            "profileImageId" to null,
                            "notificationsEnabled" to false,
                            "status" to "removed",
                            "updatedAt" to now
                        )
                    )

                    results[0].documents.forEach {
                        batch.delete(it.reference)
                    }

                    results[1].documents.forEach {
                        batch.update(
                            it.reference,
                            mapOf(
                                "status" to "cancelled",
                                "updatedAt" to now
                            )
                        )
                    }

                    batch.commit()
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it) }
                }
                .addOnFailureListener { onFailure(it) }
        }
    }

    /**
     * Removes an owner using the same profile-removal operation.
     */
    fun removeOwner(
        ownerId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        removeProfile(ownerId, onSuccess, onFailure)
    }

    /**
     * Removes image metadata and its profile/campsite reference.
     * Does not delete a Storage file while image uploads are deferred.
     */
    fun removeImage(
        imageId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        asAdmin(onFailure) {
            val imageRef = db.collection(Collections.IMAGES)
                .document(imageId)

            db.runTransaction { transaction ->
                val image = transaction.get(imageRef)
                    .toObject(ImageRecord::class.java)!!

                val collection = if (image.targetType == "profile") {
                    Collections.USERS
                } else {
                    Collections.CAMPSITES
                }

                val field = if (image.targetType == "profile") {
                    "profileImageId"
                } else {
                    "promoImageId"
                }

                val targetRef = db.collection(collection)
                    .document(image.targetId)

                val target = transaction.get(targetRef)

                transaction.delete(imageRef)

                if (target.getString(field) == imageId) {
                    transaction.update(
                        targetRef,
                        mapOf(
                            field to null,
                            "updatedAt" to Timestamp.now()
                        )
                    )
                }

                true
            }
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }
        }
    }

    private fun asAdmin(
        onFailure: (Exception) -> Unit,
        action: () -> Unit
    ) {
        db.collection(Collections.USERS)
            .document(FirebaseProvider.requireUid())
            .get()
            .addOnSuccessListener {
                val user = it.toObject(User::class.java)

                if (
                    user != null &&
                    user.status == "active" &&
                    "admin" in user.roles
                ) {
                    action()
                } else {
                    onFailure(
                        IllegalStateException("Admin role required.")
                    )
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
}