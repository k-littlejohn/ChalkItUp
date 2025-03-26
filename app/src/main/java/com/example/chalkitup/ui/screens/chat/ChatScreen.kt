package com.example.chalkitup.ui.screens.chat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.graphics.Brush
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

    // Gradient brush for the screen's background.
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF54A4FF), // 5% Blue
            androidx.compose.material3.MaterialTheme.colorScheme.surface, androidx.compose.material3.MaterialTheme.colorScheme.surface,
        )
    )

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


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (selectedUser != null) {
                    ChatAppBar(
                        user = selectedUser!!,
                        navController = navController,
                    )
                } else {
                    // Show a placeholder the user data hasn't been fetched yet
                    TopAppBar(
                        backgroundColor = Color.Transparent, // Make background transparent
                        elevation = 0.dp, // Remove shadow
                        contentColor = MaterialTheme.colors.onSurface, // Maintain text/icons visibility
                        title = { Text("") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colors.onSurface // Ensure icon is visible
                                )
                            }
                        }
                    )
                }
            }
        ) { innerPadding  ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding )
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
                ChatInputBar(
                    text = text,
                    onTextChange = { text = it },
                    onSend = {
                        if (text.isNotBlank()) {

                            val messageToSend = text
                            text = ""

                            coroutineScope.launch {

                                if (conversationId.isNullOrEmpty()) {
                                    // Fetch user details
                                    val currentUser =
                                        chatViewModel.fetchUser(chatViewModel.currentUserId)
                                    val selectedUser = chatViewModel.fetchUser(selectedUserId)

                                    // Create a new conversation
                                    val newConversationId =
                                        chatViewModel.createConversation(currentUser, selectedUser)

                                    // If the conversation was successfully created, send the message
                                    if (!newConversationId.isNullOrEmpty()) {
                                        chatViewModel.setConversationId(newConversationId)
                                        chatViewModel.sendMessage(newConversationId, messageToSend)
                                        text = ""
                                    } else {
                                        Log.e("Chat", "Failed to create a new conversation.")
                                    }
                                } else {
                                    chatViewModel.sendMessage(conversationId, messageToSend)
                                    text = ""

                                    // Scroll to latest message after message is sent
                                    withContext(Dispatchers.Main.immediate) {
                                        if (messages.size > 0) {
                                            scrollState.animateScrollToItem(messages.size - 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
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
            // Text inside the bubble
            Text(
                text = message,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                color = if (isCurrentUser) Color.White else Color.Black
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault())  // "9:11 AM" format
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
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        placeholder = {
            Text(
                text = "Message...",
                style = MaterialTheme.typography.body1
            )
        },
        textStyle = MaterialTheme.typography.body1,
        shape = RoundedCornerShape(24.dp), // Rounded corners
        trailingIcon = {
            IconButton(onClick = onSend) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send"
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    )
}

@Composable
fun ChatAppBar(
    user: User,
    navController: NavController,
) {
    val userPictureUrl = user.userProfilePictureUrl

    Box {
        TopAppBar(
            backgroundColor = Color.Transparent, // Make background transparent
            elevation = 0.dp, // Remove shadow
            contentColor = MaterialTheme.colors.onSurface, // Maintain text/icons visibility
            navigationIcon = {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.padding(end = (0).dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
//                        modifier = Modifier.size(24.dp), // Standard icon size
                        tint = MaterialTheme.colors.onSurface // Ensure icon is visible
                    )
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 0.dp)
                ) {
                    ProfilePictureIcon(profilePictureUrl = userPictureUrl)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        color = MaterialTheme.colors.onSurface
                    )
                }
            },
            modifier = Modifier.padding(horizontal = 0.dp) // Remove app bar's default padding
        )

        // Add divider at the bottom
        Divider(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.LightGray.copy(alpha = 0.5f),
            thickness = 0.5.dp
        )

    }

}