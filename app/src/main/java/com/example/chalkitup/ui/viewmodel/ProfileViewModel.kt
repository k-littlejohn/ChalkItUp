package com.example.chalkitup.ui.viewmodel

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
    val interests: List<String> = emptyList(),
    val progress: List<String> = emptyList(),
)
