package com.example.chalkitup.ui.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chalkitup.ui.components.TutorSubject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminHomeViewModel : ViewModel() {

    private val _unapprovedTutors = MutableStateFlow<List<User>>(emptyList())
    val unapprovedTutors: StateFlow<List<User>> get() = _unapprovedTutors

    private val _approvedTutors = MutableStateFlow<List<User>>(emptyList())
    val approvedTutors: StateFlow<List<User>> get() = _approvedTutors

    private val db = FirebaseFirestore.getInstance()

    init {
        fetchUnapprovedTutors()
        fetchApprovedTutors()
    }

    fun fetchUnapprovedTutors() {
        viewModelScope.launch {
            try {
                val snapshot: QuerySnapshot = db.collection("users")
                    .whereEqualTo("userType", "Tutor")
                    .whereEqualTo("adminApproved", false)
                    .get()
                    .await()

                val newTutors = snapshot.documents.mapNotNull { doc ->
                    val tutor = doc.toObject(User::class.java)
                    tutor?.apply { id = doc.id }
                }

                // Add only new tutors to the existing list
                val currentTutors = _unapprovedTutors.value
                val updatedTutors = (currentTutors + newTutors).distinctBy { it.id }
                _unapprovedTutors.value = updatedTutors

            } catch (e: Exception) {
                println("Error fetching unapproved tutors: ${e.message}")
            }
        }
    }

    fun fetchApprovedTutors() {
        viewModelScope.launch {
            try {
                val snapshot: QuerySnapshot = db.collection("users")
                    .whereEqualTo("userType", "Tutor")
                    .whereEqualTo("adminApproved", true)
                    .get()
                    .await()

                val newTutors = snapshot.documents.mapNotNull { doc ->
                    val tutor = doc.toObject(User::class.java)
                    tutor?.apply { id = doc.id }
                }

                // Add only new tutors to the existing list
                val currentTutors = _approvedTutors.value
                val updatedTutors = (currentTutors + newTutors).distinctBy { it.id }
                _approvedTutors.value = updatedTutors

            } catch (e: Exception) {
                println("Error fetching approved tutors: ${e.message}")
            }
        }
    }

    fun approveTutor(tutorId: String) {
        viewModelScope.launch {
            try {
                // Update Firestore
                db.collection("users").document(tutorId)
                    .update("adminApproved", true)
                    .await()

                // Remove from unapproved list
                _unapprovedTutors.value = _unapprovedTutors.value.filterNot { it.id == tutorId }

                // Fetch the updated tutor from Firestore
                val tutorDoc = db.collection("users").document(tutorId).get().await()
                val approvedTutor = tutorDoc.toObject(User::class.java)?.apply { id = tutorDoc.id }

                // Add to the approved list if not null
                approvedTutor?.let {
                    _approvedTutors.value = (_approvedTutors.value + it).distinctBy { it.id }
                }

            } catch (e: Exception) {
                println("Error approving tutor: ${e.message}")
            }
        }
    }


//    private val _unapprovedTutors = MutableStateFlow<List<User>>(emptyList())
//    val unapprovedTutors: StateFlow<List<User>> get() = _unapprovedTutors
//
//    private val _approvedTutors = MutableStateFlow<List<User>>(emptyList())
//    val approvedTutors: StateFlow<List<User>> get() = _approvedTutors
//
//    val db = FirebaseFirestore.getInstance()
//
//    init {
//        fetchUnapprovedTutors()
//        fetchApprovedTutors()
//    }
//
//    fun fetchUnapprovedTutors() {
//        viewModelScope.launch {
//            try {
//                _unapprovedTutors.value = emptyList() // Clear the list before fetching new data
//                val snapshot: QuerySnapshot = db.collection("users")
//                    .whereEqualTo("userType", "Tutor") // Filter for tutors only
//                    .whereEqualTo("adminApproved", false) // Filter for unapproved tutors
//                    .get()
//                    .await()
//                println("unapproved tutors: $snapshot")
//                for (doc in snapshot.documents) {
//                    val tutor = doc.toObject(User::class.java)
//                    tutor?.let {
//                        tutor.id = doc.id
////                        for (unapprovedTutor in _unapprovedTutors.value) {
////                            if (unapprovedTutor.id == tutor.id) {
////                                continue
////                            } else {
////                                _unapprovedTutors.value += tutor
////                            }
////                        }     // FIX this -- dont want list to clear everytime very ugly
//                        _unapprovedTutors.value += tutor
//                    }
//                }
//                println("unapproved tutors: ${_unapprovedTutors.value}")
//            } catch (e: Exception) {
//                println("Error fetching tutors: ${e.message}")
//            }
//        }
//    }
//
//    fun fetchApprovedTutors() {
//        viewModelScope.launch {
//            try {
//                _approvedTutors.value = emptyList() // Clear the list before fetching new data
//                val snapshot: QuerySnapshot = db.collection("users")
//                    .whereEqualTo("userType", "Tutor") // Filter for tutors only
//                    .whereEqualTo("adminApproved", true) // Filter for unapproved tutors
//                    .get()
//                    .await()
//
//                for (doc in snapshot.documents) {
//                    val tutor = doc.toObject(User::class.java)
//                    tutor?.let {
//                        tutor.id = doc.id
//                        _approvedTutors.value += tutor
//                    }
//                }
//            } catch (e: Exception) {
//                println("Error fetching tutors: ${e.message}")
//            }
//        }
//    }
//
//    fun approveTutor(tutorId: String) {
//        viewModelScope.launch {
//            try {
//                db.collection("users").document(tutorId)
//                    .update("adminApproved", true)
//                    .await()
//
//                // Refresh lists after approving a tutor
//                fetchUnapprovedTutors()
//                fetchApprovedTutors()
//            } catch (e: Exception) {
//                println("Error approving tutor: ${e.message}")
//            }
//        }
//    }

    fun signout() {
        FirebaseAuth.getInstance().signOut()
    }



}

@IgnoreExtraProperties
data class User(
    var id: String = "",
    val userType: String = "",  // Type of user ("Tutor" or "Student")
    val firstName: String = "", // First name of the user
    val lastName: String = "",  // Last name of the user
    val email: String = "",     // Email address of the user
    val subjects: List<TutorSubject> = emptyList(), // List of subjects the user is associated with (for tutors)
    val adminApproved: Boolean
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "", "", emptyList(), false)
}