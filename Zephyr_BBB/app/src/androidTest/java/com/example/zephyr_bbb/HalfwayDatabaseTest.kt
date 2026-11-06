
package com.example.zephyr_bbb

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.repository.CampsiteRepository
import com.example.zephyr_bbb.database.repository.UserRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class HalfwayDatabaseTest {

    @Before
    fun setupFirebase() {

        val auth = FirebaseProvider.auth
        val authLatch = CountDownLatch(1)
        var authError: Exception? = null

        auth.signInAnonymously()
            .addOnSuccessListener {
                authLatch.countDown()
            }
            .addOnFailureListener {
                authError = it
                authLatch.countDown()
            }

        assertTrue(
            "Firebase sign-in timed out",
            authLatch.await(20, TimeUnit.SECONDS)
        )

        if (authError != null) {
            throw AssertionError(
                "Sign-in failed: ${authError?.message}",
                authError
            )
        }

        // create or load the Firestore user profile
        val userRepo = UserRepository()
        val profileLatch = CountDownLatch(1)

        var profileError: Exception? = null
        var userStatus: String? = null

        userRepo.getOrCreateCurrentUser(
            onSuccess = { user ->
                userStatus = user.status
                profileLatch.countDown()
            },
            onFailure = {
                profileError = it
                profileLatch.countDown()
            }
        )

        assertTrue(
            "User profile timed out",
            profileLatch.await(20, TimeUnit.SECONDS)
        )

        if (profileError != null) {
            throw AssertionError(
                "Profile failed: ${profileError?.message}",
                profileError
            )
        }

        assertEquals("active", userStatus)
    }

    @Test
    fun testFirebaseAuthentication() {
        val user = FirebaseProvider.auth.currentUser

        assertNotNull(user)
        println("TEST USER UID: ${user?.uid}")
    }

    @Test
    fun testGetCampsite() {

        val repo = CampsiteRepository()
        val latch = CountDownLatch(1)

        val campId = "TEST301_13a01bf3_camp_1"

        var name: String? = null
        var location: String? = null
        var capacity: Long? = null
        var price: Long? = null
        var error: Exception? = null

        repo.getCampsite(
            campsiteId = campId,
            onSuccess = { camp ->
                name = camp?.name
                location = camp?.locationName
                capacity = camp?.maxOccupancy
                price = camp?.pricePerNightCents
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )

        assertTrue(
            "Firebase timed out",
            latch.await(20, TimeUnit.SECONDS)
        )

        if (error != null) {
            throw AssertionError(
                "Firebase failed: ${error?.message}",
                error
            )
        }

        assertEquals("TEST Jasper Lakeside", name)
        assertEquals("Jasper", location)
        assertEquals(4L, capacity)
        assertEquals(6500L, price)
    }
}
