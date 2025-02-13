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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButtonColors
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.chalkitup.R

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
    var tutorSubjects by remember { mutableStateOf(emptyList<Triple<String, String, String>>()) } // To store selected subjects


    /* didnt like.
    val AtkinsonFont = FontFamily(
        Font(R.font.atkinson_regular, FontWeight.Normal),
        Font(R.font.atkinson_light, FontWeight.Light),
        Font(R.font.atkinson_bold, FontWeight.Bold),
        Font(R.font.atkinson_extrabold, FontWeight.ExtraBold)
    )
    */

    val MontserratFont = FontFamily(
        Font(R.font.montserrat_regular, FontWeight.Normal),
        Font(R.font.montserrat_semibold, FontWeight.SemiBold),
        Font(R.font.montserrat_bold, FontWeight.Bold)
    )


    var location by remember { mutableStateOf("") }

    val availableSubjects =
        listOf("Math",
            "Science",
            "English",
            "Social",
            "Biology",
            "Physics",
            "Chemistry") // Example subjects
    val availableGradeLevels =
        listOf("7","8","9","10","11","12")
    val grade10Specs =
        listOf("- 1","- 2","Honours")
    val grade1112Specs =
        listOf("- 1","- 2","AP","IB")

    //val availableInterests =
    //    listOf("Art History", "Genetics", "Animals", "Astronomy", "Environment", "Health Science")

    var hasScrolledToBottom by remember { mutableStateOf(false) }
    var hasAgreedToTerms by remember { mutableStateOf(false) }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF54A4FF), // 5% Blue
            Color.White, Color.White, Color.White, Color.White //95% white
        )
    )

    // Track scrolling for Terms & Conditions ONLY
    LaunchedEffect(termsScrollState.value) {
        if (!hasScrolledToBottom && termsScrollState.value == termsScrollState.maxValue) {
            hasScrolledToBottom = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ){
        // Back Button . couldnt get it to look like the login page one.. will reattempt soon
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .size(58.dp)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black,
                modifier = Modifier.size(106.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp), //
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(170.dp))
            Text("Sign Up", fontFamily = MontserratFont, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(75.dp))

            Text("Select account type to get started", fontFamily = MontserratFont, fontSize = 14.sp, color = Color.Gray)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { userType = UserType.Student },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (userType == UserType.Student) Color(0xFF06C59C) else Color.LightGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Student", color = Color.DarkGray, fontSize = 16.sp) }

                Button(
                    onClick = { userType = UserType.Tutor },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (userType == UserType.Tutor) Color(0xFF54A4FF) else Color.LightGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Tutor", color = Color.DarkGray, fontSize = 16.sp) }
            }

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("City") },
                modifier = Modifier.fillMaxWidth()
            )

            // want to remove
            //TextField(value = bio, onValueChange = { bio = it }, label = { Text("BIO") })

            Spacer(modifier = Modifier.height(8.dp))

            // Subject selection (only visible for Tutors)
            if (userType == UserType.Tutor) {
                Text("Select Subjects")


                // Add Subject Button
                Row (
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tap the  +  to add a subject",
                        color = Color.Gray)

                    Spacer(modifier = Modifier.width(50.dp))

                    IconButton(
                        onClick = {
                            tutorSubjects = tutorSubjects + Triple("", "", "") // Add empty entry
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
                        itemsIndexed(tutorSubjects) { index, (subject, grade, spec) ->
                            SubjectGradeItem(
                                subject = subject,
                                gradeLevel = grade,
                                gradeSpec = spec,
                                availableSubjects = availableSubjects,
                                availableGradeLevels = availableGradeLevels,
                                grade10Specs = grade10Specs,
                                grade1112Specs = grade1112Specs,
                                onSubjectChange = { newSubject ->
                                    tutorSubjects = tutorSubjects.toMutableList().apply {
                                        this[index] = Triple(newSubject, this[index].second, this[index].third)
                                    }
                                },
                                onGradeChange = { newGrade ->
                                    tutorSubjects = tutorSubjects.toMutableList().apply {
                                        this[index] = Triple(this[index].first, newGrade, this[index].third)
                                    }
                                },
                                onSpecChange = { newSpec ->
                                    tutorSubjects = tutorSubjects.toMutableList().apply {
                                        this[index] = Triple(this[index].first, this[index].second, newSpec)
                                    }
                                },
                                onRemove = {
                                    tutorSubjects =
                                        tutorSubjects.toMutableList().apply { removeAt(index) }
                                }

                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF06C59C) else Color.LightGray
                            ),
                            modifier = Modifier.padding(2.dp),
                            shape = RoundedCornerShape(corner = CornerSize(7.dp))
                        ) {
                            Text(gradeLevel.toString())

                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(8.dp))

            // Terms and Conditions
            Text("Terms and Conditions", style = MaterialTheme.typography.titleMedium)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
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
                    (tutorSubjects.isEmpty())
                ) {
                    errorMessage = "You must be able to tutor at least 1 subject"
                } else if (!hasAgreedToTerms) {
                    errorMessage = "You must agree to the Terms and Conditions"
                } else {
                    //userType!!.name passes the enum value as a string
                    authViewModel.signupWithEmail(email, password, firstName, lastName,
                        userType!!.name, tutorSubjects,
                        onUserReady = { user ->
                            certificationViewModel.uploadFiles(context, user)
                            navController.navigate("checkEmail/verify")
                        },
                        onError = { errorMessage = it }
                    )
                }
            },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06C59C)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("SIGN UP", color = Color.White, fontSize = 16.sp)
            }

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = Color.Red)
            }
        }
    }
}

