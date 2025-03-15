package com.example.chalkitup.ui.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chalkitup.ui.components.TutorSubject
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminHomeViewModel : ViewModel() {

    private val _unapprovedTutors = MutableStateFlow<List<User>>(emptyList())
    val unapprovedTutors: StateFlow<List<User>> get() = _unapprovedTutors

    private val _approvedTutors = MutableStateFlow<List<User>>(emptyList())
    val approvedTutors: StateFlow<List<User>> get() = _approvedTutors

    val db = FirebaseFirestore.getInstance()

    init {
        fetchTutors()
    }

    private fun fetchTutors() {
        viewModelScope.launch {
            try {
                val tutors = db.collection("users")
                    .whereEqualTo("userType", "Tutor") // Filter for tutors only
                    .get()
                    .await()
                    .documents

            } catch (e: Exception) {
                println("Error fetching availability: ${e.message}")
            }
        }
    }

    // Get all unapproved tutors



    // Get all active (already approved) tutors



}

data class User(
    val id: String = "",
    val userType: String = "",  // Type of user ("Tutor" or "Student")
    val firstName: String = "", // First name of the user
    val lastName: String = "",  // Last name of the user
    val email: String = "",     // Email address of the user
    val subjects: List<TutorSubject> = emptyList(), // List of subjects the user is associated with (for tutors)
    val adminApproved: Boolean
)