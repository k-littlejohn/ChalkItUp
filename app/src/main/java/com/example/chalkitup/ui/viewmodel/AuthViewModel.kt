package com.example.chalkitup.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

// AuthViewModel : Used for login and signup backend
// authenticates and sends confirmation email
// stores userdata in database

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Function for user login
    fun loginWithEmail(email: String, password: String, onSuccess: () -> Unit, onEmailError: () -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        onSuccess() // Proceed if email is verified
                    } else {
                        onEmailError()
                    }
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
        subjects: List<String> = emptyList(),
        grades: List<Int> = emptyList(),
        onUserReady: (FirebaseUser) -> Unit, // Pass the user for file upload
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        user.reload().addOnSuccessListener {
                            val userData = hashMapOf(
                                "userType" to userType,
                                "firstName" to firstName,
                                "lastName" to lastName,
                                "email" to email,
                                "subjects" to subjects,
                                "grades" to grades,
                            )

                            // Save user data to Firestore
                            firestore.collection("users").document(user.uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    // Send email verification
                                    user.sendEmailVerification()
                                        .addOnCompleteListener { emailTask ->
                                            if (emailTask.isSuccessful) {
                                                onUserReady(user) // Pass user for file upload
                                            } else {
                                                onError(
                                                    emailTask.exception?.message
                                                        ?: "Failed to send verification email"
                                                )
                                            }
                                        }
                                }
                                .addOnFailureListener {
                                    onError(it.message ?: "Error saving user data")
                                }
                        }.addOnFailureListener {
                            onError("Error reloading user: ${it.message}")
                        }
                    } else {
                        onError("Signup successful, but user is null")
                    }
                } else {
                    onError(task.exception?.message ?: "Signup failed")
                }
            }
    }


    fun resendVerificationEmail(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.sendEmailVerification()
            ?.addOnSuccessListener { onSuccess("Email sent") }
            ?.addOnFailureListener { onError(it.message ?: "Failed to resend email") }
    }

    fun signout() {
        auth.signOut()
    }

}
