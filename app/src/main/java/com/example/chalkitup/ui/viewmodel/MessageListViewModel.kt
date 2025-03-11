package com.example.chalkitup.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chalkitup.model.Conversation
import com.example.chalkitup.model.Message
import com.example.chalkitup.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MessageListViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Holds info of current user
    val currentUser = auth.currentUser
    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    // Holds the current user's type (e.g., "Student" or "Tutor")
    private val _currentUserType = MutableStateFlow<String?>(null)
//    val currentUserType: StateFlow<String?> = _currentUserType.asStateFlow()

    // Holds a list of users with opposite user type as current user
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    // Holds a list of profile pictures from other users
    private val _profilePictures = MutableStateFlow<Map<String, String>>(emptyMap())
    val profilePictures: StateFlow<Map<String, String>> = _profilePictures.asStateFlow()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // State for loading
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // StateFlow for error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // State for the search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()



    init {
        fetchCurrentUserType {
            fetchUsers()
            fetchConversations()
        }
    }

        /** For MessagesScreen */
        // Function to insert message to Firebase
        // Function to load messages from Firebase
        // Function to load profile of other sender from Firebase
        // Function to block user from Firebase
        // Function to report user

        fun fetchCurrentUserType(onComplete: () -> Unit) {
            viewModelScope.launch {
                if (currentUser != null) {
                    try {
                        val userDoc = db.collection("users").document(currentUser.uid).get().await()
                        _currentUserType.value = userDoc.getString("userType")
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error fetching current user type: ${e.message}")
                        _error.value = "Failed to load current user type"
                    } finally {
                        onComplete() // Execute the callback
                    }
                } else {
                    _error.value = "No current user found"
                    onComplete() // Execute the callback even if there's no user
                }
            }
        }

    // Fetch users with the opposite user type
    fun fetchUsers() {
        viewModelScope.launch {

            val currentUserType = _currentUserType.value

            if (currentUser != null) {
                try {
                    _isLoading.value = true
                    _error.value = null

                    // Determine the opposite user type
                    val oppositeUserType =
                        if (currentUserType == "Student") "Tutor" else "Student"

                    // Fetch users with the opposite type
                    val query = db.collection("users")
                        .whereEqualTo("userType", oppositeUserType)
                        .get()
                        .await()

                    // Map Firestore documents to User objects
                    _users.value = query.map { document ->
                        User(
                            id = document.id,
                            firstName = document.getString("firstName") ?: "",
                            lastName = document.getString("lastName") ?: "",
                            userType = document.getString("userType") ?: ""
                        )
                    }
                } catch (e: Exception) {
                    Log.e("Firestore", "Error fetching users: ${e.message}")
                    _error.value = "Failed to load users"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    // Fetch ongoing conversations with the current user
    fun fetchConversations() {
        viewModelScope.launch {
            val currentUserType = _currentUserType.value

            try {
                _isLoading.value = true
                _error.value = null

                // Fetch conversations where the current user is either a Student or Tutor
                val conversations = when (currentUserType) {
                    "Student" -> {
                        db.collection("conversations")
                            .whereEqualTo("studentId", currentUserId)
                            .get()
                            .await()
                    }
                    "Tutor" -> {
                        db.collection("conversations")
                            .whereEqualTo("tutorId", currentUserId)
                            .get()
                            .await()
                    } else -> {
                        throw IllegalStateException("Invalid user type: $currentUserType")
                    }
                }

                // Map Firestore documents to Conversation objects
                val conversationList = conversations.mapNotNull { document ->
                    val conversation = document.toObject(Conversation::class.java)
                    conversation?.copy(id = document.id)
                }

                // Fetch the most recent message for each conversation
                val updatedConversations = conversationList.map { conversation ->
                    val lastMessage = fetchLastMessage(conversation.id)
                    conversation.copy(lastMessage = lastMessage?.text ?: "", timestamp = lastMessage?.timestamp ?: 0)
                }
                _conversations.value = updatedConversations

            } catch (e: Exception) {
                Log.e("Firestore", "Error fetching conversations: ${e.message}")
                _error.value = "Failed to load conversations"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Fetch the most recent message from a conversation
    private suspend fun fetchLastMessage(conversationId: String): Message? {
        return try {
            val query = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            query.documents.firstOrNull()?.toObject(Message::class.java)
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching last message: ${e.message}")
            null
        }
    }

        /** For NewMessagesScreen */




    // Update the search query
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Get filtered users based on the search query
    fun getFilteredUsers(): List<User> {
        return users.value.filter { user ->
            user.firstName.contains(searchQuery.value, ignoreCase = true) ||
                    user.lastName.contains(searchQuery.value, ignoreCase = true)
        }
            .sortedWith(compareBy({ it.firstName.lowercase() }, { it.lastName.lowercase() }))
    }

    fun loadProfilePictures(userIds: List<String>) {
        userIds.forEach { userId ->
            val storageRef =
                FirebaseStorage.getInstance().reference.child("$userId/profilePicture.jpg")

            storageRef.downloadUrl.addOnSuccessListener { uri ->
                _profilePictures.value = _profilePictures.value.toMutableMap().apply {
                    put(userId, uri.toString())
                }
            }.addOnFailureListener {
                Log.e("Firebase", "Failed to fetch profile picture for $userId", it)
            }
        }
    }



}


