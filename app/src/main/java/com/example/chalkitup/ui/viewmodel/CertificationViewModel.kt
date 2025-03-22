package com.example.chalkitup.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.net.URLConnection

// CertificationViewModel
// Handles interactions for Tutor's certifications:
// - uploading certificate files
// - removing certificate files
// - getting certificate file information

/**
 * ViewModel to manage certifications and their associated file operations.
 *
 * This ViewModel is responsible for fetching, uploading, and deleting certifications from Firebase Storage.
 * It also holds the state of the selected files for upload and the list of certifications fetched from Firebase.
 * The ViewModel exposes live data to the UI for displaying certifications and selected files.
 */
class CertificationViewModel : ViewModel() {

    // StateFlow to hold the list of certifications fetched from Firebase Storage
    private val _certifications = MutableStateFlow<List<Certification>>(emptyList())
    val certifications: StateFlow<List<Certification>> = _certifications

    // StateFlow to hold the list of selected files for upload
    private val _selectedFiles = MutableStateFlow<List<Uri>>(emptyList()) // For managing selected files
    val selectedFiles: StateFlow<List<Uri>> = _selectedFiles

    // Firebase instances for Authentication and Storage
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // LiveData to hold the URI of a file selected for viewing or download
    private val _fileUri = MutableLiveData<Uri?>()
    val fileUri: LiveData<Uri?> = _fileUri

    // Function to reset the file URI, called after the file is opened or downloaded
    fun resetFileUri() {
        _fileUri.value = null
    }

    // Initialize and fetch the certifications when the ViewModel is created
    init {
        getCertifications() // Fetch the list of certifications from Firebase Storage
    }

