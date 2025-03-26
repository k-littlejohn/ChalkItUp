package com.example.chalkitup.ui.viewmodel

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chalkitup.ui.components.TutorSubject
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.storage
import androidx.compose.material3.OutlinedTextField
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Handles logic for ProfileScreen
// - fetches user information from firebase and loads it

class ProfileViewModel : ViewModel() {

    // LiveData to hold and observe the user's profile data
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: MutableLiveData<UserProfile?> get() = _userProfile

    // LiveData to indicate if the user is a tutor
    private val _isTutor = MutableLiveData<Boolean>()
    val isTutor: LiveData<Boolean> get() = _isTutor

    // LiveData to hold and observe the user's profile picture URL
    private val _profilePictureUrl = MutableLiveData<String?>()
    val profilePictureUrl: LiveData<String?> get() = _profilePictureUrl

    private val db = FirebaseFirestore.getInstance()
    private val _totalSessions = MutableStateFlow(0)
    val totalSessions: StateFlow<Int> = _totalSessions

    private val _totalHours = MutableStateFlow(0.0)
    val totalHours: StateFlow<Double> = _totalHours

    private var sessionListener: ListenerRegistration? = null

    // Formatters for parsing date and time strings
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    init {
        // Automatically load user profile when ViewModel is created
        loadUserProfile()
    }

    fun startListeningForPastSessions(userIdInput: String? = "") {
        val userId: String = if (userIdInput.isNullOrEmpty()) {
            FirebaseAuth.getInstance().currentUser?.uid ?: ""
        } else {
            userIdInput
        }
        sessionListener = db.collection("appointments")
            .whereEqualTo("tutorID", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProfileViewModel", "Error fetching past sessions", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    var sessionCount = 0
                    var totalHours = 0.0
                    val currentDate = LocalDate.now()

                    for (doc in snapshot.documents) {
                        val timeRange = doc.getString("time") // Example: "11:30 AM - 12:30 PM"
                        val dateString = doc.getString("date") // Example: "2025-03-25"

                        if (timeRange != null && dateString != null) {
                            val sessionDate = LocalDate.parse(dateString, dateFormatter)

                            if (sessionDate.isBefore(currentDate)) { // Only count past sessions
                                val parts = timeRange.split(" - ")
                                if (parts.size == 2) {
                                    val startTime = LocalTime.parse(parts[0], timeFormatter)
                                    val endTime = LocalTime.parse(parts[1], timeFormatter)

                                    // Calculate session duration in hours
                                    val duration = ChronoUnit.MINUTES.between(startTime, endTime) / 60.0
                                    totalHours += duration
                                    sessionCount++
                                    println("Session duration: $duration hours")
                                    println("Total hours: $totalHours")
                                    println("Session count: $sessionCount")
                                }
                            }
                        }
                    }

                    // Update state
                    _totalSessions.value = sessionCount
                    _totalHours.value = totalHours

                    // Update Firestore user document
                    updateTutorStats(userId, sessionCount, totalHours)
                }
            }
    }

    private fun updateTutorStats(userId: String, sessionCount: Int, totalHours: Double) {
        db.collection("users").document(userId)
            .update(mapOf(
                "totalSessions" to sessionCount,
                "totalHoursTutored" to totalHours
            ))
            .addOnFailureListener { e ->
                Log.e("ProfileViewModel", "Error updating tutor stats", e)
            }
    }

    override fun onCleared() {
        super.onCleared()
        sessionListener?.remove() // Clean up Firestore listener
    }




    // Function to load the user profile when the ViewModel is initialized
    fun loadUserProfile(targetedUser: String? = "") {
        if (targetedUser.isNullOrEmpty()) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val userId = user.uid
                fetchUserProfile(userId)
            }
        } else { // Viewing another user
            fetchUserProfile(targetedUser)
        }
    }

    // Function to fetch the user profile from Firestore
    private fun fetchUserProfile(userId: String) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                // Convert the Firestore document into a UserProfile object
                val user = document.toObject(UserProfile::class.java)
                if (user != null) {
                    // Update the user profile LiveData
                    _userProfile.value = user
                    // Load the profile picture URL from Firestore
                    loadProfilePicture(userId)

                    // Check if the user is a tutor or a student
                    _isTutor.value = user.userType == "Tutor"
                }
                //if (user.userType == "Tutor") {
                    // Placeholder for loading tutor specific information
                    // Currently none saved yet
                    // Certification loading is handled by the CertificationViewModel
                }
        }

    // Function to load the profile picture from storage
    private fun loadProfilePicture(userId: String) {
        val storageRef = Firebase.storage.reference.child("$userId/profilePicture.jpg")
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            _profilePictureUrl.value = uri.toString()
        }.addOnFailureListener {
            _profilePictureUrl.value = null // Set to null if no profile picture exists
        }
    }


    fun reportUser(userId: String, reportMessage: String, onSuccess: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val reportsCollection = db.collection("reports")

        val reportData = hashMapOf(
            "userId" to userId,
            "reportMessage" to reportMessage,
            "timestamp" to FieldValue.serverTimestamp() // Firestore-generated timestamp
        )

        reportsCollection.add(reportData)
            .addOnSuccessListener { documentReference ->
                Log.d("AddReport", "Report added with ID: ${documentReference.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("AddReport", "Error adding report", e)
            }
    }
}




