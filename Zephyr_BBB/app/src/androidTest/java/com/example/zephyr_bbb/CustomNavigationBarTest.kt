package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.ui.theme.Zephyr_BBBTheme
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Tests for CustomNavigationBar.kt
// Tests to make sure the navigation bar initializes properly, and that it is displaying all items,
// and that makes sure that it changes screens when a different button is hit.

@RunWith(AndroidJUnit4::class)
class CustomNavigationBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun navigationBottomBarDisplaysAllItems(){
        composeTestRule.setContent {
            Zephyr_BBBTheme() {
                NavigationBottomBar(
                    currentRoute = "home",
                    onItemClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Campsites").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lotteries").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reservations").assertIsDisplayed()
    }

    @Test
    fun navigationBottomBarItemClick(){
        var clickedItemRoute: String? = null
        composeTestRule.setContent {
            Zephyr_BBBTheme() {
                NavigationBottomBar(
                    currentRoute = "home",
                    onItemClick = {item->clickedItemRoute = item.route}
                )
            }
        }
        composeTestRule.onNodeWithText("Campsites").performClick()
        assertEquals("campsites", clickedItemRoute)
        composeTestRule.onNodeWithText("Lotteries").performClick()
        assertEquals("lotteries", clickedItemRoute)
        composeTestRule.onNodeWithText("Reservations").performClick()
        assertEquals("reservations", clickedItemRoute)
        composeTestRule.onNodeWithText("Home").performClick()
        assertEquals("home", clickedItemRoute)
    }
}