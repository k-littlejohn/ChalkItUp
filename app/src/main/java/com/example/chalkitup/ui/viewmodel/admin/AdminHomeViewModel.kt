package com.example.chalkitup.ui.viewmodel.admin

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chalkitup.ui.components.TutorSubject
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
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
                fetchProfilePictures(updatedTutors)

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

    // LiveData to hold and observe the user's profile picture URL
    private val _profilePictureUrls = MutableStateFlow<Map<String, String?>>(emptyMap())
    val profilePictureUrls: StateFlow<Map<String, String?>> get() = _profilePictureUrls

    val storage = Firebase.storage

    // Function to load the profile picture from storage
    private fun fetchProfilePictures(tutors: List<User>) {
        viewModelScope.launch {
            val profileUrls = mutableMapOf<String, String?>()

            tutors.forEach { tutor ->
                val storageRef = storage.reference.child("${tutor.id}/profilePicture.jpg")
                try {
                    val uri = storageRef.downloadUrl.await()
                    profileUrls[tutor.id] = uri.toString()
                } catch (e: Exception) {
                    profileUrls[tutor.id] = null
                }
            }

            _profilePictureUrls.value = profileUrls
        }
    }




//    /**
//     * Removes a user based on their user ID.
//     * Deletes authentication account, Firestore document, and all user files from storage.
//     */
//    fun removeUser(userId: String, onComplete: (Boolean, String) -> Unit) {
//        // Step 1: Delete the user's Firestore document
//        db.collection("users").document(userId)
//            .delete()
//            .addOnSuccessListener {
//                Log.d("Firestore", "Successfully deleted user document.")
//
//                // Step 2: Delete all user's files from Storage
//                deleteStorageFiles(userId,
//                    onSuccess = {
//                        // Step 3: Delete user from Authentication
//                        //deleteAuthUser(userId, onComplete)!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//                    },
//                    onError = {
//                        onComplete(false, "Failed to delete user's files from storage.")
//                    }
//                )
//            }
//            .addOnFailureListener { e ->
//                Log.e("Firestore", "Failed to delete Firestore document: ${e.message}")
//                onComplete(false, "Failed to delete Firestore document.")
//            }
//    }
//
//    /**
//     * Deletes all user files stored in Firebase Storage.
//     */
//    private fun deleteStorageFiles(userId: String, onSuccess: () -> Unit, onError: () -> Unit) {
//        val storageRef = storage.reference.child("$userId/")
//
//        fun deleteDirectory(ref: StorageReference, onComplete: () -> Unit) {
//            ref.listAll()
//                .addOnSuccessListener { listResult ->
//                    val deleteTasks = mutableListOf<Task<Void>>()
//
//                    // Delete all files in the directory
//                    deleteTasks.addAll(listResult.items.map { it.delete() })
//
//                    // Recursively delete subdirectories
//                    listResult.prefixes.forEach { subDir ->
//                        deleteDirectory(subDir) {}  // Recursive call
//                    }
//
//                    // Wait for all deletions to complete
//                    Tasks.whenAllComplete(deleteTasks)
//                        .addOnSuccessListener { onComplete() }
//                        .addOnFailureListener { onError() }
//                }
//                .addOnFailureListener {
//                    Log.e("Storage", "Failed to list files: ${it.message}")
//                    onError()
//                }
//        }
//
//        deleteDirectory(storageRef) {
//            Log.d("Storage", "Successfully deleted all user files.")
//            onSuccess()
//        }
//    }

//    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
//    /**
//     * Deletes a user from Firebase Authentication (Admin Only).
//     */
//    private fun deleteAuthUser(userId: String, onComplete: (Boolean, String) -> Unit) {
//        auth.getUser(userId)
//            .addOnSuccessListener { firebaseUser ->
//                firebaseUser.delete()
//                    .addOnSuccessListener {
//                        Log.d("Authentication", "User successfully removed from Firebase Authentication.")
//                        onComplete(true, "User removed successfully.")
//                    }
//                    .addOnFailureListener { authError ->
//                        Log.e("Authentication", "Failed to delete user: ${authError.message}")
//                        onComplete(false, "Failed to remove user from Authentication.")
//                    }
//            }
//            .addOnFailureListener { e ->
//                Log.e("Authentication", "Failed to fetch user: ${e.message}")
//                onComplete(false, "Failed to retrieve user from Authentication.")
//            }
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