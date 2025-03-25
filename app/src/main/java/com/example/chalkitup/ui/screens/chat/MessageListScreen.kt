package com.example.chalkitup.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chalkitup.domain.model.Conversation
import com.example.chalkitup.domain.model.User
import com.example.chalkitup.ui.components.ProfilePictureIcon
import com.example.chalkitup.ui.viewmodel.chat.MessageListViewModel
import java.util.concurrent.TimeUnit

@Composable
fun MessageListScreen(
    navController: NavController,
    messageListViewModel: MessageListViewModel
) {
    val scrollState = rememberLazyListState()

    // Collect states from View Model
    val currentUserId = messageListViewModel.currentUserId
    val users by messageListViewModel.users.collectAsState() // List of users with opposite user type as current user
    val isConversationsLoading by messageListViewModel.isConversationsLoading.collectAsState()
    val searchQuery by messageListViewModel.searchQuery.collectAsState()
    val error by messageListViewModel.error.collectAsState()

    val filteredConversations = messageListViewModel.getFilteredConversations()


    LaunchedEffect(Unit) {
        messageListViewModel.fetchConversations()
    }

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
                modifier = Modifier.weight(1f)
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
            Box(modifier = Modifier
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
                        onClick = {
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


@Composable
fun ConversationItem(
    conversation: Conversation,
    user: User?,
    onClick: () -> Unit
) {
    val userProfilePictureUrl = user?.userProfilePictureUrl

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfilePictureIcon(profilePictureUrl = userProfilePictureUrl)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Display the other user's name
                if (user != null) {
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                // Display the last message & timestamp
                Row (
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = formatTimestamp(conversation.timestamp),
                        style = MaterialTheme.typography.bodySmall
                    )

                }
            }
        }
    }
}

// Helper function to format the timestamp
private fun formatTimestamp(timestamp: Long): String {
    val currentTime = System.currentTimeMillis()
    val difference = currentTime - timestamp
    return when {
        difference < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        difference < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(difference)} min ago"
        difference < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(difference)} h ago"
        difference < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
        difference < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(difference)} days ago"
        else -> java.text.SimpleDateFormat("MMM dd, yyyy").format(java.util.Date(timestamp)) // "Feb 15, 2024"
    }
}