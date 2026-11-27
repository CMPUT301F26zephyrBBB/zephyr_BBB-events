package com.example.zephyr_bbb

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.database.model.OwnerMessage
import com.example.zephyr_bbb.owner.OwnerMessageContent
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for OwnerMessagingScreen.kt
 * Tests Buttons, texts displayed on screen, also checks by creating dummy variables.
 */

@RunWith(AndroidJUnit4::class)
class OwnerMessagingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val  fakeMessages = listOf(
        OwnerMessage(
            id = "Message1",
            senderId = "User1",
            reservationId = "Reservation1",
            body = "Hello Test Message"
        ),
        OwnerMessage(
            id = "Message2",
            senderId = "User2",
            reservationId = "Reservation2",
            body = "Good Test Message"
        )
    )

    @Test
    fun ownerMessagingScreenButtons(){
        composeTestRule.setContent{
            OwnerMessageContent(
                messages = fakeMessages,
                messageBody = "",
                errorMessage = null,
                onMessageBodyChange = {},
                onBackClick = {},
                onSendMessage = {}
            )
        }
        composeTestRule.onNodeWithText("Entrant Messages").assertIsDisplayed()
        composeTestRule.onNodeWithText("<").assertIsDisplayed()
        composeTestRule.onNodeWithText("message").assertIsDisplayed()
        composeTestRule.onNodeWithText("Send").assertIsDisplayed()
    }

    @Test
    fun ownerMessagingScreenDisplayFakeMessages(){
        composeTestRule.setContent {
            OwnerMessageContent(
                messages = fakeMessages,
                messageBody = "",
                errorMessage = null,
                onMessageBodyChange = {},
                onBackClick = {},
                onSendMessage = {}
            )
        }
        composeTestRule.onNodeWithText("Hello Test Message").assertIsDisplayed()
        composeTestRule.onNodeWithText("Good Test Message").assertIsDisplayed()
    }

    @Test
    fun ownerMessagingScreenBackWorks() {
        var backClicked = false
        composeTestRule.setContent {
            OwnerMessageContent(
                messages = fakeMessages,
                messageBody = "",
                errorMessage = null,
                onMessageBodyChange = {},
                onBackClick = { backClicked = true },
                onSendMessage = {}
            )
        }
        composeTestRule.onNodeWithText("<").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun ownerMessagingScreenTextInputWorks(){
        composeTestRule.setContent{
            var text by remember { mutableStateOf("") }
            OwnerMessageContent(
                messages = fakeMessages,
                messageBody = text,
                errorMessage = null,
                onMessageBodyChange = { text = it },
                onBackClick = {},
                onSendMessage = {}
            )
        }
        composeTestRule.onNodeWithText("message").performTextInput("Hello Test")
        composeTestRule.onNodeWithText("Hello Test").assertIsDisplayed()
    }
}