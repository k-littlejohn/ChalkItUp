package com.example.chalkitup.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chalkitup.ui.viewmodel.AuthViewModel
import com.example.chalkitup.ui.viewmodel.CertificationViewModel

// UI of signup screen

@Composable
fun SignupScreen(
    authViewModel: AuthViewModel,
    certificationViewModel: CertificationViewModel,
    navController: NavController
) {
    val scrollState = rememberScrollState() // Main form scroll state
    val termsScrollState = rememberScrollState() // Terms & Conditions scroll state

    val context = LocalContext.current
    val selectedFiles by certificationViewModel.selectedFiles.collectAsState()

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                certificationViewModel.addSelectedFiles(uris)
            }
        }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf<UserType?>(null) } // Track user type: Student or Tutor
    var selectedSubjects by remember { mutableStateOf<Set<String>>(emptySet()) } // To store selected subjects
    var selectedGradeLevels by remember { mutableStateOf<Set<Int>>(emptySet()) } // To store selected grade levels
    var selectedInterests by remember { mutableStateOf<Set<String>>(emptySet()) }
    var bio by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    val availableSubjects = listOf("Math", "Science", "English", "History", "Biology", "Physics") // Example subjects
    val availableGradeLevels = (7..12).toList() // Grade levels from 7 to 12
    val availableInterests = listOf("Art History", "Genetics", "Animals", "Astronomy", "Environment", "Health Science")
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    var hasAgreedToTerms by remember { mutableStateOf(false) }

    // Track scrolling for Terms & Conditions ONLY
    LaunchedEffect(termsScrollState.value) {
        if (!hasScrolledToBottom && termsScrollState.value == termsScrollState.maxValue) {
            hasScrolledToBottom = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(scrollState),  // Main form scroll
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Sign Up")

        // User type selection (Student / Tutor)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    userType = UserType.Student
                    selectedSubjects = emptySet() // Clear subjects list when switching to Student
                    selectedGradeLevels = emptySet() // Clear grade levels when switching to Student
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (userType == UserType.Student) Color.Blue else Color.Gray
                )
            ) {
                Text("Student")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = { userType = UserType.Tutor },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (userType == UserType.Tutor) Color.Blue else Color.Gray
                )
            ) {
                Text("Tutor")
            }
        }

        TextField(value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )

        TextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation()
        )

        TextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") }
        )

        TextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") }
        )

        // Subject selection (only visible for Tutors)
        // - Firestore and functionality purposes, change signup UI
        if (userType == UserType.Tutor) {
            Text("Select Subjects:")
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.height(200.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    userScrollEnabled = false
                ) {
                    items(availableSubjects.size) { index ->
                        val subject = availableSubjects[index]
                        val isSelected = selectedSubjects.contains(subject)
                        Button(
                            onClick = {
                                selectedSubjects = if (isSelected) {
                                    selectedSubjects - subject // Remove subject from selection
                                } else {
                                    selectedSubjects + subject // Add subject to selection
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color.Green else Color.Gray
                            ),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(subject)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grade level selection (only visible for Tutors)
            // - Firestore and functionality purposes, change signup UI
            Text("Select Grade Levels:")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                availableGradeLevels.forEach { gradeLevel ->
                    val isSelected = selectedGradeLevels.contains(gradeLevel)
                    Button(
                        onClick = {
                            selectedGradeLevels = if (isSelected) {
                                selectedGradeLevels - gradeLevel // Remove grade level from selection
                            } else {
                                selectedGradeLevels + gradeLevel // Add grade level to selection
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color.Green else Color.Gray
                        ),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(gradeLevel.toString())
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))


            Button(onClick = { launcher.launch("*/*") }) {
                Text("Select Certifications")
            }

            if (selectedFiles.isNotEmpty()) {
                Text(text = "Selected Files:", style = MaterialTheme.typography.titleMedium)
                Column( // Use Column instead of LazyColumn
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedFiles.forEach { uri ->
                        val fileName = certificationViewModel.getFileNameFromUri(context, uri)
                        SelectedFileItem(
                            fileName = fileName,
                            fileUri = uri,
                            onRemove = { certificationViewModel.removeSelectedFile(uri) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Text(text = "No files selected.", color = Color.Gray)
            }
        }

        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
        TextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name") })
        TextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") })
        TextField(value = bio, onValueChange = { bio = it }, label = { Text("BIO") })
        TextField(value = location, onValueChange = { location = it }, label = { Text("CITY") })
        //-------------interest selection
        Spacer(modifier = Modifier.width(16.dp))
        Text("Select Interests:")
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier=Modifier.height(200.dp)){LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            items(availableInterests.size) { index ->
                val Interests = availableInterests[index]
                val isSelected = selectedInterests.contains(Interests)
                Button(
                    onClick = {
                        selectedInterests = if (isSelected) {
                            selectedInterests - Interests // Remove subject from selection
                        } else {
                            selectedInterests + Interests // Add subject to selection
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color.Green else Color.Gray
                    ),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(Interests)
                }
            }
        }}
        Spacer(modifier = Modifier.height(8.dp))
//-----------------------------------------------

        // Terms and Conditions
        Text("Terms and Conditions", style = MaterialTheme.typography.titleMedium)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .border(1.dp, Color.Gray)
                .padding(8.dp)
                .verticalScroll(termsScrollState) // Separate scroll state
        ) {
            Text(
                text = termsAndConditions,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Checkbox(
                checked = hasAgreedToTerms,
                onCheckedChange = { hasAgreedToTerms = it },
                enabled = hasScrolledToBottom
            )
            Text("I have read and agree to the Terms and Conditions")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            errorMessage = ""
            // Validate fields before attempting signup
            if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                errorMessage = "All fields must be filled"
            } else if (password != confirmPassword) {
                errorMessage = "Passwords do not match"
            } else if (!hasAgreedToTerms) {
                errorMessage = "You must agree to the Terms and Conditions"
            } else if (userType == null) {
                errorMessage = "Please select a user type"
            } else if ((userType == UserType.Tutor) &&
                (selectedSubjects.isEmpty() || selectedGradeLevels.isEmpty())
            ) {
                errorMessage = "You must be able to tutor at least 1 subject and 1 grade level"
            } else if (!hasAgreedToTerms) {
                errorMessage = "You must agree to the Terms and Conditions"
            } else {
                //userType!!.name passes the enum value as a string
                authViewModel.signupWithEmail(email, password, firstName, lastName,
                    userType!!.name, selectedSubjects.toList(), selectedGradeLevels.toList(),
                    onUserReady = { user ->
                        certificationViewModel.uploadFiles(context, user)
                        navController.navigate("checkEmail/verify")
                    },
                    onError = { errorMessage = it }
                )
            }
        }
        ) {
            Text("Sign Up")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red)
        }
    }
}

// Enum to represent user types
enum class UserType {
    Student,
    Tutor
}

// UI for certification items
@Composable
fun SelectedFileItem(fileName: String, fileUri: Uri, onRemove: () -> Unit) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(fileUri)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Show image preview if the file is an image
            if (mimeType?.startsWith("image/") == true) {
                AsyncImage(
                    model = fileUri,
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .size(100.dp) // Adjust the size as needed
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Face, contentDescription = "File Icon")

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { onRemove() }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove File")
                }
            }
        }
    }
}

