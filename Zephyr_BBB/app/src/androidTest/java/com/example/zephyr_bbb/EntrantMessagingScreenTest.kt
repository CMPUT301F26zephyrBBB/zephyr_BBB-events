package com.example.zephyr_bbb

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.zephyr_bbb.entrant.ChatBubble
import com.example.zephyr_bbb.entrant.EntrantMessagingContent
import com.example.zephyr_bbb.entrant.MESSAGE_INPUT_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// ui tests for the entrant messaging screen (US 01.14.01)
@RunWith(AndroidJUnit4::class)
class EntrantMessagingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var typed: String? = null
    private var sendClicked = false
    private var backClicked = false

    private val conversation = listOf(
        ChatBubble("1", "Is there firewood?", "Oct 17, 9:00 AM", fromMe = true),
        ChatBubble("2", "Yes, at the front gate.", "Oct 17, 9:05 AM", fromMe = false)
    )

    // shows the messaging content with fake data and records the callbacks
    private fun showMessaging(
        bubbles: List<ChatBubble> = conversation,
        canSend: Boolean = true,
        errorMessage: String? = null
    ) {
        composeTestRule.setContent {
            EntrantMessagingContent(
                header = "Pine Creek A12 · 2026-10-25 to 2026-10-27",
                bubbles = bubbles,
                draft = "",
                onDraftChange = { typed = it },
                canSend = canSend,
                onSendClick = { sendClicked = true },
                errorMessage = errorMessage,
                onBackClick = { backClicked = true }
            )
        }
    }

    // the header says which stay the chat is about
    @Test
    fun headerIsDisplayed() {
        showMessaging()
        composeTestRule.onNodeWithText("Message owner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pine Creek A12 · 2026-10-25 to 2026-10-27").assertIsDisplayed()
    }

    // both sides of the conversation are shown, labelled by who sent them
    @Test
    fun bubblesAreDisplayed() {
        showMessaging()
        composeTestRule.onNodeWithContentDescription("You: Is there firewood?, Oct 17, 9:00 AM")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Owner: Yes, at the front gate., Oct 17, 9:05 AM")
            .assertIsDisplayed()
    }

    // with no messages yet, the user is told they can start the conversation
    @Test
    fun emptyStateShownWithNoMessages() {
        showMessaging(bubbles = emptyList())
        composeTestRule.onNodeWithText("No messages yet. Ask the owner about your stay.")
            .assertIsDisplayed()
    }

    @Test
    fun emptyStateHiddenWithMessages() {
        showMessaging()
        composeTestRule.onNodeWithText("No messages yet. Ask the owner about your stay.")
            .assertDoesNotExist()
    }

    // send can't be pressed with nothing to send
    @Test
    fun sendDisabledWhenCannotSend() {
        showMessaging(canSend = false)
        composeTestRule.onNodeWithText("Send").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Send").performClick()
        assertFalse(sendClicked)
    }

    @Test
    fun sendCallsOnSendClick() {
        showMessaging(canSend = true)
        composeTestRule.onNodeWithText("Send").assertIsEnabled().performClick()
        assertTrue(sendClicked)
    }

    // typing updates the draft
    @Test
    fun typingCallsOnDraftChange() {
        showMessaging()
        composeTestRule.onNodeWithTag(MESSAGE_INPUT_TAG).performTextInput("hello")
        assertEquals("hello", typed)
    }

    @Test
    fun errorIsDisplayed() {
        showMessaging(errorMessage = "Only reservation participants can send messages.")
        composeTestRule.onNodeWithText("Only reservation participants can send messages.")
            .assertIsDisplayed()
    }

    @Test
    fun backCallsOnBackClick() {
        showMessaging()
        composeTestRule.onNodeWithText("Back").performClick()
        assertTrue(backClicked)
    }
}
