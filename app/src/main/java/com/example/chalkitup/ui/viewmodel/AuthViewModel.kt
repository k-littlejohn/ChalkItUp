package com.example.chalkitup.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

// AuthViewModel
// Handles interactions for user:
// - signup
// - login
// - logout
// - email confirmation
// - forgot password

class AuthViewModel : ViewModel() {

    // FirebaseAuth instance for authentication operations
    private val auth = FirebaseAuth.getInstance()

    // FirebaseFirestore instance for storing user data
    private val firestore = FirebaseFirestore.getInstance()

    // Function to log in a user with email and password
    fun loginWithEmail(
        email: String,
        password: String,
        onSuccess: () -> Unit, // Callback for successful login
        onEmailError: () -> Unit, // Callback if email is not verified
        onError: (String) -> Unit // Callback for errors during login
    ) {
        // Sign in with the provided email and password
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // If login is successful, check if the email is verified
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        onSuccess() // Proceed if email is verified
                    } else {
                        onEmailError() // Prompt user to verify email
                    }
                } else {
                    // Handle error if login fails
                    onError(task.exception?.message ?: "Login failed")
                }
            }
    }

    // Function to sign up a new user with email, password, and other user information
    fun signupWithEmail(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        userType: String,
        subjects: List<String> = emptyList(),
        grades: List<Int> = emptyList(),

        //bio: String,

        location: String,

        //interests: List<String> = emptyList(),

        onUserReady: (FirebaseUser) -> Unit, // Callback with the user for file upload
        onError: (String) -> Unit // Callback for errors during signup
    ) {
        // Create a new user with the provided email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Reload user data to ensure up-to-date information
                        // Ensures the Firebase user is ready so files can be uploaded
                        user.reload().addOnSuccessListener {
                            // Prepare the user data to save in Firestore
                            val userData = hashMapOf(
                                "userType" to userType,
                                "firstName" to firstName,
                                "lastName" to lastName,
                                "email" to email,
                                "subjects" to subjects,
                                "grades" to grades,

                                //"bio" to bio,

                                "location" to location,

                                //"interests" to interests

                            )

                            // Save the user data in Firestore under their UID
                            firestore.collection("users").document(user.uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    // Send a verification email to the new user
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
                                    // Handle errors if saving user data fails
                                    onError(it.message ?: "Error saving user data")
                                }
                        }.addOnFailureListener {
                            // Handle errors if reloading the user data fails
                            onError("Error reloading user: ${it.message}")
                        }
                    } else {
                        // Handle case where user creation was successful but user is null
                        onError("Signup successful, but user is null")
                    }
                } else {
                    // Handle signup failure
                    onError(task.exception?.message ?: "Signup failed")
                }
            }
    }

    // Function to resend the email verification link to the user
    fun resendVerificationEmail(
        onSuccess: (String) -> Unit, // Callback on successful email resend
        onError: (String) -> Unit // Callback on failure to resend email
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.sendEmailVerification()
            ?.addOnSuccessListener { onSuccess("Email sent") } // If email is sent, call success callback
            ?.addOnFailureListener {
                onError(
                    it.message ?: "Failed to resend email"
                )
            } // If resend fails, call error callback
    }

    // Function to sign out the current user
    fun signout() {
        auth.signOut() // Logs out the user from FirebaseAuth
    }

    fun resetPassword(email: String,
                      onSuccess: (String) -> Unit,
                      onError: (String) -> Unit)
    {
        // add: check if email is verified? can only reset password if the email is verified?
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess("Reset email sent")
                } else {
                    onError(
                        task.exception ?.message ?:"Reset failed"
                    )
                }
            }
    }

}
