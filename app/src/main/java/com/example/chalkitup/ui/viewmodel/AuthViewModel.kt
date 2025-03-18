package com.example.chalkitup.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chalkitup.ui.components.TutorSubject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// AuthViewModel
// Handles interactions for user:
// - signup
// - login
// - logout
// - email confirmation
// - forgot password

/**
 * ViewModel for handling authentication-related operations using Firebase.
 * This class includes functions for logging in, signing up, verifying email,
 * resetting password, and managing the user session.
 */
class AuthViewModel : ViewModel() {

    // FirebaseAuth instance for authentication operations
    private val auth = FirebaseAuth.getInstance()

    // FirebaseFirestore instance for storing user data
    private val firestore = FirebaseFirestore.getInstance()

    // LiveData to track authentication state; reflects whether the user is logged in
    private val _isUserLoggedIn = MutableLiveData<Boolean>()
    val isUserLoggedIn: LiveData<Boolean> = _isUserLoggedIn

    // LiveData to track authentication state for Google login. Google does not need email verification
    private val _isGoogleUserLoggedIn = MutableLiveData<Boolean>()
    val isGoogleUserLoggedIn: LiveData<Boolean> = _isGoogleUserLoggedIn

    private val _agreedToTerms = MutableLiveData<Boolean>()

    // Initializes the ViewModel and checks if a user is already logged in
    init {
        //checkAgreedToTerms()
        checkUserLoggedIn()
    }

    // Function to check if the user is currently logged in
    // This is done by checking if the current user is non-null in FirebaseAuth
    private fun checkUserLoggedIn() {
        //checkAgreedToTerms()
        val currentUser = auth.currentUser
        // Set the LiveData value to true if a user is logged in and their email is verified, false otherwise

        _isUserLoggedIn.value = currentUser != null && currentUser.isEmailVerified //&& _agreedToTerms.value == true
        Log.e("AuthViewModel","checkUserLoggedIn: ${_isUserLoggedIn.value}")
        _isGoogleUserLoggedIn.value = currentUser != null
       
    }

    private fun checkAgreedToTerms() {
        val userId = auth.currentUser?.uid
        viewModelScope.launch {
            if (userId != null) {
                val snapshot = firestore.collection("users").document(userId)
                    .get().await()
                _agreedToTerms.value = snapshot.getBoolean("agreeToTerms") ?: false
            }
        }
    }

    fun checkEmailVerified(): Boolean {
        val user = auth.currentUser
        return user!!.isEmailVerified
    }

    /**
     * Logs in a user using email and password.
     *
     * This function signs in the user with the provided email and password.
     * If the login is successful, it checks if the email is verified.
     * If the email is not verified, it calls the onEmailError callback.
     *
     * @param email The email address of the user.
     * @param password The password associated with the user's email.
     * @param onSuccess Callback invoked when login is successful and email is verified.
     * @param onEmailError Callback invoked when the email is not verified.
     * @param onError Callback invoked if there's an error during the login process.
     */
    fun loginWithEmail(
        email: String,
        password: String,
        onSuccess: () -> Unit, // Callback for successful login
        onEmailError: () -> Unit, // Callback if email is not verified
        onTermsError: () -> Unit,
        awaitingApproval: () -> Unit,
        isAdmin: () -> Unit,
        onError: (String) -> Unit // Callback for errors during login
    ) {
        // Sign in with the provided email and password
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // If login is successful, check if the email is verified
                    val user = auth.currentUser

                    checkAgreedToTerms()

                    if (_agreedToTerms.value == false) { // check if this works timely
                        onTermsError()
                    } else if (user?.isEmailVerified == true) {
                        // Proceed if email is verified
                        isAdminApproved(
                            onResult = {
                                if (it == true) {
                                    onSuccess()
                                } else if (it == false) {
                                    awaitingApproval()
                                }
                            },
                            isAdmin = {
                                if (it == true) {
                                    isAdmin()
                                }
                            }
                        )
                    } else {
                        onEmailError() // Prompt user to verify email
                    }

                } else {
                    // Handle error if login fails
                    onError(task.exception?.message ?: "Login failed")
                }
            }
    }


    /**
     * Signs in a new user using Googles email, password,
     * and additional user information to grab users UID.
     */
    fun loginWithGoogle(
        account: GoogleSignInAccount,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _isGoogleUserLoggedIn.value = true
                    onSuccess()
                } else {
                    onError(task.exception?.message ?: "Google login failed")
                }
            }
    }

    /**
     * Signs up a new user using email, password, and additional user information.
     *
     * This function creates a new Firebase user with the provided email and password,
     * and stores the user's details (first name, last name, user type, and subjects)
     * in Firestore. After user creation, a verification email is sent.
     *
     * @param email The email address for the new user.
     * @param password The password for the new user.
     * @param firstName The first name of the new user.
     * @param lastName The last name of the new user.
     * @param userType The type of user (Student or Tutor).
     * @param subjects A list of subjects the tutor can teach (optional).
     * @param onUserReady Callback invoked with the user once they are ready for file uploads.
     * @param onError Callback invoked if there's an error during signup.
     * @param onEmailError Callback invoked if the provided email is invalid.
     */
    fun signupWithEmail(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        userType: String,
        subjects: List<TutorSubject> = emptyList(),
        onUserReady: (FirebaseUser) -> Unit, // Callback with the user for file upload
        onError: (String) -> Unit, // Callback for errors during signup
        onEmailError: () -> Unit
    ) {
        // Check if email provided is valid
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            onEmailError()
        } else {
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
                                    "agreeToTerms" to false,
                                    "adminApproved" to false
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
    }

    /**
     * Resends the email verification link to the current user.
     *
     * This function sends an email verification link to the current user
     * if they have not verified their email.
     *
     * @param onSuccess Callback invoked on successful email resend.
     * @param onError Callback invoked if the email resend fails.
     */
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

    /**
     * Signs out the current user from Firebase.
     *
     * This function logs out the currently authenticated user from FirebaseAuth.
     */
    fun signout() {
        auth.signOut() // Logs out the user from FirebaseAuth
        _isUserLoggedIn.value = false
    }

    /**
     * Sends a password reset email to the provided email address.
     *
     * This function triggers the process of resetting the password for the user.
     * A password reset email is sent to the provided email address.
     *
     * @param email The email address of the user who needs a password reset.
     * @param onSuccess Callback invoked when the password reset email is successfully sent.
     * @param onError Callback invoked if there is an error sending the reset email.
     */
    fun resetPassword(
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // add: check if email is verified? can only reset password if the email is verified? -Jeremelle
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess("Reset email sent")
                } else {
                    onError(
                        task.exception?.message ?: "Reset failed"
                    )
                }
            }
    }

    fun agreeToTerms() {
        val userId = auth.currentUser?.uid
        viewModelScope.launch {
            if (userId != null) {
                firestore.collection("users").document(userId)
                    .update("agreeToTerms", true)
            }
        }
    }

    fun isAdminApproved(onResult: (Boolean?) -> Unit, isAdmin: (Boolean?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val documentSnapshot = firestore.collection("users").document(userId).get().await()
                val userType = documentSnapshot.getString("userType")
                if (userType == "Student") {
                    onResult(true)
                } else if (userType == "Admin"){
                    isAdmin(true)
                } else {
                    val isApproved = documentSnapshot.getBoolean("adminApproved")
                    onResult(isApproved)
                }
            } catch (e: Exception) {
                println("Error fetching adminApproved status: ${e.message}")
                onResult(null) // Return null in case of an error
            }
        }
    }


}