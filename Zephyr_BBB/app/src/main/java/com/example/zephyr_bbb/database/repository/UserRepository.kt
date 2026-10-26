package com.example.zephyr_bbb.database.repository

import com.example.zephyr_bbb.database.data.Collections
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source

/**
 * Handles anonymous authentication and user profile persistence.
 * Firestore security rules must protect roles and account status.
 */
class UserRepository {

    private val auth = FirebaseProvider.auth
    private val db = FirebaseProvider.db
    private val usersRef = db.collection(Collections.USERS)

    /**
     * Reuses the current account or signs in anonymously.
     * Creates a profile only if one does not already exist.
     * Returns the profile read from the server.
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
                } else {
                    loadOrCreateProfile(
                        uid,
                        onSuccess,
                        onFailure
                    )
                }
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    /**
     * Updates the current user's editable profile fields.
     * Does not modify roles, account status or creation time.
     */
    fun updateProfile(
        name: String,
        email: String,
        phone: String,
        notificationsEnabled: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure(
                IllegalStateException(
                    "Sign in before updating your profile."
                )
            )
            return
        }

        val updates = mapOf(
            "name" to name.trim(),
            "email" to email.trim(),
            "phone" to phone.trim(),
            "notificationsEnabled" to notificationsEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        usersRef.document(uid)
            .update(updates)
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
                val profile = hashMapOf<String, Any?>(
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

                transaction.set(userRef, profile)
            }

            true
        }
            .addOnSuccessListener {
                userRef.get(Source.SERVER)
                    .addOnSuccessListener profileLoaded@{ snapshot ->
                        val user = try {
                            snapshot.toObject(User::class.java)
                        } catch (error: Exception) {
                            onFailure(error)
                            return@profileLoaded
                        }

                        if (user == null) {
                            onFailure(
                                IllegalStateException(
                                    "User profile was not found."
                                )
                            )
                        } else {
                            onSuccess(user)
                        }
                    }
                    .addOnFailureListener { error ->
                        onFailure(error)
                    }
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }
}