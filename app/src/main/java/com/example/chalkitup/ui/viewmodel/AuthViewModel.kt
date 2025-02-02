package com.example.chalkitup.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// AuthViewModel : Used for login and signup backend

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Function for user login
    fun loginWithEmail(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onError(task.exception?.message ?: "Login failed")
                }
            }
    }

    // Function for user signup
    fun signupWithEmail(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        userType: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Save user data in Firestore
                    val user = auth.currentUser
                    user?.let {
                        val userData = hashMapOf(
                            "userType" to userType,
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "email" to email
                        )
                        firestore.collection("users").document(user.uid)
                            .set(userData)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onError(it.message ?: "Error saving user data") }
                    }
                } else {
                    onError(task.exception?.message ?: "Signup failed")
                }
            }
    }
}