val termsAndConditions = """
    |**Terms and Conditions**
    |
    |**Last Updated:** February 10, 2025
    |
    |Welcome to ChalkItUp! By signing up and using our platform, you agree to the following Terms and Conditions. Please read them carefully.
    |
    |**1. Introduction**
    |ChalkItUp provides a platform that connects students with tutors for educational sessions. By using our app, you acknowledge that you have read, understood, and agreed to these terms.
    |
    |**2. User Accounts**
    |- Users must provide accurate information when creating an account.
    |- You are responsible for maintaining the confidentiality of your login credentials.
    |- We reserve the right to suspend or terminate accounts that violate these terms.
    |
    |**3. User Responsibilities**
    |- **Students** must respect tutors' time and effort. Cancellations should be made in advance.
    |- **Tutors** must provide accurate information about their qualifications and availability.
    |- Users must communicate professionally and respectfully at all times.
    |- Any misuse of the platform, including harassment or fraud, may result in suspension.
    |
    |**4. Payments and Fees**
    |
    |**5. Session Cancellations and Refunds**
    |- Tutors and students should provide at least one days notice before canceling a session.
    |
    |**6. Privacy and Data Protection**
    |- We collect and store user data as described in our **Privacy Policy**.
    |- Personal information will not be shared without user consent, except as required by law.
    |
    |**7. Prohibited Activities**
    |Users must not:
    |- Provide false or misleading information.
    |- Use the platform for any illegal activities.
    |- Share or distribute inappropriate or offensive content.
    |- Attempt to hack, manipulate, or disrupt the platform.
    |
    |**8. Limitation of Liability**
    |We are not responsible for:
    |- The quality or outcomes of tutoring sessions.
    |- Any disputes between tutors and students.
    |- Technical issues that may interrupt service.
    |
    |**9. Changes to These Terms**
    |We may update these Terms and Conditions from time to time. Users will be notified of significant changes, and continued use of the app implies acceptance of the updated terms.
    |
    |**10. Contact Us**
""".trimMargin()
