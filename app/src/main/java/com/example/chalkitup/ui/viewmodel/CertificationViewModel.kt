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

// Handles backend for tutors uploading certification files
// stores images/files in database

class CertificationViewModel : ViewModel() {

    private val _certifications = MutableStateFlow<List<Certification>>(emptyList())

    private val _selectedFiles = MutableStateFlow<List<Uri>>(emptyList()) // For managing selected files
    val selectedFiles: StateFlow<List<Uri>> = _selectedFiles

    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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

    fun uploadFiles(context: Context, user: FirebaseUser) {
        val userId = user.uid  // Use the passed user object
        val storageRef = storage.reference.child("certifications/$userId")

        _selectedFiles.value.forEach { uri ->
            val fileName = getFileNameFromUri(context, uri)
            val fileRef = storageRef.child(fileName)

            fileRef.putFile(uri)
                .addOnSuccessListener {
                    Log.d("Upload", "Storage file uploaded successfully: $fileName")

                    fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        val certificationData = mapOf(
                            "fileName" to fileName,
                            "fileUrl" to downloadUrl.toString()
                        )

                        firestore.collection("users")
                            .document(userId)
                            .collection("certifications")
                            .add(certificationData)
                            .addOnSuccessListener {
                                getCertifications()
                                _selectedFiles.value = emptyList()
                                Log.d("Upload", "Firestore file uploaded successfully: $fileName")
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Upload", "Firestore file upload failed: ${exception.message}")
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Upload", "Storage file upload failed: ${exception.message}")
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
