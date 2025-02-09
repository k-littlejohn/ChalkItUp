package com.example.chalkitup.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EditProfileViewModel : ViewModel() {
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> get() = _userProfile

    private val _profilePictureUrl = MutableLiveData<String?>()
    val profilePictureUrl: LiveData<String?> get() = _profilePictureUrl

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.uid?.let { userId ->
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val userData = document.toObject(UserProfile::class.java)
                    _userProfile.value = userData
                    loadProfilePicture(userId)
                }
        }
    }

    fun updateProfile(
        firstName: String,
        lastName: String,
        subjects: List<String>,
        grades: List<Int>
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)

        val updateData = mutableMapOf<String, Any>(
            "firstName" to firstName,
            "lastName" to lastName
        )

        // Only update tutor fields if they exist
        userProfile.value?.let {
            if (it.userType == "Tutor") {
                updateData["subjects"] = subjects
                updateData["grades"] = grades
            }
        }

        userRef.update(updateData)
            .addOnSuccessListener {
                Log.d("EditProfile", "Profile updated successfully")
            }
            .addOnFailureListener {
                Log.e("EditProfile", "Profile update failed: ${it.message}")
            }
    }

    fun uploadProfilePicture(imageUri: Uri) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_pictures/$userId.jpg")

        storageRef.putFile(imageUri).continueWithTask {
            if (!it.isSuccessful) throw it.exception!!
            storageRef.downloadUrl
        }.addOnSuccessListener { uri ->
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .update("profilePictureUrl", uri.toString())
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
