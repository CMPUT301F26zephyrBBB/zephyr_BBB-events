package com.example.zephyr_bbb

import com.example.zephyr_bbb.database.model.AppNotification
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.ImageRecord
import com.example.zephyr_bbb.database.model.Lottery
import com.example.zephyr_bbb.database.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// unit tests for the models shown on the admin screens
class AdminModelTest {

    // new users should be active entrants
    @Test
    fun newUserIsActiveEntrantByDefault() {
        val user = User(id = "uid-faizaan")

        assertEquals(listOf("entrant"), user.roles)
        assertEquals("active", user.status)
        assertTrue(user.notificationsEnabled)
        assertNull(user.profileImageId)
    }

    // an admin can also be an owner and entrant
    @Test
    fun userCanHoldOwnerAndAdminRolesTogether() {
        val user = User(
            id = "uid-faizaan",
            name = "Faizaan Ruda",
            roles = listOf("entrant", "owner", "admin")
        )

        assertTrue("entrant" in user.roles)
        assertTrue("owner" in user.roles)
        assertTrue("admin" in user.roles)
    }

    // a plain entrant has no owner or admin role
    @Test
    fun entrantIsNotOwnerOrAdmin() {
        val user = User(id = "uid-faizaan", name = "Faizaan Ruda")

        assertFalse("owner" in user.roles)
        assertFalse("admin" in user.roles)
    }

    // removed profiles clear personal info but keep id and roles
    @Test
    fun removedProfileHasPersonalDetailsCleared() {
        val original = User(
            id = "uid-manroj",
            name = "Manroj Gill",
            email = "manroj.gill@ualberta.ca",
            phone = "780-555-0142",
            roles = listOf("entrant", "owner"),
            profileImageId = "img-profile-manroj"
        )

        val removed = original.copy(
            name = "",
            email = "",
            phone = "",
            profileImageId = null,
            notificationsEnabled = false,
            status = "removed"
        )

        assertEquals("uid-manroj", removed.id)
        assertEquals(listOf("entrant", "owner"), removed.roles)
        assertEquals("", removed.name)
        assertEquals("", removed.email)
        assertEquals("removed", removed.status)

        // original profile should not change
        assertEquals("Manroj Gill", original.name)
    }

    // new campsites start as drafts priced in CAD
    @Test
    fun newCampsiteStartsAsDraftInCad() {
        val campsite = Campsite(id = "site-two-jack")

        assertEquals("draft", campsite.status)
        assertEquals("CAD", campsite.currency)
        assertEquals(1L, campsite.maxOccupancy)
        assertNull(campsite.promoImageId)
    }

    // campsite keeps the details shown on the admin details screen
    @Test
    fun campsiteKeepsDetailsShownToAdmin() {
        val campsite = Campsite(
            id = "site-two-jack",
            ownerId = "uid-manroj",
            name = "Two Jack Lakeside Site 14",
            locationName = "Banff National Park, AB",
            maxOccupancy = 6,
            pricePerNightCents = 3850,
            status = "active"
        )

        assertEquals("Two Jack Lakeside Site 14", campsite.name)
        assertEquals("Banff National Park, AB", campsite.locationName)
        assertEquals(6L, campsite.maxOccupancy)
        assertEquals("active", campsite.status)
    }

    // prices are stored in cents
    @Test
    fun priceIsStoredInCents() {
        val campsite = Campsite(pricePerNightCents = 3850)

        assertEquals(3850L, campsite.pricePerNightCents)
    }

    // new images default to active profile jpegs
    @Test
    fun newImageIsActiveProfileJpeg() {
        val image = ImageRecord(id = "img-1")

        assertEquals("profile", image.targetType)
        assertEquals("image/jpeg", image.contentType)
        assertEquals("active", image.status)
    }

    // campsite images point at their campsite
    @Test
    fun campsiteImagePointsAtItsCampsite() {
        val image = ImageRecord(
            id = "img-campsite-two-jack",
            uploaderId = "uid-manroj",
            targetType = "campsite",
            targetId = "site-two-jack"
        )

        assertEquals("campsite", image.targetType)
        assertEquals("site-two-jack", image.targetId)
        assertEquals("uid-manroj", image.uploaderId)
    }

    // new notifications are pending and unread
    @Test
    fun newNotificationIsPendingAndUnread() {
        val notification = AppNotification(id = "notif-1")

        assertEquals("general", notification.type)
        assertEquals("pending", notification.deliveryStatus)
        assertNull(notification.readAt)
    }

    // notification keeps sender and recipient for the admin log
    @Test
    fun notificationKeepsSenderAndRecipient() {
        val notification = AppNotification(
            id = "notif-won",
            senderId = "uid-manroj",
            recipientId = "uid-faizaan",
            type = "lottery_won",
            title = "You won the Banff Long Weekend Draw!",
            body = "Book Two Jack Lakeside Site 14 before Aug 1.",
            deliveryStatus = "sent"
        )

        assertEquals("uid-manroj", notification.senderId)
        assertEquals("uid-faizaan", notification.recipientId)
        assertEquals("sent", notification.deliveryStatus)
    }

    // a lottery can offer more than one campsite
    @Test
    fun lotteryCanOfferSeveralCampsites() {
        val lottery = Lottery(
            id = "lottery-banff-august",
            title = "Banff Long Weekend Draw",
            campsiteIds = listOf("site-tunnel-mtn", "site-two-jack")
        )

        assertEquals(2, lottery.campsiteIds.size)
        assertTrue("site-two-jack" in lottery.campsiteIds)
    }

    // new lotteries start as drafts with one winner
    @Test
    fun newLotteryIsDraftWithOneWinner() {
        val lottery = Lottery(id = "lottery-1")

        assertEquals("draft", lottery.status)
        assertEquals(1L, lottery.winnerCount)
        assertEquals(0L, lottery.entryCount)
        assertNull(lottery.waitlistCapacity)
    }
}