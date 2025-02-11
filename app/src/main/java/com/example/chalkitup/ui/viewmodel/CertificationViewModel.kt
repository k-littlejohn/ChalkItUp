package com.example.chalkitup.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// CertificationViewModel
// Handles interactions for Tutor's certifications:
// - uploading certificate files
// - removing certificate files
// - getting certificate file information

class CertificationViewModel : ViewModel() {

    // StateFlow to hold the list of certifications
    private val _certifications = MutableStateFlow<List<Certification>>(emptyList())
    val certifications: StateFlow<List<Certification>> = _certifications

    // StateFlow to hold the selected files for upload
    private val _selectedFiles =
        MutableStateFlow<List<Uri>>(emptyList()) // For managing selected files
    val selectedFiles: StateFlow<List<Uri>> = _selectedFiles

    // Firebase instances for Authentication, Storage, and Firestore
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Initialize and fetch the certifications when the ViewModel is created
    init {
        getCertifications()
    }

    // Function to get the list of certifications from Firestore
    private fun getCertifications() {
        // Get the current user's UID from FirebaseAuth
        val userId = auth.currentUser?.uid
        userId?.let {
            // Query the user's certifications collection in Firestore
            FirebaseFirestore.getInstance().collection("users")
                .document(it)
                .collection("certifications")
                .get()
                .addOnSuccessListener { result ->
                    // Map the Firestore documents to a list of Certification objects
                    _certifications.value = result.documents.map { document ->
                        Certification(
                            fileName = document.getString("fileName") ?: "Unknown",
                            fileUrl = document.getString("fileUrl") ?: ""
                        )
                    }
                }
        }
    }

    // Function to upload selected files to Firebase Storage and save their metadata in Firestore
    fun uploadFiles(context: Context, user: FirebaseUser) {
        val userId = user.uid  // Use the passed user object
        val storageRef =
            storage.reference.child("certifications/$userId")  // Reference to the storage path for the user

        // Iterate over each selected file URI
        _selectedFiles.value.forEach { uri ->
            val fileName = getFileNameFromUri(context, uri)  // Get the file name from the URI
            val fileRef =
                storageRef.child(fileName)  // Create a reference for the file in Firebase Storage

            // Upload the file to Firebase Storage
            fileRef.putFile(uri)
                .addOnSuccessListener {
                    Log.d("Upload", "Storage file uploaded successfully: $fileName")

                    // Get the download URL of the uploaded file
                    fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // Prepare the certification data to be saved in Firestore
                        val certificationData = mapOf(
                            "fileName" to fileName,
                            "fileUrl" to downloadUrl.toString()
                        )

                        // Save the certification data in Firestore
                        firestore.collection("users")
                            .document(userId)
                            .collection("certifications")
                            .add(certificationData)
                            .addOnSuccessListener {
                                // After successful upload, refresh the list of certifications and clear selected files
                                getCertifications()
                                _selectedFiles.value = emptyList()
                                Log.d("Upload", "Firestore file uploaded successfully: $fileName")
                            }
                            .addOnFailureListener { exception ->
                                // Handle errors when saving to Firestore
                                Log.e(
                                    "Upload",
                                    "Firestore file upload failed: ${exception.message}"
                                )
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle errors during the file upload to Firebase Storage
                    Log.e("Upload", "Storage file upload failed: ${exception.message}")
                }
        }
    }

    // Function to add selected files to the list of selected files
    fun addSelectedFiles(uris: List<Uri>) {
        _selectedFiles.value += uris // Append new files to the current list
    }

    // Function to remove a selected file from the list
    fun removeSelectedFile(uri: Uri) {
        _selectedFiles.value -= uri // Remove the specified file from the list
    }

    // Helper function to extract file name from URI
    fun getFileNameFromUri(context: Context, uri: Uri): String {
        // Query the content resolver to get the display name of the file
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex =
                it.getColumnIndex(OpenableColumns.DISPLAY_NAME)  // Get the column index for the display name
            it.moveToFirst()  // Move the cursor to the first row (there should be only one)
            return it.getString(nameIndex)  // Return the file name
        }
        // Fallback in case the URI doesn't contain the expected metadata
        return uri.path?.substringAfterLast("/") ?: "Unknown File"
    }
}

// Data class to represent the structure of a certification
data class Certification(val fileName: String, val fileUrl: String)
