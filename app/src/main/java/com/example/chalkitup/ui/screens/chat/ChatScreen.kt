package com.example.chalkitup.ui.screens.chat

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chalkitup.ui.viewmodel.chat.ChatViewModel
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import com.example.chalkitup.domain.model.User
import com.example.chalkitup.ui.components.ProfilePictureIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Composable
fun ChatScreen(
    navController: NavController,
    chatViewModel: ChatViewModel,
    conversationId: String?,
    selectedUserId: String
) {

    val messages by chatViewModel.messages.collectAsState()
    var text by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedUser by remember { mutableStateOf<User?>(null) }


    // Fetch the selected user's data when screen loads
    LaunchedEffect(selectedUserId) {
        chatViewModel.viewModelScope.launch {
            selectedUser = chatViewModel.fetchUser(selectedUserId)
        }
    }

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


    // Group messages by day
    val groupedMessages = remember(messages) {
        messages.groupBy { message ->
            val date = Calendar.getInstance().apply { timeInMillis = message.timestamp }
            "${date.get(Calendar.YEAR)}-" +
            "${date.get(Calendar.MONTH)}-" +
            "${date.get(Calendar.DAY_OF_MONTH)}"
        }.toSortedMap()
    }


    Scaffold(
        topBar = {
            if (selectedUser != null) {
                ChatAppBar(
                    user = selectedUser!!,
                    navController = navController
                )
            } else {
                // Show a placeholder the user data hasn't been fetched yet
                TopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Display chat messages
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Display messages and date stamp
                groupedMessages.forEach { (_, messagesForDay) ->
                    val firstTimestamp =
                        messagesForDay.minByOrNull { it.timestamp }?.timestamp ?: 0L
                    item {
                        // Date header
                        Text(
                            text = formatDateHeader(firstTimestamp),
                            style = MaterialTheme.typography.caption,
                            color = Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.Center)
                                .padding(vertical = 8.dp)
                        )
                    }

                    items(messagesForDay) { message ->
                        ChatBubble(
                            message = message.text,
                            isCurrentUser = message.senderId == chatViewModel.currentUserId
                        )
                    }
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
                        .padding(end = 6.dp),
                    placeholder = { Text("Message...") }
                )

                // Send messages
                Button(onClick = {
                    if (text.isNotBlank()) {
//                        chatViewModel.viewModelScope.launch {
                        coroutineScope.launch {

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

                                // Scroll after message is sent
                                withContext(Dispatchers.Main.immediate) {
                                    if (messages.size > 0) {
                                        scrollState.animateScrollToItem(messages.size - 1)
                                    }
                                }
                            }
                        }
                    }
                }) {
                    Text("Send")
                }
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
            modifier = Modifier
                .widthIn(max = 300.dp),
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

private fun formatTime(timestamp: Long): String {
    // "9:11 AM" format
    return SimpleDateFormat("h:mm a", Locale.getDefault())
        .format(Date(timestamp))
}

private fun formatDateHeader(timestamp: Long): String {
    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }
    val timeString = formatTime(timestamp)

    return when {
        // If the message was sent yesterday
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) ->
            "Today, $timeString"

        // If the message was sent yesterday
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - messageTime.get(Calendar.DAY_OF_YEAR) == 1 ->
            "Yesterday, $timeString"

        // Display full date and time
        else -> {
            val dateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())
            "${dateFormatter.format(Date(timestamp))}, $timeString"
        }
    }

}

@Composable
fun ChatAppBar(
    user: User,
    navController: NavController
) {
    val userPictureUrl = user.userProfilePictureUrl

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfilePictureIcon(profilePictureUrl = userPictureUrl)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "${user.firstName} ${user.lastName}")
            }
        }

    )
}
