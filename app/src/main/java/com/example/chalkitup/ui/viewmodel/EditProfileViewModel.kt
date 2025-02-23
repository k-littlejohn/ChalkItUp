package com.example.chalkitup.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage

// EditProfileViewModel
// Handles EditProfileScreen logic:
// - loading profile information and profile picture
// - updating profile information and profile picture in firebase

class EditProfileViewModel : ViewModel() {

    // LiveData to hold and observe the user profile data
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> get() = _userProfile

    // LiveData to hold and observe the user's profile picture URL
    val _profilePictureUrl = MutableLiveData<String?>()
    val profilePictureUrl: LiveData<String?> get() = _profilePictureUrl

    // Temporary variable to store the updated profile picture URL
    private var tempProfilePictureUrl: String? = null // Temporary profile picture

    // Initialize and load the user's profile when the ViewModel is created
    init {
        loadUserProfile()
    }

    // Function to load the user's profile data from Firestore
    private fun loadUserProfile() {
        // Get the current user's UID from FirebaseAuth
        val user = FirebaseAuth.getInstance().currentUser
        user?.uid?.let { userId ->
            // Fetch the user profile data from Firestore
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    // Convert the Firestore document to a UserProfile object
                    val userData = document.toObject(UserProfile::class.java)
                    // Update the LiveData with the retrieved user data
                    _userProfile.value = userData
                    // Load the user's profile picture URL
                    loadProfilePicture(userId)
                }
        }
    }

    // Function to update the user's profile with the provided data to firestore
    fun updateProfile(
        firstName: String,
        lastName: String,
        subjects: List<TutorSubject>,
        bio: String,
        location: String
    ) {
        // Get the current user's UID
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)

        // Prepare the update data map for Firestore
        val updateData = mutableMapOf<String, Any>(
            "firstName" to firstName,
            "lastName" to lastName,
            "bio" to bio,
            "location" to location
        )

        // Only update tutor-specific fields if the user is a tutor
        userProfile.value?.let {
            if (it.userType == "Tutor") {
                updateData["subjects"] = subjects
            }
        }

        // If there is a new profile picture URL, add it to the update data
//        tempProfilePictureUrl?.let {
//            updateData["profilePictureUrl"] = it
//        }

        // Update the user's profile in Firestore
        userRef.update(updateData)
            .addOnSuccessListener {
                Log.d("EditProfile", "Profile updated successfully")
            }
            .addOnFailureListener {
                Log.e("EditProfile", "Profile update failed: ${it.message}")
            }
    }

    // Function to upload a new profile picture to Firebase Storage
    fun uploadProfilePicture(imageUri: Uri) {
        // Get the current user's UID
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Create a reference to Firebase Storage to store the profile picture
        val storageRef =
            FirebaseStorage.getInstance().reference.child("$userId/profilePicture.jpg")

        // Upload the image to Firebase Storage
        storageRef.putFile(imageUri).continueWithTask {
            if (!it.isSuccessful) throw it.exception!! // Handle upload failure
            // Retrieve the download URL for the uploaded image
            storageRef.downloadUrl
        }.addOnSuccessListener { uri ->
            // Update the temporary profile picture URL
            tempProfilePictureUrl = uri.toString()
            // Immediately update the UI with the new profile picture URL
            _profilePictureUrl.value = uri.toString()
        }
    }

    // Function to load the user's profile picture URL from Firestore
    private fun loadProfilePicture(userId: String) {
        val storageRef = Firebase.storage.reference.child("$userId/profilePicture.jpg")
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            _profilePictureUrl.value = uri.toString()
        }.addOnFailureListener {
            _profilePictureUrl.value = null
        }
    }

}
