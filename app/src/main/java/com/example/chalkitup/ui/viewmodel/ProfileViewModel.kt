package com.example.chalkitup.ui.viewmodel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

    init {
        // Automatically load user profile when ViewModel is created
        loadUserProfile()
    }

    // Function to load the user profile when the ViewModel is initialized
    fun loadUserProfile() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userId = user.uid
            fetchUserProfile(userId)
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
}




// Data class to represent the user profile data
data class UserProfile(
    val userType: String = "",  // Type of user ("Tutor" or "Student")
    val firstName: String = "", // First name of the user
    val lastName: String = "",  // Last name of the user
    val email: String = "",     // Email address of the user
    val subjects: List<TutorSubject> = emptyList(), // List of subjects the user is associated with (for tutors)
    val bio: String = "",        // User's bio
    val location: String = "",   // User's location
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
                                            Interest("Physical Activity", false), Interest("Zoology", false)),
    val progress_item: List<String> = emptyList(),
    val progress_grade: List<String> = emptyList(),
    val progressItems: List<ProgressItem> =emptyList()
)
data class Interest(val name: String, var isSelected: Boolean)
data class ProgressItem(val title: String, val grade: String)

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
