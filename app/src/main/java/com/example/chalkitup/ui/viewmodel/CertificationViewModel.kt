package com.example.chalkitup.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
    //private val firestore = FirebaseFirestore.getInstance()

    // Initialize and fetch the certifications when the ViewModel is created
    init {
        getCertifications()
    }

    // Function to get the list of certifications directly from Firebase Storage
    private fun getCertifications() {
        val userId = auth.currentUser?.uid ?: return
        val storageRef = storage.reference.child("$userId/certifications")

        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                val certificationList = mutableListOf<Certification>()

                if (listResult.items.isEmpty()) {
                    _certifications.value = emptyList() // No files found
                    return@addOnSuccessListener
                }

                // Fetch URLs for each file
                listResult.items.forEach { fileRef ->
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        certificationList.add(
                            Certification(fileName = fileRef.name, fileUrl = uri.toString())
                        )

                        // Ensure we update StateFlow only after collecting all URLs
                        if (certificationList.size == listResult.items.size) {
                            _certifications.value = certificationList
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Storage", "Failed to list certifications: ${exception.message}")
            }
    }

    // Function to upload selected files to Firebase Storage
    fun uploadFiles(context: Context, user: FirebaseUser) {
        val userId = user.uid  // Use the passed user object
        val storageRef = storage.reference.child("$userId/certifications")

        _selectedFiles.value.forEach { uri ->
            val fileName = getFileNameFromUri(context, uri)
            val fileRef = storageRef.child(fileName)

            fileRef.putFile(uri)
                .addOnSuccessListener {
                    Log.d("Upload", "Storage file uploaded successfully: $fileName")

                    // Refresh the list after upload
                    getCertifications()
                    _selectedFiles.value = emptyList()
                }
                .addOnFailureListener { exception ->
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

    // Function to delete a certification file from Storage
//    fun deleteCertification(fileName: String) {
//        val userId = auth.currentUser?.uid ?: return
//        val fileRef = storage.reference.child("$userId/certifications/$fileName")
//
//        fileRef.delete()
//            .addOnSuccessListener {
//                Log.d("Delete", "Deleted file: $fileName")
//                getCertifications() // Refresh list after deletion
//            }
//            .addOnFailureListener { exception ->
//                Log.e("Delete", "Failed to delete file: ${exception.message}")
//            }
//    }

}

// Data class to represent the structure of a certification
data class Certification(val fileName: String, val fileUrl: String)
