package com.example.chalkitup.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

// Handles SettingScreen logic
// - delete user's account

class SettingsViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Function to delete user account, Storage files, and Firestore database
    fun deleteAccount(onSuccess: () -> Unit, onError: () -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser

        user?.uid?.let { userId ->
            // 1. Delete user's Firestore data
            firestore.collection("users").document(userId)
                .delete()
                .addOnSuccessListener {
                    Log.d("Firestore", "Successfully deleted user from Firestore")

                    // 2. Delete user's files from storage
                    deleteStorageFiles(userId,
                        onSuccess = {
                            // 3. Delete user from Authentication
                            deleteAuthUser(onSuccess, onError) },
                        onError = {
                            Log.e("Storage", "Failed to delete user's files from storage")
                            onError()
                        }
                    )
                }
                .addOnFailureListener {
                    Log.e("Firestore", "Failed to delete user from Firestore")
                    onError()
                }

        }
    }


    // Helper function to delete user's files from Firebase Storage
    private fun deleteStorageFiles(
        userId: String,
        onSuccess: () -> Unit,
        onError: () -> Unit) {
        // reference to storage
        val storageRef = storage.reference.child("$userId/")

        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                val deleteTasks = listResult.items.map { it.delete() }
                Tasks.whenAllComplete(deleteTasks)  // Wait for all files to delete
                    .addOnSuccessListener {
                        Log.d("Storage", "Successfully deleted user's files from storage")
                        onSuccess()
                    }
                    .addOnFailureListener {
                        Log.e("Storage", "Failed to delete user's files from storage")
                        onError()
                    }
            }
            .addOnFailureListener {
                Log.e("Storage", "Failed to list files in storage")
            }
    }

    // Helper function to delete user from Firebase Authentication
    private fun deleteAuthUser(onSuccess: () -> Unit, onError: () -> Unit) {

        val user = auth.currentUser
        user?.delete()
            ?.addOnSuccessListener {
                Log.d("Authentication", "Successfully removed account from Authentication")
                onSuccess()
            }
            ?.addOnFailureListener {
                Log.e("Authentication", "Failed to delete account from Authentication")
                onError()
            }
    }


}