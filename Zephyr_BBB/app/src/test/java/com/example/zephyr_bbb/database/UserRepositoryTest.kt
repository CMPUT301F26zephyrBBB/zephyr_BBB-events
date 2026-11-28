package com.example.zephyr_bbb.database

import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.android.gms.tasks.Task
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class UserRepositoryTest {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser
    private lateinit var userRef: DocumentReference
    private lateinit var repository: UserRepository

    @Before
    fun setup() {
        mockkObject(FirebaseProvider)

        db = mockk(relaxed = true)
        auth = mockk(relaxed = true)
        user = mockk(relaxed = true)
        userRef = mockk(relaxed = true)

        every { FirebaseProvider.db } returns db
        every { FirebaseProvider.auth } returns auth

        every { auth.currentUser } returns user
        every { user.uid } returns "test_user"

        every {
            db.collection("users").document("test_user")
        } returns userRef

        every {
            userRef.update(any<Map<String, Any?>>())
        } returns mockk<Task<Void>>(relaxed = true)

        repository = UserRepository()
    }

    @After
    fun cleanup() {
        unmockkObject(FirebaseProvider)
    }

    @Test
    fun testUpdateProfileTrimsWhitespace() {
        repository.updateProfile(
            name = "  Test User  ",
            email = "  test@example.com  ",
            phone = "  7805550100  ",
            onSuccess = {},
            onFailure = { throw it }
        )

        verify(exactly = 1) {
            userRef.update(
                match<Map<String, Any?>> {
                    it["name"] == "Test User" &&
                            it["email"] == "test@example.com" &&
                            it["phone"] == "7805550100"
                }
            )
        }
    }

    @Test
    fun testUpdateProfilePreservesRoles() {
        repository.updateProfile(
            "Test User",
            "test@example.com",
            "7805550100",
            {},
            { throw it }
        )

        verify {
            userRef.update(
                match<Map<String, Any?>> {
                    it.keys == setOf(
                        "name",
                        "email",
                        "phone",
                        "updatedAt"
                    )
                }
            )
        }
    }

    @Test
    fun testDisableNotifications() {
        repository.updateNotificationPreference(
            false, {}, { throw it }
        )

        verify {
            userRef.update(
                match<Map<String, Any?>> {
                    it["notificationsEnabled"] == false
                }
            )
        }
    }

    @Test
    fun testEnableNotifications() {
        repository.updateNotificationPreference(
            true, {}, { throw it }
        )

        verify {
            userRef.update(
                match<Map<String, Any?>> {
                    it["notificationsEnabled"] == true
                }
            )
        }
    }

    @Test
    fun testSetProfileImage() {
        repository.updateProfileImage(
            "test_image", {}, { throw it }
        )

        verify {
            userRef.update(
                match<Map<String, Any?>> {
                    it["profileImageId"] == "test_image"
                }
            )
        }
    }

    @Test
    fun testRemoveProfileImage() {
        repository.updateProfileImage(
            null, {}, { throw it }
        )

        verify {
            userRef.update(
                match<Map<String, Any?>> {
                    it.containsKey("profileImageId") &&
                            it["profileImageId"] == null
                }
            )
        }
    }

    @Test
    fun testUnauthenticatedProfileUpdateRejected() {
        every { auth.currentUser } returns null

        var success = false
        var error: Exception? = null

        repository.updateProfile(
            "Test User",
            "test@example.com",
            "7805550100",
            { success = true },
            { error = it }
        )

        assertFalse(success)
        assertTrue(error is IllegalStateException)
        assertEquals("Sign in first.", error?.message)

        verify(exactly = 0) {
            userRef.update(any<Map<String, Any?>>())
        }
    }

    @Test
    fun testUnauthenticatedProfileDeletionRejected() {
        every { auth.currentUser } returns null

        var success = false
        var error: Exception? = null

        repository.deleteCurrentProfile(
            { success = true },
            { error = it }
        )

        assertFalse(success)
        assertTrue(error is IllegalStateException)
        assertEquals("Sign in first.", error?.message)

        verify(exactly = 0) {
            userRef.delete()
        }
    }
}