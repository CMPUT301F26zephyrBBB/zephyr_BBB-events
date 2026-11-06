package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.admin.AdminDashboardScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// ui tests for the admin dashboard menu
@RunWith(AndroidJUnit4::class)
class AdminDashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // tracks which button was pressed
    private var lastClicked: String? = null

    // show the dashboard before each test
    @Before
    fun setUp() {
        lastClicked = null

        composeTestRule.setContent {
            AdminDashboardScreen(
                onBackClick = { lastClicked = "back" },
                onProfilesClick = { lastClicked = "profiles" },
                onCampsitesClick = { lastClicked = "campsites" },
                onImagesClick = { lastClicked = "images" },
                onNotificationsClick = { lastClicked = "notifications" }
            )
        }
    }

    // all admin tools should be visible
    @Test
    fun dashboardShowsAllAdminTools() {
        composeTestRule.onNodeWithText("Admin Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("All Profiles").assertIsDisplayed()
        composeTestRule.onNodeWithText("All Campsites").assertIsDisplayed()
        composeTestRule.onNodeWithText("All Uploaded Images").assertIsDisplayed()
        composeTestRule.onNodeWithText("All Notifications").assertIsDisplayed()
    }

    // profiles button opens the profile browser
    @Test
    fun profilesButtonOpensProfileBrowser() {
        composeTestRule.onNodeWithText("All Profiles").performClick()
        assertEquals("profiles", lastClicked)
    }

    // campsites button opens the campsite browser
    @Test
    fun campsitesButtonOpensCampsiteBrowser() {
        composeTestRule.onNodeWithText("All Campsites").performClick()
        assertEquals("campsites", lastClicked)
    }

    // images button opens the image browser
    @Test
    fun imagesButtonOpensImageBrowser() {
        composeTestRule.onNodeWithText("All Uploaded Images").performClick()
        assertEquals("images", lastClicked)
    }

    // notifications button opens the notification log
    @Test
    fun notificationsButtonOpensNotificationLog() {
        composeTestRule.onNodeWithText("All Notifications").performClick()
        assertEquals("notifications", lastClicked)
    }

    // back button leaves the dashboard
    @Test
    fun backButtonLeavesDashboard() {
        composeTestRule.onNodeWithText("Back").performClick()
        assertEquals("back", lastClicked)
    }
}