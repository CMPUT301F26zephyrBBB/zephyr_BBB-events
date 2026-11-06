
package com.example.zephyr_bbb

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.database.data.FirebaseProvider
import com.example.zephyr_bbb.database.repository.CampsiteRepository
import com.example.zephyr_bbb.database.repository.UserRepository
import com.example.zephyr_bbb.database.repository.LotteryRepository
import com.example.zephyr_bbb.database.repository.AvailabilityRepository
import com.example.zephyr_bbb.database.repository.ReservationRepository
import com.example.zephyr_bbb.database.repository.NotificationRepository
import com.example.zephyr_bbb.database.repository.AdminRepository
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


    @Test
    fun testSearchCampsitesByLocation() {
        val repo = CampsiteRepository()
        val latch = CountDownLatch(1)
        var foundJasper = false
        var allMatch = false
        var error: Exception? = null
        repo.searchCampsites(
            location = "Jasper",
            onSuccess = { camps -> foundJasper = camps.any {it.id == "TEST301_13a01bf3_camp_1" }
                allMatch = camps.all { it.locationName.contains("Jasper", true) }
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )
        assertTrue("Search timed out", latch.await(20, TimeUnit.SECONDS))
        if (error != null) {
            throw AssertionError("Search failed: ${error?.message}", error)
        }
        assertTrue("Jasper campsite not found", foundJasper)
        assertTrue("Non-Jasper campsite returned", allMatch)
    }

    @Test
    fun testSearchCampsitesByGuests() {
        val repo = CampsiteRepository()
        val latch = CountDownLatch(1)
        var foundHinton = false
        var correctCapacity = false
        var error: Exception? = null
        repo.searchCampsites(
            searchText = "TEST",
            guests = 7,
            onSuccess = { camps ->
                foundHinton = camps.any {
                    it.id == "TEST301_13a01bf3_camp_3"
                }
                correctCapacity = camps.all {
                    it.maxOccupancy >= 7
                }
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )
        assertTrue("Search timed out", latch.await(20, TimeUnit.SECONDS))
        if (error != null) {
            throw AssertionError("Search failed: ${error?.message}", error)
        }
        assertTrue("Hinton campsite was not found", foundHinton)
        assertTrue("Campsite with insufficient capacity returned", correctCapacity)
    }

    @Test
    fun testGetAvailability() {
        val repo = AvailabilityRepository()
        val latch = CountDownLatch(1)
        var dates = emptyList<com.example.zephyr_bbb.database.model.AvailabilityDay>()
        var error: Exception? = null
        repo.getAvailability(
            campsiteId = "TEST301_13a01bf3_camp_3",
            onSuccess = {
                dates = it
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )
        assertTrue("Availability timed out", latch.await(20, TimeUnit.SECONDS))
        if (error != null) {
            throw AssertionError("Availability failed: ${error?.message}", error)
        }
        assertEquals(3, dates.size)
        assertEquals("open", dates.find { it.date == "2027-05-14" }?.status)
        assertEquals("blocked", dates.find { it.date == "2027-05-15" }?.status)
        assertEquals("open", dates.find { it.date == "2027-05-16" }?.status)
    }

    @Test
    fun testGetLottery() {
        val repo = LotteryRepository()
        val latch = CountDownLatch(1)
        var lottery: com.example.zephyr_bbb.database.model.Lottery? = null
        var error: Exception? = null
        repo.getLottery(
            lotteryId = "TEST301_13a01bf3_lottery_1",
            onSuccess = {
                lottery = it
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )
        assertTrue("Lottery timed out", latch.await(20, TimeUnit.SECONDS))
        if (error != null) {
            throw AssertionError("Lottery failed: ${error?.message}", error)
        }
        assertNotNull("Lottery not found", lottery)
        assertEquals("TEST Open Lottery", lottery?.title)
        assertEquals("open", lottery?.status)
        assertEquals(2L, lottery?.winnerCount)
        assertEquals(10L, lottery?.waitlistCapacity)
    }

    // USER TESTS

    @Test
    fun testGetUser() {

        val repo = UserRepository()
        val latch = CountDownLatch(1)
        val uid = FirebaseProvider.auth.currentUser!!.uid

        var user: com.example.zephyr_bbb.database.model.User? = null
        var error: Exception? = null

        repo.getUser(
            userId = uid,
            onSuccess = {
                user = it
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )

        assertTrue("Timed out", latch.await(20, TimeUnit.SECONDS))

        if (error != null) {
            throw AssertionError("Get user failed: ${error?.message}", error)
        }

        assertNotNull(user)
        assertEquals(uid, user?.id)
        assertEquals("active", user?.status)
    }

    @Test
    fun testGetOrCreateUser() {

        val repo = UserRepository()
        val latch = CountDownLatch(1)
        val uid = FirebaseProvider.auth.currentUser!!.uid

        var user: com.example.zephyr_bbb.database.model.User? = null
        var error: Exception? = null

        repo.getOrCreateCurrentUser(
            onSuccess = {
                user = it
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )

        assertTrue("Timed out", latch.await(20, TimeUnit.SECONDS))

        if (error != null) {
            throw AssertionError("User creation failed: ${error?.message}", error)
        }

        assertNotNull(user)
        assertEquals(uid, user?.id)
        assertTrue(user!!.roles.contains("entrant"))
    }


    // CAMPSITE TESTS

    @Test
    fun testGetMissingCampsite() {

        val repo = CampsiteRepository()
        val latch = CountDownLatch(1)

        var camp: com.example.zephyr_bbb.database.model.Campsite? = null
        var error: Exception? = null

        repo.getCampsite(
            campsiteId = "TEST301_missing_campsite",
            onSuccess = {
                camp = it
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )

        assertTrue("Timed out", latch.await(20, TimeUnit.SECONDS))

        if (error != null) {
            throw AssertionError("Get campsite failed: ${error?.message}", error)
        }

        assertNull(camp)
    }

    @Test
    fun testSearchCampsitesByDate() {

        val repo = CampsiteRepository()
        val latch = CountDownLatch(1)

        var camps = emptyList<com.example.zephyr_bbb.database.model.Campsite>()
        var error: Exception? = null

        repo.searchCampsites(
            searchText = "TEST",
            checkInDate = "2027-05-14",
            checkOutDate = "2027-05-16",
            onSuccess = {
                camps = it
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )

        assertTrue("Search timed out", latch.await(20, TimeUnit.SECONDS))

        if (error != null) {
            throw AssertionError("Search failed: ${error?.message}", error)
        }

        // Hinton is blocked on May 15
        assertFalse(camps.any {
            it.id == "TEST301_13a01bf3_camp_3"
        })

        assertTrue(camps.any {
            it.id == "TEST301_13a01bf3_camp_1"
        })

        assertTrue(camps.any {
            it.id == "TEST301_13a01bf3_camp_2"
        })
    }


    // LOTTERY TESTS

    @Test
    fun testGetMissingLottery() {

        val repo = LotteryRepository()
        val latch = CountDownLatch(1)

        var lottery: com.example.zephyr_bbb.database.model.Lottery? = null
        var error: Exception? = null

        repo.getLottery(
            lotteryId = "TEST301_missing_lottery",
            onSuccess = {
                lottery = it
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )

        assertTrue("Timed out", latch.await(20, TimeUnit.SECONDS))

        if (error != null) {
            throw AssertionError("Get lottery failed: ${error?.message}", error)
        }

        assertNull(lottery)
    }

    @Test
    fun testGetActiveLotteries() {

        val repo = LotteryRepository()
        val latch = CountDownLatch(1)

        var lotteries = emptyList<com.example.zephyr_bbb.database.model.Lottery>()
        var error: Exception? = null

        repo.getActiveLotteriesForCampsite(
            campsiteId = "TEST301_13a01bf3_camp_1",
            onSuccess = {
                lotteries = it
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )

        assertTrue("Timed out", latch.await(20, TimeUnit.SECONDS))

        if (error != null) {
            throw AssertionError("Active lotteries failed: ${error?.message}", error)
        }

        assertTrue(lotteries.any {
            it.id == "TEST301_13a01bf3_lottery_1"
        })

        assertTrue(lotteries.all {
            it.status == "open"
        })
    }

    @Test
    fun testGetWaitlist() {

        val repo = LotteryRepository()
        val latch = CountDownLatch(1)

        var entries = emptyList<com.example.zephyr_bbb.database.model.LotteryEntry>()
        var error: Exception? = null

        repo.getWaitlist(
            lotteryId = "TEST301_13a01bf3_lottery_1",
            onSuccess = {
                entries = it
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )

        assertTrue("Timed out", latch.await(20, TimeUnit.SECONDS))

        if (error != null) {
            throw AssertionError("Waitlist failed: ${error?.message}", error)
        }

        assertTrue(entries.all {
            it.lotteryId == "TEST301_13a01bf3_lottery_1"
        })
    }

    @Test
    fun testGetMyLotteryEntry() {

        val repo = LotteryRepository()
        val latch = CountDownLatch(1)

        var entry: com.example.zephyr_bbb.database.model.LotteryEntry? = null
        var error: Exception? = null

        repo.getMyEntry(
            lotteryId = "TEST301_13a01bf3_lottery_1",
            onSuccess = {
                entry = it
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )

        assertTrue("Timed out", latch.await(20, TimeUnit.SECONDS))

        if (error != null) {
            throw AssertionError("Get entry failed: ${error?.message}", error)
        }

        val uid = FirebaseProvider.auth.currentUser!!.uid

        // An existing entry must belong to this user
        if (entry != null) {
            assertEquals(uid, entry?.entrantId)
            assertEquals("TEST301_13a01bf3_lottery_1", entry?.lotteryId)
        }
    }


    // RESERVATION TESTS

    @Test
    fun testGetMissingReservation() {

        val repo = ReservationRepository()
        val latch = CountDownLatch(1)

        var reservation: com.example.zephyr_bbb.database.model.Reservation? = null
        var error: Exception? = null

        repo.getReservation(
            reservationId = "TEST301_missing_reservation",
            onSuccess = {
                reservation = it
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )

        assertTrue("Timed out", latch.await(20, TimeUnit.SECONDS))

        if (error != null) {
            throw AssertionError("Reservation failed: ${error?.message}", error)
        }

        assertNull(reservation)
    }

    @Test
    fun testGetMyReservations() {

        val repo = ReservationRepository()
        val latch = CountDownLatch(1)

        var reservations = emptyList<com.example.zephyr_bbb.database.model.Reservation>()
        var error: Exception? = null

        repo.getMyReservations(
            onSuccess = {
                reservations = it
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )

        assertTrue("Timed out", latch.await(20, TimeUnit.SECONDS))

        if (error != null) {
            throw AssertionError("Reservations failed: ${error?.message}", error)
        }

        val uid = FirebaseProvider.auth.currentUser!!.uid

        assertTrue(reservations.all {
            it.entrantId == uid
        })
    }


    // NOTIFICATION TEST

    @Test
    fun testGetMyNotifications() {

        val repo = NotificationRepository()
        val latch = CountDownLatch(1)

        var notifications = emptyList<com.example.zephyr_bbb.database.model.AppNotification>()
        var error: Exception? = null

        repo.getMyNotifications(
            onSuccess = {
                notifications = it
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )

        assertTrue("Timed out", latch.await(20, TimeUnit.SECONDS))

        if (error != null) {
            throw AssertionError("Notifications failed: ${error?.message}", error)
        }

        val uid = FirebaseProvider.auth.currentUser!!.uid

        assertTrue(notifications.all {
            it.recipientId == uid
        })
    }


    // ADMIN TEST

    @Test
    fun testAdminAccessDenied() {

        val repo = AdminRepository()
        val latch = CountDownLatch(1)

        var error: Exception? = null
        var succeeded = false

        repo.getCampsites(
            onSuccess = {
                succeeded = true
                latch.countDown()
            },
            onFailure = {
                error = it
                latch.countDown()
            }
        )

        assertTrue("Timed out", latch.await(20, TimeUnit.SECONDS))
        assertFalse("Non-admin user was given admin access", succeeded)
        assertNotNull("Expected permission error", error)
    }

}
