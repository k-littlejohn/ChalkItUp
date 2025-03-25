package com.example.chalkitup.ui.viewmodel.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chalkitup.domain.model.Conversation
import com.example.chalkitup.domain.model.Message
import com.example.chalkitup.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel for managing conversations.
 *
 * This ViewModel handles the selection, storage, and retrieval of ongoing conversations.
 * It allows users to select users who they want create a conversation with or view an ongoing conversation.
 * Conversation data is automatically fetched from Firestore upon ViewModel initialization.
 *
 */
class MessageListViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Holds info of current user
    val currentUser = auth.currentUser
    val currentUserId: String get() = currentUser?.uid ?: ""

    // Holds the current user's type (e.g., "Student" or "Tutor")
    private val _currentUserType = MutableStateFlow<String?>(null)

    // Holds a list of users with opposite user type as current user
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _profilePictures = MutableStateFlow<Map<String, String>>(emptyMap())

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // For MessageListScreen
    private val _isConversationsLoading = MutableStateFlow(true)
    val isConversationsLoading: StateFlow<Boolean> = _isConversationsLoading.asStateFlow()

    // For NewMessageScreen
    private val _isUsersLoading = MutableStateFlow(true)
    val isUsersLoading: StateFlow<Boolean> = _isUsersLoading.asStateFlow()

//    // Flags to track whether data has been loaded
//    private var isConversationsLoaded = false
//    private var isUsersLoaded = false

    // Error state (shared between screens)
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // State for the search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery


    init {
        fetchCurrentUserType {
            fetchConversations()
            fetchUsers()
        }
    }

    private fun fetchCurrentUserType(onComplete: () -> Unit) {
        viewModelScope.launch {
            if (currentUser != null) {
                try {
                    val userDoc = db.collection("users").document(currentUser.uid)
                        .get()
                        .await()
                    _currentUserType.value = userDoc.getString("userType")
                } catch (e: Exception) {
                    Log.e("MessageListViewModel", "Error fetching user type: ${e.message}")
                    _error.value = "Failed to load user type"
                } finally {
                    onComplete()
                }
            } else {
                _error.value = "No current user found"
                onComplete()
            }
        }
    }

    // Fetch users with the opposite user type
    fun fetchUsers() {
        viewModelScope.launch {
            try {
                _isUsersLoading.value = true
                _error.value = null

                val currentUserDoc = db.collection("users").document(currentUserId).get().await()

                val currentType = currentUserDoc.getString("userType") ?: ""
                val oppositeType = if (currentType == "Student") "Tutor" else "Student"

                val querySnapshot = db.collection("users")
                    .whereEqualTo("userType", oppositeType)
                    .get()
                    .await()

                val usersList = querySnapshot.map { document ->
                    User(
                        id = document.id,
                        firstName = document.getString("firstName") ?: "",
                        lastName = document.getString("lastName") ?: "",
                        userType = document.getString("userType") ?: "",
                        userProfilePictureUrl = "" // initially empty; will be updated
                    )
                }

                _users.value = usersList

                // Load profile pictures for these users
                val userIds = usersList.map { it.id }
                loadProfilePictures(userIds)

            } catch (e: Exception) {
                Log.e("MessageListViewModel", "Error fetching users: ${e.message}")
                _error.value = "Failed to load users"
            } finally {
                _isUsersLoading.value = false
            }
        }
    }


    // Listen to conversations using a snapshot listener wrapped in a Flow
    fun fetchConversations() {
        viewModelScope.launch {

            try {
                _isConversationsLoading.value = true
                _error.value = null

                val userType = _currentUserType.value

                val snapshot = when (userType) {
                    "Student" -> db.collection("conversations")
                        .whereEqualTo("studentId", currentUserId)
                        .get().await()

                    "Tutor" -> db.collection("conversations")
                        .whereEqualTo("tutorId", currentUserId)
                        .get().await()

                    else -> throw IllegalStateException("Invalid user type: $userType")
                }

                val convos = snapshot.documents.mapNotNull { document ->
                    Conversation(
                        id = document.id,
                        studentId = document.getString("studentId") ?: "",
                        tutorId = document.getString("tutorId") ?: "",
                        studentName = document.getString("studentName") ?: "",
                        tutorName = document.getString("tutorName") ?: "",
                        lastMessage = document.getString("lastMessage") ?: "",
                        timestamp = document.getLong("timestamp") ?: 0L
                    )
                }

                // Update state
                _conversations.value = convos

                // Load users' profile pictures
                val userIds = convos.flatMap { listOf(it.studentId, it.tutorId) }.distinct()
                loadProfilePictures(userIds)

            } catch (e: Exception) {
                Log.e("MessageListViewModel", "Error fetching conversations: ${e.message}")
                _error.value = "Failed to load conversations"

            } finally {
                _isConversationsLoading.value = false
            }
        }
    }


    // Update the search query
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Filter users based on the search query
    fun getFilteredUsers(): List<User> {
        return users.value.filter { user ->
            user.firstName.contains(searchQuery.value, ignoreCase = true) ||
                    user.lastName.contains(searchQuery.value, ignoreCase = true)
        }.sortedWith(compareBy({ it.firstName.lowercase() }, { it.lastName.lowercase() }))
    }

    // Filter existing conversations based on the search query
    fun getFilteredConversations(): List<Conversation> {
        val query = searchQuery.value.trim().lowercase()

        return conversations.value.filter { conversation ->
            val otherUserName = if (conversation.studentId == currentUserId) {
                conversation.tutorName
            } else {
                conversation.studentName
            }
            otherUserName.lowercase().contains(query)
        }.sortedByDescending { it.timestamp }
    }

    // Function to retrieve the URL for each user’s profile picture from Firebase Storage
    private suspend fun loadProfilePictures(userIds: List<String>) {
        val storage = FirebaseStorage.getInstance()
        val results = mutableMapOf<String, String>()

        userIds.forEach { userId ->
            val storageRef = storage.reference.child("$userId/profilePicture.jpg")
            try {
                // Check if the file exists by retrieving its metadata
                storageRef.metadata.await()

                // If metadata exists, then download the URL
                val url = storageRef.downloadUrl.await().toString()
                results[userId] = url

            } catch (e: Exception) {
                // File does not exist—skip fetching and use an empty string (or default URL).
                results[userId] = ""
            }
        }
        // Update the profile pictures map
        _profilePictures.value = results

        // Update each user's profile picture URL
        _users.value = _users.value.map { user ->
            user.copy(userProfilePictureUrl = results[user.id] ?: "")
        }
    }

}

