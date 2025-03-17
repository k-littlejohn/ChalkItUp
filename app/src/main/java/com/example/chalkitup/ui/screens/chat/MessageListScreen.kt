package com.example.chalkitup.ui.screens.chat

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chalkitup.domain.model.Conversation
import com.example.chalkitup.ui.viewmodel.MessageListViewModel
import java.util.concurrent.TimeUnit

@Composable
fun MessageListScreen(
    navController: NavController,
    messageListViewModel: MessageListViewModel
) {
    // Scroll state for vertical scrolling
    val scrollState = rememberLazyListState()

    val currentUserId = messageListViewModel.currentUserId
    val searchQuery by messageListViewModel.searchQuery.collectAsState()
    val filteredConversations = messageListViewModel.getFilteredConversations()

    val isLoading by messageListViewModel.isLoading.collectAsState()
    val error by messageListViewModel.error.collectAsState()

    // Fetch conversations when screen is launched
    LaunchedEffect(currentUserId) {
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

            Spacer(modifier = Modifier.width(8.dp))

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

        // Chat List
        if (isLoading) {
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
                    ConversationItem(
                        conversation = conversation,
                        currentUserId = currentUserId,
                        onClick = {
                            navController.navigate("chat/${conversation.id}/${
                                if (conversation.studentId == currentUserId) 
                                    conversation.tutorId 
                                else conversation.studentId
                            }" )
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
    currentUserId: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)) {
            // Display the other user's name
            Text(
                text = if (conversation.studentId == currentUserId) {
                    conversation.tutorName
                } else {
                    conversation.studentName
                },
                style = MaterialTheme.typography.titleMedium
            )

            // Display the last message
            Text(
                text = conversation.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )

            // Display the timestamp of the last message
            Text(
                text = formatTimestamp(conversation.timestamp),
                style = MaterialTheme.typography.bodySmall,
            )
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