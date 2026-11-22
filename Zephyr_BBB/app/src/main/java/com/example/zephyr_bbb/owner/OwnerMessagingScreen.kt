package com.example.zephyr_bbb.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zephyr_bbb.database.model.OwnerMessage
import com.example.zephyr_bbb.database.repository.MessageRepository

// Screen for Messaging
@Composable
fun OwnerMessagingScreen(
    reservationId: String,
    onBackClick: () -> Unit
){
    val messageRepository = remember{
        MessageRepository()
    }
    var messages by remember{
        mutableStateOf<List<OwnerMessage>>(emptyList())
    }
    var messageBody by remember{
        mutableStateOf("")
    }
    var errorMessage by remember{
        mutableStateOf<String?>(null)
    }
    // Knowledge of DisposableEffect came from Google Gemini and Android Developers, Details in SethCitations.txt
    // Updates the messages in real time, and when the function is stopped the messages are no
    // longer updated until the function is called again.
    DisposableEffect(reservationId){
        val listener = messageRepository.observeMessagesForReservation(
            reservationId = reservationId,
            onSuccess = {fetched->
                messages = fetched
            },
            onFailure = {error->
                errorMessage = error.message
            }
        )
        onDispose {
            listener.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
    ){
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ){
            Button(
                onClick = onBackClick
            ){
                Text("<")
            }
            Text("Entrant Messages")
        }
        if (errorMessage != null){
            Text("Error: $errorMessage")
        }
        // Creates all the text messsages on the screen
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ){
            items(messages) {message->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ){
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ){
                        Text(message.body)
                    }
                }
            }
        }
        // Area to input text message
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ){
            OutlinedTextField(
                value = messageBody,
                onValueChange = {messageBody = it},
                modifier = Modifier.weight(1f),
                placeholder = {Text("message")}
            )
            Button(
                onClick = {
                    messageRepository.sendMessage(
                        reservationId = reservationId,
                        body = messageBody,
                        onSuccess = {
                            messageBody = ""
                            errorMessage = null
                        },
                        onFailure = {error->
                            errorMessage = error.message
                        }
                    )
                }
            ){
                Text("Send")
            }
        }
    }
}