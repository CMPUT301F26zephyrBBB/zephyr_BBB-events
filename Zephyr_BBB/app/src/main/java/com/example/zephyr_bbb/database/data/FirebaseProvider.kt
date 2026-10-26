package com.example.zephyr_bbb.database.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Provides shared access to Firebase Authentication and Firestore.
 */
object FirebaseProvider {
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    /**
     * Returns the signed-in user's ID.
     * Throws if authentication has not completed.
     */
    fun requireUid(): String {
        return checkNotNull(auth.currentUser) {
            "Sign in before accessing the database."
        }.uid
    }
}