    /**
     * Fetch the list of certifications directly from Firebase Storage.
     * This function retrieves all the certifications for the current user
     * and stores them in the _certifications and _selectedFiles state flows.
     */
    fun getCertifications(otherUser: String = "") {
        var userId = auth.currentUser?.uid ?: return // Get the current user ID
        if (otherUser.isNotEmpty()) {
            userId = otherUser
        }
        val storageRef = storage.reference.child("$userId/certifications")

        storageRef.listAll() // List all certifications from Firebase Storage
            .addOnSuccessListener { listResult ->

                val certificationList = mutableListOf<Certification>()
                val uriList = mutableListOf<Uri>()

                // If no files are found, update the state to empty lists
                if (listResult.items.isEmpty()) {
                    _certifications.value = emptyList()
                    _selectedFiles.value = emptyList()
                    return@addOnSuccessListener
                }

                // Fetch URLs for each file in Firebase Storage
                listResult.items.forEach { fileRef ->
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        certificationList.add(
                            Certification(fileName = fileRef.name, fileUrl = uri.toString())
                        )
                        // Convert URL to Uri and add to selectedFiles for display
                        uriList.add(uri)

                        // Update the state after all URLs are fetched
                        if (certificationList.size == listResult.items.size) {
                            _certifications.value = certificationList
                            _selectedFiles.value = uriList
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Storage", "Failed to list certifications: ${exception.message}")
            }
    }

    /**
     * Upload the selected files to Firebase Storage.
     * For each selected file, the function uploads it to Firebase Storage and then
     * fetches the list of certifications again to update the UI.
     */
    fun uploadFiles(context: Context, user: FirebaseUser) {
        val userId = user.uid  // Use the passed user object
        val storageRef = storage.reference.child("$userId/certifications")

        _selectedFiles.value.forEach { uri ->
            val fileName = getFileNameFromUri(context, uri) // Extract the file name from the URI
            val fileRef = storageRef.child(fileName)

            fileRef.putFile(uri) // Upload the file to Firebase Storage
                .addOnSuccessListener {
                    Log.d("Upload", "Storage file uploaded successfully: $fileName")
                    getCertifications() // Refresh the list after the upload
                    _selectedFiles.value = emptyList() // Clear the selected files
                }
                .addOnFailureListener { exception ->
                    Log.e("Upload", "Storage file upload failed: ${exception.message}")
                }
        }
    }

    /**
     * Update the certifications by deleting old files and uploading new ones.
     * This function compares the existing files with the selected files, deletes any extra files,
     * and uploads new files that are not already in the storage.
     */
    fun updateCertifications(context: Context) {
        val userId = auth.currentUser?.uid ?: return
        val storageRef = storage.reference.child("$userId/certifications")

        storageRef.listAll() // List current files in Firebase Storage
            .addOnSuccessListener { listResult ->
                val existingFiles = listResult.items.map { it.name }.toSet()
                val selectedFileNames = _selectedFiles.value.map { getFileNameFromUri(context, it) }.toSet()

                // Files to delete (those that are not selected anymore)
                val filesToDelete = existingFiles - selectedFileNames
                filesToDelete.forEach { fileName ->
                    storageRef.child(fileName).delete() // Delete file from Firebase Storage
                        .addOnSuccessListener {
                            Log.d("Update", "Deleted file: $fileName")
                        }
                        .addOnFailureListener { exception ->
                            Log.e("Update", "Failed to delete file: ${exception.message}")
                        }
                }

                // Files to upload (those that are selected but not already in storage)
                _selectedFiles.value.forEach { uri ->
                    val fileName = getFileNameFromUri(context, uri)
                    if (!existingFiles.contains(fileName)) {
                        val fileRef = storageRef.child(fileName)
                        fileRef.putFile(uri) // Upload new file to Firebase Storage
                            .addOnSuccessListener {
                                Log.d("Update", "Uploaded file: $fileName")
                                getCertifications() // Refresh the list after upload
                                _selectedFiles.value = emptyList() // Clear the selected files
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Update", "Failed to upload file: ${exception.message}")
                            }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Update", "Failed to list certifications: ${exception.message}")
            }
    }

    /**
     * Add selected files to the list of files to be uploaded.
     * This function appends new files to the existing list of selected files.
     */
    fun addSelectedFiles(uris: List<Uri>) {
        _selectedFiles.value += uris // Append new files to the current list
    }

    /**
     * Remove a selected file from the list of files to be uploaded.
     * This function removes the specified file from the current list of selected files.
     */
    fun removeSelectedFile(uri: Uri) {
        _selectedFiles.value -= uri // Remove the specified file from the list
    }

    /**
     * Helper function to extract the file name from a URI.
     * This function queries the content resolver to get the display name of the file
     * and returns it. If no display name is available, it falls back to extracting
     * the file name from the URI path.
     */
    fun getFileNameFromUri(context: Context, uri: Uri): String {
        // Query the content resolver to get the display name of the file
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)  // Get the column index for the display name
            it.moveToFirst()  // Move the cursor to the first row (there should be only one)
            return it.getString(nameIndex)  // Return the file name
        }
        // Fallback in case the URI doesn't contain the expected metadata
        return uri.path?.substringAfterLast("/") ?: "Unknown File"
    }

    /**
     * Download a certification file from Firebase Storage and store it in the cache.
     * After downloading, the file URI is set to the _fileUri LiveData for opening.
     */
    fun downloadFileToCache(context: Context, fileName: String, otherUser: String = "") {
        var userId = auth.currentUser?.uid ?: return
        if (otherUser.isNotEmpty()) {
            userId = otherUser
        }
        val storageRef = FirebaseStorage.getInstance().reference.child("$userId/certifications/$fileName")
        val localFile = File(context.cacheDir, fileName)

        storageRef.getFile(localFile)
            .addOnSuccessListener {
                Log.d("CertificationViewModel", "File downloaded to cache: ${localFile.absolutePath}")
                val fileUri = FileProvider.getUriForFile(
                    context,
                    "com.example.chalkitup.fileprovider",
                    localFile
                )
                Log.d("CertificationViewModel", "Generated URI: $fileUri")

                // Set the value of LiveData to the URI
                _fileUri.value = fileUri
            }
            .addOnFailureListener { exception ->
                Log.e("CertificationViewModel", "Failed to download file", exception)
                _fileUri.value = null // Reset the file URI on failure
            }
    }

    /**
     * Open the file from the given URI using an appropriate app installed on the device.
     * The function checks if an app is available to open the file type and displays it.
     */
    fun openFile(context: Context, uri: Uri) {
        Log.d("ProfileScreen", "Opening file with URI: $uri")

        val mimeType = getMimeType(File(uri.path!!)) // Get MIME type for the file

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType) // Set URI and MIME type for the intent
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        Log.d("ProfileScreen", "Intent created: $intent")

        // Check if there's an app to open the file, otherwise show a toast
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent) // Start the activity to open the file
        } else {
            Toast.makeText(context, "No app to open this file type", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Helper function to get the MIME type of a file based on its name.
     * If the MIME type cannot be determined, it returns (generic type).
    */
    private fun getMimeType(file: File): String {
        val mimeType = URLConnection.guessContentTypeFromName(file.name)
        return mimeType ?: "*/*"  // Return */* if MIME type cannot be determined
    }

}

/**
 * Data class to represent the structure of a certification.
 * This class contains the file name and the URL of the certification stored in Firebase Storage.
 */
data class Certification(val fileName: String, val fileUrl: String)
