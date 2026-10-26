package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.User
import com.google.firebase.firestore.FieldValue

/**
 * Handles anonymous device sign-in and user profile persistence.
 * Profiles are stored at users/{Firebase Authentication UID}.
 * Image uploads are handled separately by ImageRepository.
 */
class UserRepository {

    private val auth = FirebaseProvider.auth
    private val db = FirebaseProvider.db
    private val usersRef = db.collection(Collections.USERS)

    /**
     * Reuses the current account or signs in anonymously.
     * Creates a profile only if one does not already exist.
     */
    fun getOrCreateCurrentUser(
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            loadOrCreateProfile(
                currentUser.uid,
                onSuccess,
                onFailure
            )
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid

                if (uid == null) {
                    onFailure(
                        IllegalStateException(
                            "Authentication returned no user."
                        )
                    )
                    return@addOnSuccessListener
                }

                loadOrCreateProfile(
                    uid,
                    onSuccess,
                    onFailure
                )
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    /**
     * Reads a profile by its user ID.
     * Returns null if the profile does not exist.
     */
    fun getUser(
        userId: String,
        onSuccess: (User?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        usersRef.document(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val user = try {
                    snapshot.toObject(User::class.java)
                } catch (error: Exception) {
                    onFailure(error)
                    return@addOnSuccessListener
                }

                onSuccess(user)
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    /**
     * Updates the current user's name, email and phone number.
     * Preserves the user's roles and other profile fields.
     */
    fun updateProfile(
        name: String,
        email: String,
        phone: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        updateCurrentUser(
            updates = mapOf(
                "name" to name.trim(),
                "email" to email.trim(),
                "phone" to phone.trim()
            ),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    /**
     * Enables or disables notifications for the current user
     * Notification creation/delivery code must read this preference
     * when deciding whether to notify the user.
     */
    fun updateNotificationPreference(
        enabled: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        updateCurrentUser(
            updates = mapOf(
                "notificationsEnabled" to enabled
            ),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    /**
     * Sets the current user's profile image reference.
     * Pass null to remove the reference.
     * This does not upload or delete the actual image file.
     */
    fun updateProfileImage(
        imageId: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        updateCurrentUser(
            updates = mapOf(
                "profileImageId" to imageId
            ),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    /**
     * Deletes the current user's profile document and signs out
     * Existing reservations, campsite listings and image records
     * are not deleted by this method.
     */
    fun deleteCurrentProfile(
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure(
                IllegalStateException("Sign in first.")
            )
            return
        }

        usersRef.document(uid)
            .delete()
            .addOnSuccessListener {
                auth.signOut()
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    private fun updateCurrentUser(
        updates: Map<String, Any?>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure(
                IllegalStateException("Sign in first.")
            )
            return
        }

        val fields = updates + (
                "updatedAt" to FieldValue.serverTimestamp()
                )

        usersRef.document(uid)
            .update(fields)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    private fun loadOrCreateProfile(
        uid: String,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userRef = usersRef.document(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)

            if (!snapshot.exists()) {
                transaction.set(
                    userRef,
                    mapOf(
                        "name" to "",
                        "email" to "",
                        "phone" to "",
                        "roles" to listOf("entrant"),
                        "profileImageId" to null,
                        "notificationsEnabled" to true,
                        "status" to "active",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            true
        }
            .addOnSuccessListener {
                getUser(
                    userId = uid,
                    onSuccess = { user ->
                        if (user == null) {
                            onFailure(
                                IllegalStateException(
                                    "User profile was not found."
                                )
                            )
                        } else if (user.status != "active") {
                            onFailure(
                                IllegalStateException(
                                    "This profile has been removed."
                                )
                            )
                        } else {
                            onSuccess(user)
                        }
                    },
                    onFailure = onFailure
                )
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }
}