package com.example.chalkitup.ui.screens.chat

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chalkitup.ui.viewmodel.ChatViewModel
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


@Composable
fun ChatScreen(
    navController: NavController,
    chatViewModel: ChatViewModel,
    conversationId: String?,
    selectedUserId: String
) {

    val messages by chatViewModel.messages.collectAsState()
    var text by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState(initialFirstVisibleItemIndex = messages.size)

    LaunchedEffect(conversationId) {
        if (conversationId.isNullOrBlank()) {
            chatViewModel.clearMessages()
        } else {
            chatViewModel.setConversationId(conversationId)
        }
    }

    // Auto scroll to most recent message
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(index = messages.size - 1)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Display chat messages
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(messages) { message ->
                ChatBubble(
                    message = message.text,
                    isCurrentUser = message.senderId == chatViewModel.currentUserId
                )
            }
        }

        // Chat input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = { Text("Message...") }
            )

            Button(onClick = {
                if (text.isNotBlank()) {
                    chatViewModel.viewModelScope.launch {
                        if (conversationId.isNullOrEmpty()) {
                            // Fetch user details
                            val currentUser = chatViewModel.fetchUser(chatViewModel.currentUserId)
                            val selectedUser = chatViewModel.fetchUser(selectedUserId)

                            // Create a new conversation
                            val newConversationId = chatViewModel.createConversation(currentUser, selectedUser)

                            // If the conversation was successfully created, send the message
                            if (!newConversationId.isNullOrEmpty()) {
                                chatViewModel.setConversationId(newConversationId)
                                chatViewModel.sendMessage(newConversationId, text)
                                text = ""
                            } else {
                                Log.e("Chat", "Failed to create a new conversation.")
                            }
                        } else {
                            chatViewModel.sendMessage(conversationId, text)
                            text = ""
                        }
                    }
                }
            }) {
                Text("Send")
            }
        }
    }

}



@Composable
fun ChatBubble(
    message: String,
    isCurrentUser: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (isCurrentUser) Color.Blue else Color.LightGray,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = message,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = if (isCurrentUser) Color.White else Color.Black
            )
        }
    }
}