// Enum to represent user types
enum class UserType {
    Student,
    Tutor
}

@Composable
fun SubjectGradeItem(
    subject: String,
    gradeLevel: String,
    gradeSpec: String,
    availableSubjects: List<String>,
    availableGradeLevels: List<String>,
    grade10Specs: List<String>,
    grade1112Specs: List<String>,
    onSubjectChange: (String) -> Unit,
    onGradeChange: (String) -> Unit,
    onSpecChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    var expandedSubject by remember { mutableStateOf(false) }
    var expandedGrade by remember { mutableStateOf(false) }
    var expandedSpec by remember { mutableStateOf(false) }

    val selectedButtonColor = Color(0xFF06C59C) // Green when selected
    val defaultButtonColor = Color.LightGray // Gray when not selected

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Subject Selection Button
        Box(modifier = Modifier
            .weight(3.5f)
            .background(Color.Transparent)) {
            Button(
                onClick = { expandedSubject = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (subject.isNotEmpty()) selectedButtonColor else defaultButtonColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = subject.ifEmpty { "Subject" })
            }

            DropdownMenu(
                expanded = expandedSubject,
                onDismissRequest = { expandedSubject = false },
                shadowElevation = 0.dp,
                containerColor = Color.Transparent,
                modifier = Modifier
                    .width(150.dp)  // Make dropdown wider
            ) {
                availableSubjects.forEach { subj ->
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .shadow(6.dp, shape = RoundedCornerShape(8.dp), clip = true) // Apply shadow properly
                            .background(Color.White, shape = RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text(subj) },
                            onClick = {
                                onSubjectChange(subj)
                                expandedSubject = false
                            },
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp) // Ensure spacing inside the menu item
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Grade Level Selection Button
        Box(modifier = Modifier
            .weight(2f)
            .background(Color.Transparent)) {
            Button(
                onClick = { expandedGrade = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (gradeLevel.isNotEmpty()) selectedButtonColor else defaultButtonColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = gradeLevel.ifEmpty { "Grade" })
            }

            DropdownMenu(
                expanded = expandedGrade,
                onDismissRequest = { expandedGrade = false },
                shadowElevation = 0.dp,
                containerColor = Color.Transparent,
                modifier = Modifier
                    .width(150.dp)  // Make dropdown wider
            ) {
                availableGradeLevels.forEach { grade ->
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .shadow(6.dp, shape = RoundedCornerShape(8.dp), clip = true) // Apply shadow properly
                            .background(Color.White, shape = RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text(grade) },
                            onClick = {
                                onGradeChange(grade)
                                onSpecChange("") // Reset specialization when grade level changes
                                expandedGrade = false
                            },
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp) // Ensure spacing inside the menu item
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (gradeLevel == "10") {
            // Spec Selection Button
            Box(modifier = Modifier
                .weight(2.5f)
                .background(Color.Transparent)) {
                Button(
                    onClick = { expandedSpec = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (gradeSpec.isNotEmpty()) selectedButtonColor else defaultButtonColor
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = gradeSpec.ifEmpty { "Level" })
                }

                DropdownMenu(
                    expanded = expandedSpec,
                    onDismissRequest = { expandedSpec = false },
                    shadowElevation = 0.dp,
                    containerColor = Color.Transparent,
                    modifier = Modifier
                        .width(150.dp)  // Make dropdown wider
                ) {
                    grade10Specs.forEach { spec ->
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .shadow(6.dp, shape = RoundedCornerShape(8.dp), clip = true) // Apply shadow properly
                                .background(Color.White, shape = RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text(spec) },
                                onClick = {
                                    onSpecChange(spec)
                                    expandedSpec = false
                                },
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp) // Ensure spacing inside the menu item
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
        } else if (gradeLevel == "11" || gradeLevel == "12") {
            // Spec Selection Button
            Box(modifier = Modifier
                .weight(2.5f)
                .background(Color.Transparent)) {
                Button(
                    onClick = { expandedSpec = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (gradeSpec.isNotEmpty()) selectedButtonColor else defaultButtonColor
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = gradeSpec.ifEmpty { "Level" })
                }

                DropdownMenu(
                    expanded = expandedSpec,
                    onDismissRequest = { expandedSpec = false },
                    shadowElevation = 0.dp,
                    containerColor = Color.Transparent,
                    modifier = Modifier
                        .width(150.dp)  // Make dropdown wider
                ) {
                    grade1112Specs.forEach { spec ->
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .shadow(6.dp, shape = RoundedCornerShape(8.dp), clip = true) // Apply shadow properly
                                .background(Color.White, shape = RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text(spec) },
                                onClick = {
                                    onSpecChange(spec)
                                    expandedSpec = false
                                },
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp) // Ensure spacing inside the menu item
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Box(modifier = Modifier
                .weight(2.5f)
                .background(Color.Transparent))
            onSpecChange("")
        }
        // Remove Button
        IconButton(onClick = onRemove) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove Subject")
        }
    }
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