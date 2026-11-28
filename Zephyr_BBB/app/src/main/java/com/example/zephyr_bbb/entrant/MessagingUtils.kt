package com.example.zephyr_bbb.entrant

import com.example.zephyr_bbb.database.model.OwnerMessage
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Small helpers for the entrant's chat with a campsite owner, kept free of
 * Compose and Firebase calls so they can be unit tested. US 01.14.01.
 */
internal object MessagingUtils {

    /**
     * Whether the Send button should be enabled.
     *
     * @param body the text currently typed in the message box
     * @param isSending true while a message is already being sent
     * @return true if there's something to send and nothing is sending
     */
    fun canSend(body: String, isSending: Boolean): Boolean = body.isNotBlank() && !isSending

    /**
     * Puts messages oldest first. A message with no time yet (one that's still being
     * saved on this phone) goes last, since it was just sent.
     *
     * @param messages the messages in any order
     * @return a new list, oldest first
     */
    fun sortChronologically(messages: List<OwnerMessage>): List<OwnerMessage> =
        messages.sortedWith(compareBy(nullsLast()) { it.createdAt })

    /**
     * Whether a message was sent by the signed-in user.
     *
     * @param message the message to check
     * @param myUid the signed-in user's id
     */
    fun isFromMe(message: OwnerMessage, myUid: String): Boolean = message.senderId == myUid

    /**
     * Formats when a message was sent, e.g. "Oct 17, 9:00 AM".
     *
     * @param timestamp when the message was sent, or null if it hasn't been saved yet
     * @param locale the locale for month names and AM/PM
     * @param zone the time zone to show the time in
     * @return the formatted time, or "" if [timestamp] is null
     */
    fun formatTime(
        timestamp: Timestamp?,
        locale: Locale = Locale.getDefault(),
        zone: TimeZone = TimeZone.getDefault()
    ): String {
        if (timestamp == null) return ""
        val format = SimpleDateFormat("MMM d, h:mm a", locale).apply { timeZone = zone }
        return format.format(timestamp.toDate())
    }

    /**
     * The line at the top of the chat saying which stay it's about,
     * e.g. "Pine Creek A12 · 2026-10-25 to 2026-10-27".
     *
     * @param campsiteName the campsite's name, or null while it's loading
     * @param checkInDate the reservation's check-in date
     * @param checkOutDate the reservation's check-out date
     */
    fun header(campsiteName: String?, checkInDate: String, checkOutDate: String): String {
        val name = campsiteName?.takeIf { it.isNotBlank() } ?: "Campsite"
        return "$name · $checkInDate to $checkOutDate"
    }

    /**
     * Turns stored messages into chat bubbles for the screen, oldest first.
     *
     * @param messages the reservation's messages in any order
     * @param myUid the signed-in user's id, used to put their messages on the right
     * @param locale the locale for the time under each bubble
     * @param zone the time zone for the time under each bubble
     */
    fun toBubbles(
        messages: List<OwnerMessage>,
        myUid: String,
        locale: Locale = Locale.getDefault(),
        zone: TimeZone = TimeZone.getDefault()
    ): List<ChatBubble> = sortChronologically(messages).map {
        ChatBubble(
            id = it.id,
            body = it.body,
            time = formatTime(it.createdAt, locale, zone),
            fromMe = isFromMe(it, myUid)
        )
    }
}

/**
 * One message as the chat screen shows it, so the screen doesn't depend on Firestore types.
 *
 * @property id the message's id
 * @property body the message text
 * @property time when it was sent, already formatted, or "" if it's still sending
 * @property fromMe true if the signed-in user sent it
 */
internal data class ChatBubble(
    val id: String,
    val body: String,
    val time: String,
    val fromMe: Boolean
)