// Data class to represent the user profile data
data class UserProfile(
    val userType: String = "",  // Type of user ("Tutor" or "Student")
    val firstName: String = "", // First name of the user
    val lastName: String = "",  // Last name of the user
    val email: String = "",     // Email address of the user
    val subjects: List<TutorSubject> = emptyList(), // List of subjects the user is associated with (for tutors)
    val bio: String = "",        // User's bio
    var startingPrice: String = "", // Tutors startingPrice
    var experience: String = "", // Tutors years of experience
    val interests: List<Interest> =listOf( Interest("Accounting", false), Interest("Agriculture", false),
        Interest("Ancient History", false), Interest("Animal", false),
        Interest("Art", false), Interest("Art-History", false),
        Interest("Biology", false), Interest("Business", false),
        Interest("Computer Science", false), Interest("Cell-Biology", false),
        Interest("Chemistry", false), Interest("Earth-Science", false),
        Interest("English", false), Interest("Engineering", false),
        Interest("Finance", false), Interest("French", false),
        Interest("Food", false), Interest("Geology", false),
        Interest("Government", false), Interest("Kinesiology", false),
        Interest("Language", false), Interest("Legal", false),
        Interest("Marketing", false), Interest("Math", false),
        Interest("Medical Science", false), Interest("Music", false),
        Interest("Nutrition", false), Interest("Physics", false),
        Interest("Psychology", false), Interest("Social Studies", false),
        Interest("Physical Activity", false), Interest("Zoology", false)
    ),
    val progress: List<ProgressItem> =emptyList()
) {
    fun copyWith(
        userType: String = this.userType,
        firstName: String = this.firstName,
        lastName: String = this.lastName,
        email: String = this.email,
        subjects: List<TutorSubject> = this.subjects,
        interests: List<Interest> = this.interests,
        progress: List<ProgressItem> = this.progress
    ): UserProfile {
        return copy(
            userType = userType,
            firstName = firstName,
            lastName = lastName,
            email = email,
            subjects = subjects,
            interests = interests,
            progress = progress
        )
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userType" to userType,
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "subjects" to subjects,
            "interests" to interests,
            "progress" to progress
        )
    }

    companion object {
        fun fromUser(user: FirebaseUser, onUserProfileLoaded: (UserProfile?) -> Unit) {
            val userId = user.uid
            val userProfileRef = FirebaseFirestore.getInstance().collection("users").document(userId)

            userProfileRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userType = document.getString("userType") ?: ""
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val email = user.email ?: ""

                        val subjects = (document.get("subjects") as? List<Map<String, Any>>)?.map {
                            TutorSubject.fromMap(it)
                        } ?: emptyList()

                        val interests = (document.get("interests") as? List<Map<String, Any>>)?.map {
                            Interest.fromMap(it)
                        } ?: emptyList()

                        val progress = (document.get("progress") as? List<Map<String, Any>>)?.map {
                            ProgressItem.fromMap(it)
                        } ?: emptyList()

                        onUserProfileLoaded(
                            UserProfile(
                                userType = userType,
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                subjects = subjects,
                                interests = interests,
                                progress = progress
                            )
                        )
                    } else {
                        onUserProfileLoaded(null) // Handle missing document
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    onUserProfileLoaded(null) // Handle failure case
                }
        }
    }
}

data class Interest(val name: String = "", var isSelected: Boolean = false) {
    companion object {
        fun fromMap(map: Map<String, Any>): Interest {
            return Interest(
                name = map["name"] as? String ?: "",
                isSelected = map["isSelected"] as? Boolean ?: false
            )
        }
    }
}
data class ProgressItem(val title: String = "", val grade: String = "") {
    companion object {
        fun fromMap(map: Map<String, Any>): ProgressItem {
            return ProgressItem(
                title = map["title"] as? String ?: "",
                grade = map["grade"] as? String ?: ""
            )
        }
    }
}
@Composable
fun InterestItem(
    interest: Interest,
    onInterestChange: (Boolean) -> Unit,
) {
    val selectedButtonColor = Color(0xFF54A4FF)
    val defaultButtonColor = Color.LightGray
    val errorButtonColor = Color.Red
    var isSelected by remember { mutableStateOf(interest.isSelected) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Subject Selection Button and Dropdown
            Button(
                onClick = { onInterestChange(!interest.isSelected)
                          },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (interest.isSelected) selectedButtonColor else defaultButtonColor),
                        //subjectError -> errorButtonColor
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(interest.name)
            }
            //---------------
    }
}


@Composable
fun ProgressInput(
    progressItem: ProgressItem,
    onProgressChange: (String, String) -> Unit,
    onRemove:() ->Unit
) {
    val selectedButtonColor = Color(0xFF54A4FF)
    val defaultButtonColor = Color.LightGray
    val errorButtonColor = Color.Red
    var title by remember { mutableStateOf(progressItem.title) }
    var grade by remember { mutableStateOf(progressItem.grade) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                onProgressChange(title, grade)},
            label = { Text("Title") },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = grade,
            onValueChange = {
                grade= it
                onProgressChange(title, grade)},
            label = { Text("Grade") },
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove",
                tint = Color.Gray
            )
        }
    }
}