package com.example.chalkitup.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

    // LiveData to hold and observe the academic progress for students
    private val _academicProgress = MutableLiveData<List<String>>()
    val academicProgress: LiveData<List<String>> get() = _academicProgress

    // Automatically load the user profile when the ViewModel is created
    private val _interests=MutableLiveData<List<String>>()
    val interests: LiveData<List<String>> get() = _interests


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
                    loadInterests(userId)

                    _isTutor.value = user.userType == "Tutor"
                    if (user.userType == "Tutor") {
                        // Placeholder for loading tutor specific information
                        // Currently none saved yet
                        // Certification loading is handled by the CertificationViewModel
                    } else {
                        // Load academic progress for students
                        loadStudentProgress(userId)
                    }
                }
            }
    }

    // Function to load the academic progress (such as reports) for a student
    private fun loadStudentProgress(userId: String) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("academicProgress")
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Extract the file URLs for academic progress documents
                val ProgressList = querySnapshot.documents.mapNotNull { document ->
                    document.getString("fileUrl") // Get the file path stored in Firestore
                }
                // Update the academic progress LiveData
                _academicProgress.value = ProgressList
            }

    }
    private fun loadInterests(userId: String){
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("Interests")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val InterestsList = querySnapshot.documents.mapNotNull { document ->
                    document.getString("fileUrl") // Get file path stored in Firestore
                }
                _academicProgress.value = InterestsList
            }

    // Function to load the profile picture URL from Firestore
    }
    private fun loadProfilePicture(userId: String) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                // Set the profile picture URL in the LiveData
                _profilePictureUrl.value = document.getString("profilePictureUrl")
            }
    }

}

// Data class to represent the user profile data
data class UserProfile(
    val userType: String = "",  // Type of user ("Tutor" or "Student")
    val firstName: String = "", // First name of the user
    val lastName: String = "",  // Last name of the user
    val email: String = "",     // Email address of the user
    val subjects: List<String> = emptyList(), // List of subjects the user is associated with (e.g., for tutors)
    val grades: List<Int> = emptyList(),      // List of grades associated with the user (e.g., for tutors)
    val bio: String = "",        // User's bio
    val location: String = ""    // User's location
    val interests: List<String> = emptyList(),
)
