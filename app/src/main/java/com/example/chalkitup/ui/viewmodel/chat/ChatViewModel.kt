package com.example.chalkitup.ui.viewmodel.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chalkitup.domain.model.User
import com.example.chalkitup.domain.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel for managing direct messaging within conversations.
 *
 * This ViewModel handles updating and retrieval of messages in Firestore.
 * It allows users to send messages to a selected user (with an opposite user
 * type as the current user).
 * Message data of current conversation is automatically fetched from Firestore upon ViewModel initialization.
 *
 */
//@HiltViewModel
class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""

    // Collecting states from ViewModel
    private val _conversationId = MutableStateFlow<String?>(null)
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()


    /**
     * Function to fetch information of a user
     * @param userId: ID of a user
     * @return User data class
     */
    suspend fun fetchUser(userId: String): User {
        return try {
            val userDoc = db.collection("users").document(userId)
                .get()
                .await()

            // Get profile picture URL from Storage
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference.child("$userId/profilePicture.jpg")
            val profileUrl = try {
                storageRef.metadata.await() // Check if file exists
                storageRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                "" // If no picture
            }

            val user = User(
                id = userDoc.id,
                firstName = userDoc.getString("firstName") ?: "",
                lastName = userDoc.getString("lastName") ?: "",
                userType = userDoc.getString("userType") ?: "",
                userProfilePictureUrl = profileUrl
            )
            Log.d("ChatViewModel", "Fetched user: $user")
            user
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error fetching user: ${e.message}")
            User()
        }
    }

    /**
     * Function to retrieve conversation ID
     * @param selectedUserId: ID of the selected user.
     * @return Conversation ID or null
     */
    suspend fun getConversation(selectedUserId: String): String? {
        return try {

            // Query for conversation where currentUser is the student and selectedUser is the tutor
            val query1 = db.collection("conversations")
                .whereEqualTo("studentId", currentUserId)
                .whereEqualTo("tutorId", selectedUserId)
                .get()
                .await()

            // Query for conversation where currentUser is the tutor and selectedUser is the student
            val query2 = db.collection("conversations")
                .whereEqualTo("studentId", selectedUserId)
                .whereEqualTo("tutorId", currentUserId)
                .get()
                .await()

            val allResults = query1.documents + query2.documents

            // Return the ID of the first matching conversation
            allResults.firstOrNull()?.id


        } catch (e: Exception) {
            Log.e("Firestore", "Error getting conversation: ${e.message}")
            null
        }
    }

    /**
     * Function to set the current conversation ID
     * @param newConversationId: ID of the conversation to be set.
     */
    fun setConversationId(newConversationId: String?) {
        if (_conversationId.value == newConversationId) return  // Prevent unnecessary fetching

        _conversationId.value = newConversationId

        newConversationId?.let { id ->
            viewModelScope.launch {
                _messages.value = emptyList()   // Clear previous messages for a new chat
                getMessages(id).collectLatest { newMessages ->
                    _messages.value = newMessages
                }
            }
        }

    }

    /**
     * Create a new conversation in 'conversations' collection in Firestore
     * @param currentUser: Data of the current user.
     * @param selectedUser: Data of the selected user.
     * @return The new conversation ID
     */
    suspend fun createConversation(
        currentUser: User,
        selectedUser: User
    ): String? {
        return try {
            // Determine student and tutor
            val studentId: String
            val studentName: String
            val tutorId: String
            val tutorName: String

            when (currentUser.userType) {
                "Student" -> {
                    studentId = currentUserId
                    studentName = "${currentUser.firstName} ${currentUser.lastName}"
                    tutorId = selectedUser.id
                    tutorName = "${selectedUser.firstName} ${selectedUser.lastName}"
                }
                "Tutor" -> {
                    studentId = selectedUser.id
                    studentName = "${selectedUser.firstName} ${selectedUser.lastName}"
                    tutorId = currentUserId
                    tutorName = "${currentUser.firstName} ${currentUser.lastName}"
                }
                else -> {
                    throw IllegalStateException("Invalid user type: ${currentUser.userType}")
                }
            }

            // Create new conversation document
            val conversationRef = db.collection("conversations")
                .add(
                    hashMapOf(
                    "studentId" to studentId,
                    "tutorId" to tutorId,
                    "studentName" to studentName,
                    "tutorName" to tutorName,
                    "lastMessage" to "",
                    "timestamp" to System.currentTimeMillis()
                    )
                ).await()
            conversationRef.id

        } catch (e: Exception) {
            println("Error creating conversation: ${e.message}")
            null
        }

    }


    /**
     * Function to add a message to an existing conversation in Firestore
     * @param conversationId: ID of the conversation.
     * @param text: message text.
     */
    suspend fun sendMessage(
        conversationId: String,
        text: String
    ) {
        val message = Message(
            senderId = currentUserId,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        try {
            // Add message to "messages" subcollection in the conversation
            db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .add(message)
                .await()

            // Update the "last message" and "timestamp" fields in the "conversation" document
            db.collection("conversations")
                .document(conversationId)
                .update(
                    "lastMessage", text,
                    "timestamp", message.timestamp
                )
                .await()
            Log.d("Firestore", "Message successfully sent.")

        } catch (e: Exception) {
            Log.e("Firestore", "Error adding message: ${e.message}")
        }
    }

    /**
     * Function to fetch messages for selected conversation.
     *
     * @param conversationId: current conversation ID
     */
    fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val messagesRef = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp")

        // Set up a Firestore listener for the messages subcollection
        val messagesListener = messagesRef
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // Map Firestore documents to Message objects
                val messageList = snapshot?.documents?.mapNotNull {
                    it.toObject(Message::class.java)
                } ?: emptyList()
                trySend(messageList)
            }

        awaitClose { messagesListener.remove() }
    }

    // Clear messages and remove the listener
    fun clearMessages() {
        _messages.value = emptyList()
    }



}
