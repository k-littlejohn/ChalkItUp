package com.example.chalkitup.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// AuthViewModel : Used for login and signup backend

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
        subjects: List<String> = emptyList(), // Default to empty list for students
        grades: List<Int> = emptyList(), // Default to empty list for students
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Authenticate user email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let{
                        // Create user data map
                        val userData = hashMapOf(
                            "userType" to userType,
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "email" to email,
                            "subjects" to subjects,
                            "grades" to grades,
                        )

                        // Save to Firestore
                        firestore.collection("users").document(user.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                // Send Email Verification
                                user.sendEmailVerification()
                                    .addOnCompleteListener { emailTask ->
                                        if (emailTask.isSuccessful) {
                                            onSuccess() //Navigate to CheckEmailScreen
                                        } else {
                                            onError(
                                                emailTask.exception?.message
                                                    ?: "Failed to send verification email"
                                            )
                                        }
                                    }
                            }
                            .addOnFailureListener {
                                onError(
                                    it.message ?: "Error saving user data"
                                )
                            }
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
