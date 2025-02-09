package com.example.chalkitup.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileViewModel : ViewModel() {
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> get() = _userProfile

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
}
