package com.example.zephyr_bbb.entrant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.Campsite
import com.example.zephyr_bbb.database.model.OwnerMessage
import com.example.zephyr_bbb.database.model.Reservation
import com.example.zephyr_bbb.database.repository.CampsiteRepository
import com.example.zephyr_bbb.database.repository.MessageRepository
import com.example.zephyr_bbb.database.repository.ReservationRepository

/** Test tag for the message text field on the entrant messaging screen. */
internal const val MESSAGE_INPUT_TAG = "messageInput"

/**
 * Lets an entrant message the campsite owner about one of their reservations.
 * Shows the whole conversation, which updates live when the owner replies. US 01.14.01.
 *
 * This is the stateful wrapper: it signs the device in, loads the reservation and
 * campsite for the header, listens to [MessageRepository] for messages, and passes
 * plain values down to [EntrantMessagingContent].
 *
 * @param reservationId the reservation the conversation is about
 * @param onBackClick is invoked when the user clicks the back button
 * @param modifier lets the caller tell this screen about padding it has already
 * applied (like the system bars), so the keyboard padding isn't added twice
 */
@Composable
fun EntrantMessagingScreen(
    reservationId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DeviceIdentityGate(modifier = modifier) { user ->
        val reservationRepository = remember { ReservationRepository() }
        val campsiteRepository = remember { CampsiteRepository() }
        val messageRepository = remember { MessageRepository() }

        var reservation by remember { mutableStateOf<Reservation?>(null) }
        var campsite by remember { mutableStateOf<Campsite?>(null) }
        var messages by remember { mutableStateOf<List<OwnerMessage>>(emptyList()) }
        var draft by remember { mutableStateOf("") }
        var isSending by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Load the reservation and its campsite for the header, like the reservation details screen
        LaunchedEffect(reservationId) {
            reservationRepository.getReservation(
                reservationId,
                onSuccess = { loaded ->
                    reservation = loaded
                    if (loaded == null) {
                        errorMessage = "Reservation not found."
                    } else {
                        campsiteRepository.getCampsite(
                            loaded.campsiteId,
                            onSuccess = { site -> campsite = site },
                            onFailure = { e -> errorMessage = e.message }
                        )
                    }
                },
                onFailure = { errorMessage = it.message ?: "Could not load the reservation." }
            )
        }

        // Listen for messages while this screen is open, so owner replies show up right away.
        // Only asks for messages this user sent or received, which is what the security rules allow
        DisposableEffect(reservationId) {
            val listener = messageRepository.observeMyMessagesForReservation(
                reservationId = reservationId,
                onSuccess = { messages = it },
                onFailure = { errorMessage = it.message ?: "Could not load messages." }
            )
            onDispose { listener.remove() }
        }

        val loadedReservation = reservation
        EntrantMessagingContent(
            header = if (loadedReservation == null) {
                ""
            } else {
                MessagingUtils.header(
                    campsite?.name,
                    loadedReservation.checkInDate,
                    loadedReservation.checkOutDate
                )
            },
            bubbles = MessagingUtils.toBubbles(messages, user.id),
            draft = draft,
            onDraftChange = { draft = it },
            // Messages are tied to the reservation, so wait until it has loaded
            canSend = loadedReservation != null && MessagingUtils.canSend(draft, isSending),
            onSendClick = {
                isSending = true
                errorMessage = null
                messageRepository.sendMessage(
                    reservationId = reservationId,
                    body = draft,
                    onSuccess = {
                        isSending = false
                        draft = ""
                    },
                    onFailure = {
                        isSending = false
                        errorMessage = it.message ?: "Could not send your message."
                    }
                )
            },
            errorMessage = errorMessage,
            onBackClick = onBackClick,
            modifier = modifier
        )
    }
}

/**
 * The entrant messaging screen's UI with no Firebase access, so it can be tested on its own.
 * US 01.14.01.
 *
 * @param header which campsite and dates the conversation is about, or "" while loading
 * @param bubbles the messages to show, oldest first
 * @param draft the text currently typed in the message box
 * @param onDraftChange is invoked when the user edits the message
 * @param canSend whether the Send button is enabled
 * @param onSendClick is invoked when the user clicks Send
 * @param errorMessage an error to show, or null
 * @param onBackClick is invoked when the user clicks the back button
 * @param modifier applied to the whole screen
 */
@Composable
internal fun EntrantMessagingContent(
    header: String,
    bubbles: List<ChatBubble>,
    draft: String,
    onDraftChange: (String) -> Unit,
    canSend: Boolean,
    onSendClick: () -> Unit,
    errorMessage: String?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Keep the newest message in view when one arrives
    LaunchedEffect(bubbles.size) {
        if (bubbles.isNotEmpty()) listState.animateScrollToItem(bubbles.lastIndex)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            // Moves the message box up above the keyboard
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onBackClick) { Text("Back") }
        Text("Message owner", style = MaterialTheme.typography.headlineMedium)
        if (header.isNotEmpty()) {
            Text(header, style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider()

        if (bubbles.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(
                    "No messages yet. Ask the owner about your stay.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(bubbles, key = { it.id }) { bubble -> ChatBubbleRow(bubble) }
            }
        }

        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text("Message") },
                maxLines = 4,
                modifier = Modifier
                    .weight(1f)
                    .testTag(MESSAGE_INPUT_TAG)
            )
            Button(onClick = onSendClick, enabled = canSend) { Text("Send") }
        }
    }
}

/**
 * One chat bubble. The entrant's own messages sit on the right in a light green,
 * and the owner's sit on the left in grey. US 01.14.01.
 *
 * @param bubble the message to show
 */
@Composable
private fun ChatBubbleRow(bubble: ChatBubble) {
    val sender = if (bubble.fromMe) "You" else "Owner"
    Row(
        horizontalArrangement = if (bubble.fromMe) Arrangement.End else Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            // Screen readers read the bubble as one line, e.g. "You: Is there firewood?, Oct 17, 9:00 AM"
            .clearAndSetSemantics {
                contentDescription = buildString {
                    append("$sender: ${bubble.body}")
                    if (bubble.time.isNotEmpty()) append(", ${bubble.time}")
                }
            }
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            // A light tint of the theme green for your own messages (the theme doesn't
            // set primaryContainer, so that would fall back to the default purple)
            color = if (bubble.fromMe) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(bubble.body, style = MaterialTheme.typography.bodyLarge)
                if (bubble.time.isNotEmpty()) {
                    Text(bubble.time, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
