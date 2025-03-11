package com.example.chalkitup.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chalkitup.model.User
import com.example.chalkitup.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel for managing direct messaging * * This ViewModel handles sending new messages, and retrieval of * new messages. */
class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // StateFlow for messages
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    val currentUserId = auth.currentUser?.uid ?: ""
    var messagesListener: ListenerRegistration? = null


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
            User(
                id = userDoc.id,
                firstName = userDoc.getString("firstName") ?: "",
                lastName = userDoc.getString("lastName") ?: "",
                userType = userDoc.getString("userType") ?: "",
                userProfilePictureUrl = userDoc.getString("userProfilePictureUrl") ?: ""
            )
        } catch (e: Exception) {
            println("Error fetching user info: ${e.message}")
            User()
        }
    }

    /**
     * Function to retrieve conversation ID     *     * @param selectedUserId: ID of the selected user.
     *     * @return Conversation ID or null
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

//            val correctConversation = allResults.find { document ->
//                val studentId = document.getString("studentId") ?: ""
//                val tutorId = document.getString("tutorId") ?: ""
//                (studentId == currentUserId && tutorId == selectedUserId) ||
//                        (studentId == selectedUserId && tutorId == currentUserId)
//            }
//
//            correctConversation?.id

        } catch (e: Exception) {
            Log.e("Firestore", "Error getting conversation: ${e.message}")
            null
        }
    }


    /**
     * Create a new conversation in 'conversations' collection in Firestore     *     * @param currentUser: Data of the current user.
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
//            val conversationRef = db.collection("conversations").document()
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
//            conversationRef.set(conversationData).await()
            conversationRef.id

        } catch (e: Exception) {
            println("Error creating conversation: ${e.message}")
            null
        }

    }

    /**
     * Function to add a message to an existing conversation in Firestore     *     * @param selectedUserId: ID of the selected user.
     * @param text: message text.
     */
    fun sendMessage(
        conversationId: String?,
        selectedUserId: String,
        text: String
    ) {
        viewModelScope.launch {
            try {
                var finalConversationId = conversationId

                // If conversationId is null or empty, create a new conversation
                if (finalConversationId.isNullOrEmpty()) {
                    Log.d("Firestore", "Creatinga new conversation")

                    // Fetch user details
                    val currentUser = fetchUser(currentUserId)
                    val selectedUser = fetchUser(selectedUserId)

                    // Create a new conversation
                    finalConversationId = createConversation(currentUser, selectedUser)

                    // Check if the conversation was successfully created
                    if (finalConversationId.isNullOrEmpty()) {
                        Log.e("Firestore", "Failed to create conversation. Aborting message send.")
                        return@launch
                    }
                }

                // add message to conversation ID
                if (finalConversationId != null) {
                    addMessageToConversation(finalConversationId, text)
                }

            } catch (e: Exception) {
                Log.e("Firestore", "Error in sendMessage(): ${e.message}")
            }

        }
    }

    /**
     * Helper function add a message to an existing conversation.     *     * @param conversationId: ID of the conversation.
     * @param text: message text.
     */
    private suspend fun addMessageToConversation(
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
     * Function to fetch messages for selected conversation.     *     * @param conversationId: current conversation ID
     */
    fun fetchMessages(conversationId: String) {
        messagesListener?.remove()

        // Set up a Firestore listener for the messages subcollection
        messagesListener = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error fetching messages: ${error.message}")
                    return@addSnapshotListener
                }

                // Map Firestore documents to Message objects
                val messageList = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Message::class.java)
                } ?: emptyList()

                // Update messages state
                _messages.value = messageList
            }
    }

    // Clear messages and remove the listener
    fun clearMessages() {
        _messages.value = emptyList()
        messagesListener?.remove()
    }

    // Clear Firestore listener when the ViewModel is no longer used
    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
    }


}
