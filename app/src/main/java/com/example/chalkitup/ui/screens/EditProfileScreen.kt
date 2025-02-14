package com.example.chalkitup.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chalkitup.R
import com.example.chalkitup.ui.viewmodel.EditProfileViewModel
import com.example.chalkitup.ui.viewmodel.TutorSubject

@Composable
fun EditProfileScreen(navController: NavController, viewModel: EditProfileViewModel) {
    val scrollState = rememberScrollState()

    val userProfile by viewModel.userProfile.observeAsState()

    val profilePictureUrl by viewModel.profilePictureUrl.observeAsState()

    val isTutor by remember(userProfile) {
        derivedStateOf { userProfile?.userType == "Tutor" }
    }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var tutorSubjects by remember { mutableStateOf<List<TutorSubject>>(emptyList()) } // To store selected subjects
    var bio by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    val availableSubjects =
        listOf("Math",
            "Science",
            "English",
            "Social",
            "Biology",
            "Physics",
            "Chemistry")
    val availableGradeLevels =
        listOf("7","8","9","10","11","12")
    val grade10Specs =
        listOf("- 1","- 2","Honours")
    val grade1112Specs =
        listOf("- 1","- 2","AP","IB")

    var originalProfilePictureUrl by remember { mutableStateOf<String?>(null) }

    // Profile picture
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.uploadProfilePicture(it) }
    }

    LaunchedEffect(userProfile) {
        userProfile?.let {
            firstName = it.firstName
            lastName = it.lastName
            email = it.email
            tutorSubjects = it.subjects
            bio = it.bio
            location = it.location
            originalProfilePictureUrl = profilePictureUrl // Save original profile picture
        }
    }

    Column(modifier = Modifier
        .padding(16.dp)
        .verticalScroll(scrollState)) {

        Text("Edit Profile")

        // Circular profile picture that acts as a button
        AsyncImage(
            model = profilePictureUrl ?: R.drawable.baseline_person_24, // Default profile picture if none is set
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(2.dp, Color.Gray, CircleShape)
                .clickable { launcher.launch("image/*") } // When clicked, allow the user to select a new image
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") }
        )

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") }
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            enabled = false // Prevent email from being edited
        )

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") }
        )

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio") }
        )

        // Tutor-Specific Fields
        if (isTutor) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Subjects You Teach")

            // need to display the subjects in editable form


            //
            // Add Subject Button
            Row (
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tap the  +  to add a subject",
                    color = Color.Gray)

                Spacer(modifier = Modifier.width(50.dp))

                IconButton(
                    onClick = {
                        tutorSubjects = tutorSubjects + TutorSubject("", "", "") // Add empty entry
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonColors(
                        Color.LightGray,
                        contentColor = Color.DarkGray,
                        disabledContainerColor = Color.DarkGray,
                        disabledContentColor = Color.DarkGray
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Subject",
                        tint = Color.DarkGray
                    )
                }
            }

            // Default text if no subjects added
            if (tutorSubjects.isEmpty()) {
                Text(
                    text = "No subjects added",
                    color = Color.Gray
                )
            }

            // List of subject-grade level pairs
            Box (modifier = Modifier.heightIn(20.dp,500.dp)) {
                LazyColumn {
                    itemsIndexed(tutorSubjects) { index, tutorSubject ->
                        SubjectGradeItem(
                            tutorSubject = tutorSubject, // Pass the entire TutorSubject object
                            availableSubjects = availableSubjects,
                            availableGradeLevels = availableGradeLevels,
                            grade10Specs = grade10Specs,
                            grade1112Specs = grade1112Specs,
                            onSubjectChange = { newSubject ->
                                tutorSubjects = tutorSubjects.toMutableList().apply {
                                    this[index] = this[index].copy(subject = newSubject)
                                }
                            },
                            onGradeChange = { newGrade ->
                                tutorSubjects = tutorSubjects.toMutableList().apply {
                                    this[index] = this[index].copy(grade = newGrade)
                                }
                            },
                            onSpecChange = { newSpec ->
                                tutorSubjects = tutorSubjects.toMutableList().apply {
                                    this[index] = this[index].copy(specialization = newSpec)
                                }
                            },
                            onRemove = {
                                tutorSubjects =
                                    tutorSubjects.toMutableList().apply { removeAt(index) }
                            }
                        )
                    }
                }
            }

        } else {
            // Student-Specific Fields

        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(onClick = {
                viewModel.updateProfile(firstName, lastName, tutorSubjects, bio, location)
                navController.popBackStack() // Navigate back
            }) {
                Text("Save Changes")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                viewModel._profilePictureUrl.value =
                    originalProfilePictureUrl // Restore old picture
                navController.popBackStack()
            }) {
                Text("Cancel")
            }
        }
    }
}
