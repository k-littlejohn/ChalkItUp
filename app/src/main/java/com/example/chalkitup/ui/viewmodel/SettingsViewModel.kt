package com.example.chalkitup.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class SettingsViewModel : ViewModel() {


    // Function to delete user account
    fun deleteAccount(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.delete()
            ?.addOnSuccessListener { onSuccess("Account deleted") }
            ?.addOnFailureListener { onError(it.message ?: "Failed to delete account") }
    }

}