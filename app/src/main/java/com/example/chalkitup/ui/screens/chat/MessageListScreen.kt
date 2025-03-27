package com.example.chalkitup.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chalkitup.domain.model.Conversation
import com.example.chalkitup.domain.model.User
import com.example.chalkitup.ui.components.ProfilePictureIcon
import com.example.chalkitup.ui.viewmodel.chat.MessageListViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun MessageListScreen(
    navController: NavController,
    messageListViewModel: MessageListViewModel
) {
    val scrollState = rememberLazyListState()

    // Collect states from View Model
    val currentUserId = messageListViewModel.currentUserId
    val users by messageListViewModel.users.collectAsState()
    val currentUserType by messageListViewModel.currentUserType.collectAsState()
    val isConversationsLoading by messageListViewModel.isConversationsLoading.collectAsState()
    val searchQuery by messageListViewModel.searchQuery.collectAsState()
    val error by messageListViewModel.error.collectAsState()

    val filteredConversations = messageListViewModel.getFilteredConversations()

    // Gradient brush for the screen's background.
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF54A4FF), // 5% Blue
            MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface,
        )
    )

    // Trigger to reload user's conversations when screen is launched
    LaunchedEffect(Unit) {
        messageListViewModel.fetchConversations()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar and + Button in the same row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { messageListViewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.weight(1f),

                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { navController.navigate("newMessage") },
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New Message")
                }
            }

            // Display error message
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (isConversationsLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    CircularProgressIndicator()
                }

            } else if (filteredConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text("No conversations")
                }
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredConversations) { conversation ->
                        // Determine the other user's id
                        val otherUserId = if (conversation.studentId == currentUserId)
                            conversation.tutorId else conversation.studentId

                        // Find the corresponding User object from the users list
                        val otherUser = users.find { it.id == otherUserId }

                        ConversationItem(
                            conversation = conversation,
                            user = otherUser,
                            currentUserType = currentUserType,
                            onClick = {

                                // Mark the conversation as read
                                messageListViewModel.markAsRead(conversation.id)

                                // Navigate to chat screen
                                navController.navigate(
                                    "chat/${conversation.id}/${
                                        if (conversation.studentId == currentUserId)
                                            conversation.tutorId
                                        else conversation.studentId
                                    }"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ConversationItem(
    conversation: Conversation,
    user: User?,
    currentUserType: String?,
    onClick: () -> Unit
) {

    // Flag for unread conversation
    val isUnread = when (currentUserType) {

        // If current user is a Student and the last message hasn't been read, then isUnread will be true
        "Student" -> !conversation.lastMessageReadByStudent
        "Tutor" -> !conversation.lastMessageReadByTutor
        else -> false
    }


    // Determine display name if user is null (from account deletion)
    val displayName = if (user != null) {
        "${user.firstName} ${user.lastName}"
    } else {
        "Deleted User" // Placeholder text
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(Color.Transparent),
        shape = RectangleShape,
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
//            .padding(vertical = 0.dp)
//            .height(80.dp)
            .fillMaxHeight()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
//                .fillMaxSize(),
                .padding(start = 12.dp, top = 7.dp, end = 14.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfilePictureIcon(
                profilePictureUrl = user?.userProfilePictureUrl,
                size = 60.dp
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {

                // Name and timestamp
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Display the other user's name
//                    if (user != null) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )


                    // Display the timestamp
                    Text(
                        text = formatTimestamp(conversation.timestamp),
                        style = MaterialTheme.typography.bodySmall.copy(

                        )
                    )
                }

                // Message and notification badge
                Row (
//                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Display the last message
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                            color = if (isUnread) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Display the notification icon if message is unread
                    if (isUnread) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .padding(end = 7.dp)
                                .size(10.dp)
                                .background(
                                    color = Color(0xFF1A73E8),
                                    shape = CircleShape
                                )
                        )
                    }


                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val currentTime = System.currentTimeMillis()
    val difference = currentTime - timestamp
    return when {
        difference < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        difference < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(difference)}min"
        difference < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(difference)}h"
        difference < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
        difference < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(difference)} days ago"
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(timestamp)) // "2025-01-30"
    }
}