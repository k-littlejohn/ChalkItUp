package com.example.chalkitup.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Handles backend for profile viewing
// fetching user data

class ProfileViewModel : ViewModel() {
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: MutableLiveData<UserProfile?> get() = _userProfile

    private val _certifications = MutableLiveData<List<String>>()
    val certifications: LiveData<List<String>> get() = _certifications

    private val _isTutor = MutableLiveData<Boolean>()
    val isTutor: LiveData<Boolean> get() = _isTutor

    private val _profilePictureUrl = MutableLiveData<String?>()
    val profilePictureUrl: LiveData<String?> get() = _profilePictureUrl

    init {
        // Automatically load user profile when ViewModel is created
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userId = user.uid
            fetchUserProfile(userId)
        }
    }

    private fun fetchUserProfile(userId: String) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(UserProfile::class.java)
                if (user != null) {
                    _userProfile.value = user
                    loadProfilePicture(userId)
                    
                    _isTutor.value = user.userType == "Tutor"
                    if (user.userType == "Tutor") {
                        loadTutorCertifications(userId) // Load certifications for tutor
                    }
                }
            }
    }

    private fun loadTutorCertifications(userId: String) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("certifications")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val certificationsList = querySnapshot.documents.mapNotNull { document ->
                    document.getString("fileUrl") // Get file path stored in Firestore
                }
                _certifications.value = certificationsList
            }
    }

    private fun loadProfilePicture(userId: String) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                _profilePictureUrl.value = document.getString("profilePictureUrl")
            }
    }

}

data class UserProfile(
    val userType: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val subjects: List<String> = emptyList(),
    val grades: List<Int> = emptyList()
)
