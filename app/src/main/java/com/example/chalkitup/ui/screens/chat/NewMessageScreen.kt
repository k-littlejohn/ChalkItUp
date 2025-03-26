package com.example.chalkitup.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.chalkitup.domain.model.User
import com.example.chalkitup.ui.components.ProfilePictureIcon
import com.example.chalkitup.ui.viewmodel.chat.ChatViewModel
import com.example.chalkitup.ui.viewmodel.chat.MessageListViewModel
import kotlinx.coroutines.launch


@Composable
fun NewMessageScreen(
    navController: NavController,
    messageListViewModel: MessageListViewModel,
    chatViewModel: ChatViewModel
) {
    // Collecting states from ViewModel
    val isUsersLoading by messageListViewModel.isUsersLoading.collectAsState()
    val searchQuery by messageListViewModel.searchQuery.collectAsState()
    val error by messageListViewModel.error.collectAsState()

    val filteredUsers = messageListViewModel.getFilteredUsers()

    // Gradient brush for the screen's background.
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF54A4FF), // 5% Blue
            MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface,
        )
    )


    // Trigger to reload user's conversations when screen is launched
    LaunchedEffect(Unit) {
        messageListViewModel.fetchUsers()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar and Back Button in the same row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { messageListViewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.weight(1f)
                )
            }
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (isUsersLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text("No users found")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredUsers) { user ->
                        UserItem(
                            user = user,
                            onClick = {
                                // Fetch existing conversation before navigating
                                chatViewModel.viewModelScope.launch {
                                    val existingConversationId =
                                        chatViewModel.getConversation(user.id)

                                    // If no conversation exists, pass "null" as the conversationId
                                    val conversationId = existingConversationId ?: "null"

                                    navController.navigate("chat/$conversationId/${user.id}")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}


// UI Component for displaying each user
@Composable
fun UserItem(
    user: User,
    onClick: () -> Unit
) {
    val userProfilePictureUrl = user.userProfilePictureUrl

    Card(
        onClick = onClick,
        shape = RectangleShape,
        colors = CardDefaults.cardColors(Color(0xFFF3F0FA)),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
//            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
//                .padding(16.dp),
                .padding(start = 12.dp, top = 7.dp, end = 14.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Display profile picture
            ProfilePictureIcon(
                profilePictureUrl = userProfilePictureUrl,
                size = 60.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${user.firstName} ${user.lastName}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}