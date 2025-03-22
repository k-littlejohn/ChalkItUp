package com.example.chalkitup.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotificationViewModel: ViewModel() {

    private val _userType = MutableStateFlow<String?>("Unknown")
    val userType: StateFlow<String?> get() = _userType

    private val _notifications = MutableStateFlow<List<NotifClass>>(emptyList())
    val notifications: StateFlow<List<NotifClass>> get() = _notifications

    init {
        getUserName()
        grabNotifications()
    }

    private fun getUserName() {
        viewModelScope.launch {
            val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
            currentUserID?.let {
                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("users").document(it)
                val snapshot = userRef.get().await()
                _userType.value = snapshot.getString("userType") ?: "Unknown"
            }
        }
    }

    // UNFINISHED!!
    private fun grabNotifications() {
        viewModelScope.launch {
            val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

            if (currentUserID != null) {
                val db = FirebaseFirestore.getInstance()
                val notifsRef = db.collection("notifications")
                val usersRef = db.collection("users")

                try {
                    val userNotifications = notifsRef
                        .whereEqualTo("notifUserID", currentUserID)
                        .get()
                        .await()
                        .documents
                    
                    println("User ID: $userNotifications")

                    _notifications.value = userNotifications
                        .mapNotNull {
                            //it.getString("date")?.replace("\"", "") }
                            doc ->
                            val notification = doc.toObject(NotifClass::class.java)?.copy(notifID = doc.id)

                            notification?.let {
                                val userSnapshot = usersRef.document(it.notifUserID).get().await()
                                val userFirstName = userSnapshot.getString("firstName") ?: "Unknown"
                                val userLastName = userSnapshot.getString("lastName") ?: ""

                                it.copy(
                                    notifUserName = "$userFirstName $userLastName",
                                )
                            }
                        }
                        .sortedBy { notification ->
                            LocalDate.parse(notification.notifDate,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
                        }

                    println("Notifs: $notifications")

                } catch (e: Exception) {
                    println("Error fetching notifications: ${e.message}")
                }
            }
        }
    }

    // Firebase order: notifications/actual notification info
    fun addNotification(
        notifType: String, // Update, Session, Message
        notifUserName: String, // Name of the person in the notification
        notifTime: String,
        notifDate: String,
        comments: String,
        sessType: String, // Booked, Rescheduled, Cancelled
        sessDate: String,
        sessTime: String,
        otherID: String, // ID of the other person in the notification
        otherName: String, // ID of the other person in the notification
        subject: String,
        grade: String,
        spec: String,
        mode: String
    ) {
        viewModelScope.launch {
            val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserID != null) {
                val db = FirebaseFirestore.getInstance()

                val notifData = hashMapOf(
                    "notifType" to notifType,
                    "notifUserID" to currentUserID,
                    "notifUserName" to notifUserName,
                    "notifTime" to notifTime,
                    "notifDate" to notifDate,
                    "comments" to comments,
                    "sessType" to sessType,
                    "sessDate" to sessDate,
                    "sessTime" to sessTime,
                    "otherID" to otherID,
                    "otherName" to otherName,
                    "subject" to subject,
                    "grade" to grade,
                    "spec" to spec,
                    "mode" to mode
                )

                db.collection("notifications")
                    .add(notifData)
                    .await()
            }
        }
    }
}


/**
 * Data needed for the notification
 *
 * notifType: What the notification is about in one word (Update, Session, Message)
 *
 * Everything else contains the specific information about the notification
 **/
data class NotifClass (
    var notifID: String = "",
    var notifType: String = "", // Update, Session, Message
    val notifUserID: String = "", // ID of the person in the notification
    val notifUserName: String = "", // Name of the person in the notification
    val notifTime: String = "",
    var notifDate: String = "",
    var comments: String = "",
    var sessType: String = "", // Booked, Rescheduled, Cancelled
    val sessDate: String = "",
    val sessTime: String = "",
    val otherID: String = "", // ID of the other person in the notification
    val otherName: String = "", // ID of the other person in the notification
    val subject: String = "",
    val grade: String = "",
    val spec: String = "",
    val mode: String = ""
)