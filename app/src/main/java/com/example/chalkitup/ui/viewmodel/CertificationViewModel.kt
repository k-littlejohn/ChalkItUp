package com.example.chalkitup.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Handles backend for tutors uploading certification files
// stores images/files in database

class CertificationViewModel : ViewModel() {

    private val _certifications = MutableStateFlow<List<Certification>>(emptyList())

    private val _selectedFiles = MutableStateFlow<List<Uri>>(emptyList()) // For managing selected files
    val selectedFiles: StateFlow<List<Uri>> = _selectedFiles

    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    init {
        getCertifications()
    }

    fun getCertifications() {
        val userId = auth.currentUser?.uid
        userId?.let {
            FirebaseFirestore.getInstance().collection("users")
                .document(it)
                .collection("certifications")
                .get()
                .addOnSuccessListener { result ->
                    _certifications.value = result.documents.map { document ->
                        Certification(
                            fileName = document.getString("fileName") ?: "Unknown",
                            fileUrl = document.getString("fileUrl") ?: ""
                        )
                    }
                }
        }
    }

    fun uploadFiles(context: Context) {
        val userId = auth.currentUser?.uid
        userId?.let {
            val storageRef = storage.reference.child("certifications/$it")

            // Loop through each selected file and upload
            _selectedFiles.value.forEach { uri ->
                val fileName = getFileNameFromUri(context, uri)
                val fileRef = storageRef.child(fileName)

                fileRef.putFile(uri).addOnSuccessListener {
                    // Once the file is uploaded, get the download URL
                    fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // Save the file metadata to Firestore using the userId
                        FirebaseFirestore.getInstance().collection("users")
                            .document(userId)
                            .collection("certifications")
                            .add(mapOf(
                                "fileName" to fileName,
                                "fileUrl" to downloadUrl.toString()
                            ))
                            .addOnSuccessListener {
                                getCertifications()
                                _selectedFiles.value = emptyList()
                            }
                    }
                }
            }
        }
    }

    fun addSelectedFiles(uris: List<Uri>) {
        _selectedFiles.value += uris
    }

    fun removeSelectedFile(uri: Uri) {
        _selectedFiles.value -= uri
    }

    // Helper function to extract file name from URI
    fun getFileNameFromUri(context: Context, uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            return it.getString(nameIndex)
        }
        return uri.path?.substringAfterLast("/") ?: "Unknown File"
    }
}

data class Certification(val fileName: String, val fileUrl: String)
