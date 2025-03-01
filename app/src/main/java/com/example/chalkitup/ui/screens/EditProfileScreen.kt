package com.example.chalkitup.ui.screens

import android.content.ClipData.Item
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chalkitup.R
import com.example.chalkitup.ui.components.SelectedFileItem
import com.example.chalkitup.ui.components.SubjectGradeItem
import com.example.chalkitup.ui.components.TutorSubject
import com.example.chalkitup.ui.components.TutorSubjectError
import com.example.chalkitup.ui.components.validateTutorSubjects
import com.example.chalkitup.ui.viewmodel.CertificationViewModel
import com.example.chalkitup.ui.viewmodel.EditProfileViewModel
import com.example.chalkitup.ui.viewmodel.Interest
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken

/**
 * EditProfileScreen
 *
 * This composable function displays the Edit Profile screen, allowing users to
 * update their profile information, including name, bio, location, subjects
 * (for tutors), and certifications. Users can also change their profile
 * picture. Form validation ensures required fields are filled before saving.
 *
 * @param navController - The NavController for navigation between screens.
 * @param editProfileViewModel - ViewModel handling user profile data and profile picture upload.
 * @param certificationViewModel - ViewModel managing certification file selection, upload, and removal.
 */
@Composable
fun EditProfileScreen(
    navController: NavController,
    editProfileViewModel: EditProfileViewModel,
    certificationViewModel: CertificationViewModel,
) {
    //------------------------------VARIABLES----------------------------------------------

    // Scroll state for vertical scrolling.
    val scrollState = rememberScrollState()

    // Observing the user's profile data from the ViewModel.
    val userProfile by editProfileViewModel.userProfile.observeAsState()

    // Determines if the user is a tutor based on their profile information.
    val isTutor by remember(userProfile) {
        derivedStateOf { userProfile?.userType == "Tutor" }
    }

    // Observing the profile picture URL from the ViewModel.
    val profilePictureUrl by editProfileViewModel.profilePictureUrl.observeAsState()

    // Store the original profile picture URL to allow restoration.
    var originalProfilePictureUrl by remember { mutableStateOf<String?>(null) }

    // Profile picture selection launcher.
    val launcherPFP = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { editProfileViewModel.uploadProfilePicture(it) }
    }

    // State variables for profile fields.
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var tutorSubjects by remember { mutableStateOf<List<TutorSubject>>(emptyList()) } // To store selected subjects
    var bio by remember { mutableStateOf("") }

    // Lists for available subjects and grade levels.
    val availableSubjects = listOf("Math", "Science", "English", "Social", "Biology", "Physics", "Chemistry")
    val availableGradeLevels = listOf("7","8","9","10","11","12")
    val availableGradeLevelsBPC = listOf("11","12")
    val grade10Specs = listOf("- 1","- 2","Honours")
    val grade1112Specs = listOf("- 1","- 2","AP","IB")
    val availablePrice = listOf("$20/hr", "$25/hr", "$30/hr", "$35/hr", "$40/hr", "$45/hr", "$50/hr", "$55/hr", "$60/hr", "$65/hr", "$70/hr", "$75/hr", "$80/hr", "$85/hr", "$90/hr", "$95/hr", "$100/hr", "$105/hr", "$110/hr", "$115/hr", "$120/hr")

    // Error states for form validation. -> all check only for empty fields
    var tutorSubjectErrors by remember { mutableStateOf<List<TutorSubjectError>>(emptyList()) }
    var firstNameError by remember { mutableStateOf(false) }
    var lastNameError by remember { mutableStateOf(false) }
    var subjectError by remember { mutableStateOf(false) }
    val progress_item = remember { mutableListOf<String>()}
    val progress_grade =remember { mutableListOf<String>() }
    var selectedInterests by remember { mutableStateOf(listOf<Int>()) }
    val mainHandler= Handler(Looper.getMainLooper())

    // Initialize profile fields when the user profile data changes.
    LaunchedEffect(userProfile) {
        userProfile?.let {
            firstName = it.firstName
            lastName = it.lastName
            email = it.email
            tutorSubjects = it.subjects
            bio = it.bio
            originalProfilePictureUrl = profilePictureUrl // Save original profile picture
        }
    }

    // Context for accessing resources and system services.
    val context = LocalContext.current

    // State to hold the URIs of selected files for certification uploads.
    val selectedFiles by certificationViewModel.selectedFiles.collectAsState()

    // Launcher for selecting multiple files from the device's storage.
    val launcherCT =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                certificationViewModel.addSelectedFiles(uris)
            }
        }

    //------------------------------VARIABLES-END----------------------------------------------

    // Main layout.
    Column(modifier = Modifier
        .padding(24.dp)
        .verticalScroll(scrollState)
        .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Edit Profile")

        // Circular profile picture with a click to change.
        AsyncImage(
            model = profilePictureUrl
                ?: R.drawable.baseline_person_24, // Default profile picture if none is set
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(2.dp, Color.Gray, CircleShape)
                .clickable { launcherPFP.launch("image/*") } // When clicked, allow the user to select a new image
        )

        // First Name input field.
        OutlinedTextField(
            value = firstName,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = {
                firstName = it
                if (firstName.isNotBlank()) firstNameError =
                    false // Clears the error if input is provided.
            },
            label = { Text("First Name") },
            supportingText = { if (firstNameError) Text("First name cannot be blank") }, // Shows an error message if there's an error.
            isError = firstNameError // Indicates that there's an error if firstNameError is true.
        )

        // Last Name input field.
        OutlinedTextField(
            value = lastName,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = {
                lastName = it
                if (lastName.isNotBlank()) lastNameError =
                    false // Clears the error if input is provided.
            },
            label = { Text("Last Name") },
            supportingText = { if (lastNameError) Text("Last name cannot be blank") }, // Shows an error message if there's an error.
            isError = lastNameError // Indicates that there's an error if lastNameError is true.
        )

//        OutlinedTextField(
//            value = email,
//            onValueChange = { email = it },
//            label = { Text("Email") },
//            enabled = false // Prevent email from being edited
//        )

        // Bio input field.
        OutlinedTextField(
            value = bio,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { bio = it },
            label = { Text("Bio") }
        )

        // Tutor-Specific Fields.
        if (isTutor) {
            Spacer(modifier = Modifier.height(16.dp))

            if (subjectError) {
                Text(
                    "You must be able to teach at least 1 subject",
                    color = Color.Red
                )
            }

            Text("Subjects You Teach")

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tap the  +  to add a subject",
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.width(50.dp))

                // Add Subject button.
                IconButton(
                    onClick = {
                        tutorSubjects =
                            tutorSubjects + TutorSubject("", "", "", "") // Add empty entry
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonColors(
                        Color(0xFF06C59C),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF06C59C),
                        disabledContentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Subject",
                        tint = Color.White
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

            // Display list of subjects the tutor teaches.
            Box(modifier = Modifier.heightIn(20.dp, 500.dp)) {
                LazyColumn {
                    itemsIndexed(tutorSubjects) { index, tutorSubject ->
                        SubjectGradeItem(
                            tutorSubject = tutorSubject, // Pass the entire TutorSubject object
                            availableSubjects = availableSubjects,
                            availableGradeLevels = availableGradeLevels,
                            availableGradeLevelsBPC = availableGradeLevelsBPC,
                            availablePrice = availablePrice,
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
                            onPriceChange = { newPrice ->
                                tutorSubjects = tutorSubjects.toMutableList().apply {
                                    this[index] = this[index].copy(price = newPrice)
                                }
                            },

                            onRemove = {
                                tutorSubjects =
                                    tutorSubjects.toMutableList().apply { removeAt(index) }
                            },
                            subjectError = tutorSubjectErrors.getOrNull(index)?.subjectError
                                ?: false,
                            gradeError = tutorSubjectErrors.getOrNull(index)?.gradeError ?: false,
                            specError = tutorSubjectErrors.getOrNull(index)?.specError ?: false,
                            priceError = tutorSubjectErrors.getOrNull(index)?.priceError ?: false
                        )
                    }
                }
            }

            // Certification file upload section.
            Text("Your Certifications", style = MaterialTheme.typography.titleMedium)

            // Button to trigger file upload.
            Button(
                onClick = { launcherCT.launch("*/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06C59C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Upload a file")
            }

            // Display selected files if any.
            if (selectedFiles.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedFiles.forEach { uri ->
                        val fileName =
                            certificationViewModel.getFileNameFromUri(context, uri)
                        SelectedFileItem(           // images not being displayed if being retrieved from db
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


        } else {
            // Student-Specific Fields

//            else {
//                UserType.Student - Specific Fields
//                        MultiSelectDropdown(
//                            availableOptions = (7..12).map { it.toString() },
//                            selectedOptions = selectedGrades.map { it.toString() },
//                            onSelectionChange = { selectedGrades = it.map { it.toInt() } }
//                        )
//            }
        }

        // Save and Cancel buttons.
//        else {
//            userProfile?.let {
//                progress_item.clear()
//                progress_item.addAll(it.progress_item)
//                progress_grade.clear()
//                progress_grade.addAll(it.progress_grade)
//
//                }
//
//            Spacer(modifier = Modifier.height(16.dp))
//            Text("Progress")
//            Box(modifier = Modifier.heightIn(20.dp, 500.dp)) {
//                Column (
//                    modifier = Modifier
//                        .padding(16.dp)
//                        .verticalScroll(scrollState)
//                ){
//                    var item_input by remember { mutableStateOf("") }
//                    var grade_input by remember { mutableStateOf("") }
//                    OutlinedTextField(
//                        value = item_input,
//                        onValueChange = { item_input = it },
//                        label = { Text("Progress Item") })
//                    OutlinedTextField(
//                        value = grade_input,
//                        onValueChange = { grade_input = it },
//                        label = { Text("Grade") })
//                    Button(onClick = {
//
//                        if (item_input.isNotBlank() && grade_input.isNotBlank()) {
//                            progress_item.add(item_input)
//                            progress_grade.add(grade_input)
//                            item_input = ""
//                            grade_input = ""
//                        }
//                    })
//                    { Text("add progress") }
//
//
//                }
//
//                LazyColumn {
//                    itemsIndexed(progress_item) { index, progItem ->
//                        Column(
//                            modifier = Modifier
//                                .padding(16.dp)
//                                .verticalScroll(scrollState)
//                        )
//                        {
//                            OutlinedTextField(
//                                value = progItem,
//                                onValueChange = { newValue ->
//                                    progress_item[index] = newValue
//                                },
//                                label = { Text("Title of Assessment") })
//                            Spacer(modifier = Modifier.height(16.dp))
//                            OutlinedTextField(
//                                value = progress_grade[index],
//                                onValueChange = { newValue ->
//                                    progress_grade[index] = newValue
//                                },
//                                label = { Text("Grade") })
//                        }
//                    }
//                }
//
//            }
//        }
        //------------------INTERESTS--------------------------------------
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            userProfile?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Interests:")
                it.interests.forEachIndexed { _, interest ->
                        SelectableButton(interest)
                    }
                }
            }
        //------------------INTERESTS--------------------------------------


        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = {
                // Error checks

                tutorSubjectErrors = validateTutorSubjects(tutorSubjects)
                subjectError = ((isTutor) && (tutorSubjects.isEmpty()))
                firstNameError = firstName.isEmpty()
                lastNameError = lastName.isEmpty()

                if (!(tutorSubjectErrors.any { it.subjectError || it.gradeError || it.specError }) &&
                        !subjectError && !firstNameError && !lastNameError
                    ) {
                    editProfileViewModel.updateProfile(firstName, lastName, tutorSubjects, bio)
                    certificationViewModel.updateCertifications(context)
                    navController.navigate("profile") // Navigate back to profile
                }
            },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06C59C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                editProfileViewModel._profilePictureUrl.value =
                    originalProfilePictureUrl // Restore old picture
                navController.navigate("profile") // Navigate back to profile
            },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06C59C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun SelectableButton(interest: Interest) {
    var isSelected by remember { mutableStateOf(interest.isSelected) }
    Button(
        onClick = { isSelected = !isSelected },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.Green else Color.Gray
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        Text(interest.name)
    }
}
