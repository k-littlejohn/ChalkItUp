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

    private val _academicProgress=MutableLiveData<List<String>>()
    val academicProgress: LiveData<List<String>> get() = _academicProgress

    private val _interests=MutableLiveData<List<String>>()
    val interests: LiveData<List<String>> get() = _interests


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
                    loadInterests(userId)
                    
                    _isTutor.value = user.userType == "Tutor"
                    if (user.userType == "Tutor") {
                        loadTutorCertifications(userId) // Load certifications for tutor
                    }
                    else {
                        loadStudentProgress(userId) //load progress reports for students
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
    private fun loadStudentProgress(userId: String){
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("academicProgress")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val ProgressList = querySnapshot.documents.mapNotNull { document ->
                    document.getString("fileUrl") // Get file path stored in Firestore
                }
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
    val grades: List<Int> = emptyList(),
    val quote: String ="",
    val location: String="",
    val interests: List<String> = emptyList(),
)
