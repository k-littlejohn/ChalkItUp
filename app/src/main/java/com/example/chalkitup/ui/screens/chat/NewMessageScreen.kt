package com.example.chalkitup.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chalkitup.domain.model.User
import com.example.chalkitup.ui.viewmodel.ChatViewModel
import com.example.chalkitup.ui.viewmodel.MessageListViewModel
import kotlinx.coroutines.launch


@Composable
fun NewMessageScreen(
    navController: NavController,
    messageListViewModel: MessageListViewModel,
    chatViewModel: ChatViewModel
    ) {
    // Collecting states from ViewModel
    val userList by messageListViewModel.users.collectAsState() // List of users with opposite user type as current user
    val profilePictures by messageListViewModel.profilePictures.collectAsState()
    val isLoading by messageListViewModel.isLoading.collectAsState()
    val searchQuery by messageListViewModel.searchQuery.collectAsState()


    // Load users' profile pictures
    LaunchedEffect(userList) {
        messageListViewModel.fetchUsers()
        val userIds = userList.map { it.id }
        messageListViewModel.loadProfilePictures(userIds)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            //horizontalArrangement = Arrangement.SpaceBetween
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { messageListViewModel.updateSearchQuery(it) }, // Update the state when user types
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        // Display filtered users
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            userList.isEmpty() -> Text("No users found.", modifier = Modifier.align(Alignment.CenterHorizontally))
            else -> {
                val filteredUsers = messageListViewModel.getFilteredUsers()
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredUsers) { user ->
                        UserItem(
                            user = user,
                            profilePictureUrl = profilePictures[user.id],
                            onClick = {
                                // Fetch existing conversation before navigating
                                chatViewModel.viewModelScope.launch {
                                    val existingConversationId = chatViewModel.getConversation(user.id)

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
    profilePictureUrl: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserProfilePicture(
                profilePictureUrl = profilePictureUrl,
                firstName = user.firstName,
                lastName = user.lastName
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${user.firstName} ${user.lastName}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun UserProfilePicture(profilePictureUrl: String?, firstName: String, lastName: String) {
    if (profilePictureUrl.isNullOrEmpty()) {
        // Show a placeholder with initials
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${firstName[0]}${lastName[0]}".uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    } else {
        // Display user's profile picture
        AsyncImage(
            model = profilePictureUrl,
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(40.dp)
                .border(1.dp, Color.Gray, CircleShape)
                .clip(CircleShape)
        )
    }
}
