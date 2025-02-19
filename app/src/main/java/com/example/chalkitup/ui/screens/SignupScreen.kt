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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CardColors
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.chalkitup.R
import com.example.chalkitup.ui.viewmodel.TutorSubject
import com.example.chalkitup.ui.viewmodel.TutorSubjectError
import com.example.chalkitup.ui.viewmodel.validateTutorSubjects

/**
 * Composable function for the Signup Screen.
 *
 * This screen allows users to create a new account by providing their details,
 * selecting their user type (Student or Tutor), and agreeing to the Terms and Conditions.
 *
 * @param authViewModel The ViewModel responsible for authentication-related operations.
 * @param certificationViewModel The ViewModel responsible for handling certifications and file uploads.
 * @param navController The NavController for navigating between screens.
 */

@Composable
fun SignupScreen(
    authViewModel: AuthViewModel,
    certificationViewModel: CertificationViewModel,
    navController: NavController
) {
    //------------------------------VARIABLES----------------------------------------------

    // Scroll states for the main form and the Terms & Conditions box.
    val scrollState = rememberScrollState() // Main form scroll state - entire screen
    val termsScrollState = rememberScrollState() // Terms & Conditions scroll state - inside Terms and Cond. box

    // State variables for Terms and Conditions agreement.
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    var hasAgreedToTerms by remember { mutableStateOf(false) }

    // Effect to track scrolling of the Terms & Conditions box.
    LaunchedEffect(termsScrollState.value) {
        if (!hasScrolledToBottom && termsScrollState.value == termsScrollState.maxValue) {
            hasScrolledToBottom = true
        }
    }

    // Context for accessing resources and system services.
    val context = LocalContext.current

    // State to hold the URIs of selected files for certification uploads.
    val selectedFiles by certificationViewModel.selectedFiles.collectAsState()

    // Launcher for selecting multiple files from the device's storage.
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                certificationViewModel.addSelectedFiles(uris)
            }
        }

    // State variables for user input fields.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf<UserType?>(null) } // Track user type: Student or Tutor
    var tutorSubjects by remember { mutableStateOf<List<TutorSubject>>(emptyList()) } // To store selected subjects

    // State variables to track input field errors.
    var emailError by remember { mutableStateOf(false) }        // Tracks Empty Field
    var invalidEmailError by remember { mutableStateOf(false) } // Tracks Empty Field
    var passwordError by remember { mutableStateOf(false) }     // Tracks Empty Field
    var confirmPasswordError by remember { mutableStateOf(false) }   // Tracks Empty Field
    var passGreaterThan6Error by remember { mutableStateOf(false) }  // Password Specific Error
    var passIncludesNumError by remember { mutableStateOf(false) }   // Password Specific Error
    var passIncludesLowerError by remember { mutableStateOf(false) } // Password Specific Error
    var passMatchError by remember { mutableStateOf(false) }         // Password Specific Error
    var firstNameError by remember { mutableStateOf(false) }    // Tracks Empty Field
    var lastNameError by remember { mutableStateOf(false) }     // Tracks Empty Field
    var userTypeError by remember { mutableStateOf(false) }     // Tracks Empty Field
    var subjectError by remember { mutableStateOf(false) }      // Tracks Empty Field
    var termsError by remember { mutableStateOf(false) }        // Tracks Not Checked

    // State to track errors in tutor subject selections.
    var tutorSubjectErrors by remember { mutableStateOf<List<TutorSubjectError>>(emptyList()) }

    // Lists for subject and grade level selections.
    val availableSubjects = listOf("Math", "Science", "English", "Social", "Biology", "Physics", "Chemistry")
    val availableGradeLevels = listOf("7", "8", "9", "10", "11", "12")
    val availableGradeLevelsBPC = listOf("11", "12")
    val grade10Specs = listOf("- 1", "- 2", "Honours")
    val grade1112Specs = listOf("- 1", "- 2", "AP", "IB")

    //val availableInterests = listOf("Art History", "Genetics", "Animals", "Astronomy", "Environment", "Health Science")
    //var location by remember { mutableStateOf("") }

    // Icons for password requirement checks.
    val passCheckIcon = Icons.Default.CheckCircle
    val passFailIcon = Icons.Default.Clear

    /* didnt like.
    val AtkinsonFont = FontFamily(
        Font(R.font.atkinson_regular, FontWeight.Normal),
        Font(R.font.atkinson_light, FontWeight.Light),
        Font(R.font.atkinson_bold, FontWeight.Bold),
        Font(R.font.atkinson_extrabold, FontWeight.ExtraBold)
    )
    */

    // Font family
    val MontserratFont = FontFamily(
        Font(R.font.montserrat_regular, FontWeight.Normal),
        Font(R.font.montserrat_semibold, FontWeight.SemiBold),
        Font(R.font.montserrat_bold, FontWeight.Bold)
    )

    // Gradient brush for the screen's background.
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF54A4FF), // 5% Blue
            Color.White, Color.White, Color.White, Color.White //95% white
        )
    )

    //------------------------------VARIABLES-END----------------------------------------------

    // Main layout for the screen.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        // Back Button . couldnt get it to look like the login page one.. will reattempt soon -Kaitlyn
        IconButton(
            onClick = { navController.navigate("start") },
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

        // Main content column.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(170.dp))

            // Title for the sign-up screen.
            Text(
                "Sign Up",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            ) // Not using rn : fontFamily = MontserratFont, -Kaitlyn

            Spacer(modifier = Modifier.height(75.dp))

            // User type selection prompt.
            Text(
                if (userTypeError) "Please select an account type"
                else "Select account type to get started",
                fontFamily = MontserratFont,
                fontSize = 14.sp,
                color = if (userTypeError) Color.Red // Changes the text color to red if there's an error.
                else Color.Gray
            ) // Otherwise, the text is gray.

            // Row that contains buttons for selecting user type (Student or Tutor).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Button for selecting the "Student" user type.
                Button(
                    onClick = {
                        userType = UserType.Student
                    }, // Sets the user type to Student when clicked.
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                        if (userType == UserType.Student) Color(0xFF06C59C) // Green color if selected.
                        else if (userTypeError) Color.Red // Red if there's an error.
                        else Color.LightGray // Light gray when not selected.
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Student", color = Color.DarkGray, fontSize = 16.sp) }

                // Button for selecting the "Tutor" user type.
                Button(
                    onClick = {
                        userType = UserType.Tutor
                    }, // Sets the user type to Tutor when clicked.
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (userType == UserType.Tutor) Color(0xFF54A4FF) // Blue color if selected.
                        else if (userTypeError) Color.Red // Red if there's an error.
                        else Color.LightGray // Light gray when not selected.
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Tutor", color = Color.DarkGray, fontSize = 16.sp) }

                // Resets userTypeError if a userType is selected.
                if (userType != null) {
                    userTypeError = false
                }
            }

            // Outlined text field for entering the user's first name.
            OutlinedTextField(
                value = firstName, // Binds the firstName variable to the input value.
                onValueChange = {
                    firstName = it // Updates the firstName when the input changes.
                    if (firstName.isNotBlank()) firstNameError =
                        false // Clears the error if input is provided.
                },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { if (firstNameError) Text("Please enter your First Name") }, // Shows an error message if there's an error.
                isError = firstNameError // Indicates that there's an error if firstNameError is true.
            )

            // Outlined text field for entering the user's last name.
            OutlinedTextField(
                value = lastName, // Binds the lastName variable to the input value.
                onValueChange = {
                    lastName = it // Updates the lastName when the input changes.
                    if (lastName.isNotBlank()) lastNameError =
                        false // Clears the error if input is provided.
                },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { if (lastNameError) Text("Please enter your Last Name") }, // Shows an error message if there's an error.
                isError = lastNameError // Indicates that there's an error if lastNameError is true.
            )

            // Outlined text field for entering the user's email address.
            OutlinedTextField(
                value = email, // Binds the email variable to the input value.
                onValueChange = {
                    email = it // Updates the email when the input changes.
                    if (email.isNotBlank()) {
                        emailError = false // Clears the error if input is provided.
                        invalidEmailError = false // Clears invalid email error if input is valid.
                    }
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    if (emailError) Text("Please enter your Email") // Shows an error message if emailError is true.
                    else if (invalidEmailError) Text("Invalid email address") // Shows an invalid email error if invalidEmailError is true.
                    else Text("") // No error message when email is valid.
                },
                isError = emailError || invalidEmailError
            )

            // Outlined text field for entering the user's password.
            OutlinedTextField(
                value = password, // Binds the password variable to the input value.
                onValueChange = {
                    password = it // Updates the password when the input changes.
                    if (password.isNotBlank()) passwordError =
                        false // Clears the error if input is provided.
                },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                supportingText = { if (passwordError) Text("Please enter your Password") }, // Shows an error message if there's a password error.
                isError = passwordError || passGreaterThan6Error || passIncludesNumError || passIncludesLowerError // Indicates that there's an error based on password validity.
            )

            // Column containing password requirement checks.
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                // Row for checking password length requirement.
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon indicating the status of the password length requirement.
                    Icon(
                        imageVector = if (password.length < 6) passFailIcon else passCheckIcon, // Displays a fail or check icon.
                        contentDescription = "Password Requirement Status",
                        tint = if (passGreaterThan6Error || passwordError) Color.Red // Red if there’s an error.
                        else if (password.length < 6) Color.Gray // Gray if password is too short.
                        else Color.Green, // Green if the password meets the requirement.
                        modifier = Modifier.size(10.dp)
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        "Is 6 or more characters long", // Displays the password requirement message.
                        color = if (passGreaterThan6Error || passwordError) Color.Red // Red if there's an error.
                        else if (password.length < 6) Color.Gray // Gray if password is too short.
                        else Color.Green, // Green if the password meets the requirement.
                        fontSize = 13.sp
                    )
                    // Clears the error if the password meets the length requirement.
                    if (password.length >= 6) {
                        passGreaterThan6Error = false
                    }
                }
                // Row for checking if the password includes at least one digit.
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon indicating the status of the password digit requirement.
                    Icon(
                        imageVector = if (password.any { it.isDigit() }) passCheckIcon else passFailIcon, // Displays a fail or check icon.
                        contentDescription = "Password Requirement Status",
                        tint = if (passIncludesNumError || passwordError) Color.Red // Red if there’s an error.
                        else if (password.any { it.isDigit() }) Color.Green // Green if the password includes a digit.
                        else Color.Gray, // Gray if the password doesn't include a digit.
                        modifier = Modifier.size(10.dp)
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        "Includes at least 1 digit", // Displays the password requirement message.
                        color = if (passIncludesNumError || passwordError) Color.Red // Red if there's an error.
                        else if (password.any { it.isDigit() }) Color.Green // Green if the password includes a digit.
                        else Color.Gray, // Gray if the password doesn't include a digit.
                        fontSize = 13.sp
                    )
                    // Clears the error if the password includes at least one digit.
                    if (password.any { it.isDigit() }) {
                        passIncludesNumError = false
                    }
                }
                // Row for checking if the password includes at least one lowercase letter.
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon indicating the status of the password lowercase letter requirement.
                    Icon(
                        imageVector = if (password.any { it.isLowerCase() }) passCheckIcon else passFailIcon, // Displays a fail or check icon.
                        contentDescription = "Password Requirement Status",
                        tint = if (passIncludesLowerError || passwordError) Color.Red // Red if there's an error.
                        else if (password.any { it.isLowerCase() }) Color.Green // Green if the password includes a lowercase letter.
                        else Color.Gray, // Gray if the password doesn't include a lowercase letter.
                        modifier = Modifier.size(10.dp)
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        "Includes at least 1 lowercase letter",  // Displays the password requirement message.
                        color = if (passIncludesLowerError || passwordError) Color.Red // Red if there's an error.
                        else if (password.any { it.isLowerCase() }) Color.Green // Green if the password includes a lowercase letter.
                        else Color.Gray, // Gray if the password doesn't include a lowercase letter.
                        fontSize = 13.sp
                    )
                    // Clears the error if the password includes at least one lowercase letter.
                    if (password.any { it.isLowerCase() }) {
                        passIncludesLowerError = false
                    }
                }
            }

            // Field for confirming the password input.
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    // Update the confirm password value and reset error if the field is not blank.
                    confirmPassword = it
                    if (confirmPassword.isNotBlank()) confirmPasswordError = false
                },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                supportingText = { if (confirmPasswordError) Text("Please confirm your Password") }, // Error message if there is a confirmation error..
                isError = confirmPasswordError || passMatchError // Show error if passwords do not match or are empty.
            )

            // Column containing the password match validation status
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                // Row to check if the password and confirm password match
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Display icon based on password match status.
                    Icon(
                        imageVector = if ((password == confirmPassword) && password.isNotEmpty()) passCheckIcon else passFailIcon,
                        contentDescription = "Password Requirement Status",
                        tint = if (passMatchError || confirmPasswordError) Color.Red
                        else if ((password != confirmPassword) || confirmPassword.isEmpty()) Color.Gray
                        else Color.Green,
                        modifier = Modifier.size(10.dp)
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    // Text showing password match status.
                    Text(
                        if (password != confirmPassword) "Passwords do not match"
                        else if (password.isEmpty()) "Password is empty"
                        else "Passwords match",
                        color = if (passMatchError || confirmPasswordError) Color.Red
                        else if ((password != confirmPassword) || confirmPassword.isEmpty()) Color.Gray
                        else Color.Green,
                        fontSize = 13.sp
                    )
                    // Reset error if passwords match.
                    if ((password == confirmPassword) && password.isNotEmpty()) {
                        passMatchError = false
                    }
                }
            }

//            OutlinedTextField(
//                value = location,
//                onValueChange = { location = it },
//                label = { Text("City") },
//                modifier = Modifier.fillMaxWidth()
//            )
            // want to remove
            //TextField(value = bio, onValueChange = { bio = it }, label = { Text("BIO") })

            Spacer(modifier = Modifier.height(8.dp))

            // Subject selection (only visible for Tutors)
            if (userType == UserType.Tutor) {
                // Display text for selecting subjects.
                Text("Select Subjects", style = MaterialTheme.typography.titleMedium)

                // Display error if no subjects have been selected.
                if (subjectError) {
                    Text(
                        "You must be able to tutor at least 1 subject",
                        color = Color.Red
                    )
                }

                // Button to add subjects to the tutor's list.
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Tap the  +  to add a subject",
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.width(50.dp))

                    // Button to add a new subject to the list.
                    IconButton(
                        onClick = {
                            // Add an empty tutor subject entry.
                            subjectError = false
                            tutorSubjects =
                                tutorSubjects + TutorSubject("", "", "") // Add empty entry
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

                // Default text if no subjects have been added yet.
                if (tutorSubjects.isEmpty()) {
                    Text(
                        text = "No subjects added",
                        color = Color.Gray
                    )
                }

                // Display list of selected subjects and their grade levels.
                Box(modifier = Modifier.heightIn(20.dp, 500.dp)) {
                    LazyColumn {
                        itemsIndexed(tutorSubjects) { index, tutorSubject ->
                            // Display each tutor subject item and its details.
                            SubjectGradeItem( // SubjectGradeItem function is defined below.
                                tutorSubject = tutorSubject,
                                availableSubjects = availableSubjects,
                                availableGradeLevels = availableGradeLevels,
                                availableGradeLevelsBPC = availableGradeLevelsBPC,
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
                                    tutorSubjectErrors = tutorSubjectErrors.toMutableList().apply {
                                        if (index < size) removeAt(index)
                                    }
                                },
                                subjectError = tutorSubjectErrors.getOrNull(index)?.subjectError
                                    ?: false,
                                gradeError = tutorSubjectErrors.getOrNull(index)?.gradeError
                                    ?: false,
                                specError = tutorSubjectErrors.getOrNull(index)?.specError ?: false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Section for uploading certifications.
                Text("Upload Certifications", style = MaterialTheme.typography.titleMedium)

                // Button to trigger file upload.
                Button(
                    onClick = { launcher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06C59C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Upload a file")
                }

                // Display selected files if any.
                if (selectedFiles.isNotEmpty()) {
                    Text(text = "Selected Files:", style = MaterialTheme.typography.titleMedium)
                    Column( // Use Column instead of LazyColumn
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedFiles.forEach { uri ->
                            val fileName =
                                certificationViewModel.getFileNameFromUri(context, uri)
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

            Spacer(modifier = Modifier.height(16.dp))

            // Terms and conditions section.
            Text("Terms and Conditions", style = MaterialTheme.typography.titleMedium)
            // Box to display the terms and conditions text with scroll.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(5.dp))
                    .padding(8.dp)
                    .verticalScroll(termsScrollState) // Separate scroll state
            ) {
                // Converted to an annotated string to individually format each line
                // Easier to edit individual lines + style them.
                Text(
                    buildAnnotatedString {
                        //MaterialTheme.typography.bodyMedium)
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Terms and Conditions\n\n")
                            append("Last Updated February 17, 2025\n\n")
                        }
                        append("Welcome to ChalkItUp! By signing up and using our platform," +
                                "you agree to the following Terms and Conditions. Please read them carefully.\n\n")

                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("1. Introduction\n")
                        }
                        append("ChalkItUp provides a platform that connects students " +
                                "with tutors for educational sessions. By using our app, you " +
                                "acknowledge that you have read, understood, and agreed to these terms.\n\n")

                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("2. User Accounts\n")
                        }
                        append("- Users must provide accurate information when creating an account.\n" +
                                "- You are responsible for maintaining the confidentiality of your login credentials.\n" +
                                "- We reserve the right to suspend or terminate accounts that violate these terms.\n\n")

                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("3. User Responsibilities\n")
                            append("- Students")
                        }
                        append(" must respect tutors' time and effort. Cancellations should be made in advance.\n")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("- Tutors")
                        }
                        append(" must provide accurate information about their qualifications and availability.\n")
                        append("- Users must communicate professionally and respectfully at all times.\n" +
                                "- Any misuse of the platform, including harassment or fraud, may result in suspension.\n\n")

                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("4. Payments and Fees\n")
                        }
                        append("Blank for now...\n\n")

                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("5. Session Cancellations and Refunds\n")
                        }
                        append("- Tutors and students should provide at least one days notice" +
                                " before canceling a session.\n\n")

                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("6. Privacy and Data Protection\n")
                        }
                        append("- We collect and store user data as described in our ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Privacy Policy.\n")
                        }

                        append("- Personal information will not be shared without user consent, " +
                                "except as required by law.\n\n")

                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("7. Prohibited Activities\n")
                        }
                        append("Users must not:\n" +
                                "- Provide false or misleading information.\n" +
                                "- Use the platform for any illegal activities.\n" +
                                "- Share or distribute inappropriate or offensive content.\n" +
                                "- Attempt to hack, manipulate, or disrupt the platform.\n\n")

                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("8. Limitation of Liability\n")
                        }
                        append("We are not responsible for:\n" +
                                "- Provide false or misleading information.\n" +
                                "- Use the platform for any illegal activities.\n" +
                                "- Share or distribute inappropriate or offensive content.\n" +
                                "- Attempt to hack, manipulate, or disrupt the platform.\n\n")

                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("9. Changes to These Terms\n")
                        }
                        append("We may update these Terms and Conditions from time to time. " +
                                "Users will be notified of significant changes, and continued use of the app " +
                                "implies acceptance of the updated terms.")
                    }
                )
            }

            // Checkbox for agreeing to terms and conditions.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Checkbox(
                    checked = hasAgreedToTerms,
                    onCheckedChange = { hasAgreedToTerms = it },
                    enabled = hasScrolledToBottom,
                    colors = CheckboxColors(
                        checkedCheckmarkColor = Color.White,
                        uncheckedCheckmarkColor = Color.DarkGray,
                        checkedBoxColor = Color(0xFF54A4FF),
                        uncheckedBoxColor = Color.White,
                        disabledCheckedBoxColor = Color.DarkGray,
                        disabledUncheckedBoxColor = Color.Gray,
                        disabledIndeterminateBoxColor = Color.DarkGray,
                        checkedBorderColor = Color(0xFF54A4FF),
                        uncheckedBorderColor = Color.DarkGray,
                        disabledBorderColor = Color.DarkGray,
                        disabledUncheckedBorderColor = Color.DarkGray,
                        disabledIndeterminateBorderColor = Color.DarkGray
                    )
                )
                Text("I have read and agree to the Terms and Conditions")
            }

            // Error message if terms are not agreed to.
            if (termsError) {
                Text(
                    "You must agree to the Terms and Conditions before signing up",
                    color = Color.Red,
                    fontSize = 15.sp
                )
            }
            // Reset the terms error if terms are agreed to.
            if (hasAgreedToTerms) {
                termsError = false
            }

            Spacer(modifier = Modifier.height(50.dp))

            // Sign-up button that validates input and triggers the signup process.
            Button(
                onClick = {
                    errorMessage = ""

                    // Validate fields before attempting signup
                    emailError = email.isEmpty()
                    firstNameError = firstName.isEmpty()
                    lastNameError = lastName.isEmpty()
                    userTypeError = (userType == null)
                    passwordError = password.isEmpty()
                    confirmPasswordError = confirmPassword.isEmpty()
                    passGreaterThan6Error = (password.length < 6)
                    passIncludesNumError = !(password.any { it.isDigit() })
                    passIncludesLowerError = !(password.any { it.isLowerCase() })
                    passMatchError = (password != confirmPassword) || password.isEmpty()
                    subjectError = ((userType == UserType.Tutor) && (tutorSubjects.isEmpty()))
                    termsError = !hasAgreedToTerms

                    // Validate tutor subjects.
                    tutorSubjectErrors = validateTutorSubjects(tutorSubjects)

                    // Proceed with signup if validation passes.
                    if (!emailError && !firstNameError && !lastNameError && !userTypeError &&
                        !passwordError && !confirmPasswordError && !subjectError && !termsError &&
                        !passGreaterThan6Error && !passIncludesNumError && !passIncludesLowerError &&
                        !passMatchError && !(tutorSubjectErrors.any { it.subjectError || it.gradeError || it.specError })
                    ) {
                        authViewModel.signupWithEmail(email, password, firstName, lastName,
                            userType!!.name, tutorSubjects,
                            onUserReady = { user ->
                                certificationViewModel.uploadFiles(context, user)
                                navController.navigate("checkEmail/verify")
                            },
                            onError = { errorMessage = it },
                            onEmailError = { invalidEmailError = true }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06C59C)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("SIGN UP", color = Color.White, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Enum to represent user types.
enum class UserType {
    Student,
    Tutor
}

/**
 * Composable function for displaying and managing the Subject, Grade, and Specialization selection for a tutor.
 *
 * This component allows a tutor to select their teaching subject, grade level, and specialization (if applicable),
 * and handles the visual state for errors, dropdown menus, and the removal of the selection.
 *
 * @param tutorSubject The current tutor's subject, grade, and specialization details.
 * @param availableSubjects The list of available subjects for the tutor to choose from.
 * @param availableGradeLevels The list of available grade levels for general subjects.
 * @param availableGradeLevelsBPC The list of available grade levels for Biology, Chemistry, and Physics.
 * @param grade10Specs The list of specializations available for grade 10.
 * @param grade1112Specs The list of specializations available for grades 11 and 12.
 * @param onSubjectChange A callback function to handle subject selection change.
 * @param onGradeChange A callback function to handle grade level selection change.
 * @param onSpecChange A callback function to handle specialization selection change.
 * @param onRemove A callback function to handle removal of the subject-grade-specialization selection.
 * @param subjectError A boolean flag indicating if there's an error with the subject selection.
 * @param gradeError A boolean flag indicating if there's an error with the grade level selection.
 * @param specError A boolean flag indicating if there's an error with the specialization selection.
 */

@Composable
fun SubjectGradeItem(
    tutorSubject: TutorSubject,
    availableSubjects: List<String>,
    availableGradeLevels: List<String>,
    availableGradeLevelsBPC: List<String>,
    grade10Specs: List<String>,
    grade1112Specs: List<String>,
    onSubjectChange: (String) -> Unit,
    onGradeChange: (String) -> Unit,
    onSpecChange: (String) -> Unit,
    onRemove: () -> Unit,
    subjectError: Boolean,
    gradeError: Boolean,
    specError: Boolean
) {
    // State variables to control the visibility of dropdown menus for subject, grade, and specialization
    var expandedSubject by remember { mutableStateOf(false) }
    var expandedGrade by remember { mutableStateOf(false) }
    var expandedSpec by remember { mutableStateOf(false) }

    // Define colors for the buttons based on their state (selected, error, default)
    val selectedButtonColor = Color(0xFF54A4FF)
    val defaultButtonColor = Color.LightGray
    val errorButtonColor = Color.Red

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Subject Selection Button and Dropdown
        Box(modifier = Modifier.weight(3.5f)) {
            Button(
                onClick = { expandedSubject = true }, // Show the subject dropdown when clicked
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        tutorSubject.subject.isNotEmpty() -> selectedButtonColor
                        subjectError -> errorButtonColor
                        else -> defaultButtonColor
                    }
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = tutorSubject.subject.ifEmpty { "Subject" }, fontSize = 14.sp)
            }

            // Subject dropdown menu
            DropdownMenu(
                expanded = expandedSubject,
                onDismissRequest = { expandedSubject = false },
                shadowElevation = 0.dp,
                containerColor = Color.Transparent,
                modifier = Modifier.width(125.dp)
            ) {
                availableSubjects.forEach { subj -> // Iterate through available subjects
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .shadow(
                                6.dp,
                                shape = RoundedCornerShape(8.dp),
                                clip = true
                            ) // Apply shadow properly
                            .background(Color.White, shape = RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text(subj) },
                            onClick = {
                                onSubjectChange(subj) // Update the subject when a selection is made
                                onGradeChange("") // Reset grade and specialization when subject changes
                                onSpecChange("")
                                expandedSubject = false // Close the dropdown
                            },
                            //modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Grade Level Selection Button and Dropdown
        Box(modifier = Modifier.weight(1.8f)) {
            Button(
                onClick = { expandedGrade = true }, // Show the grade dropdown when clicked
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        tutorSubject.grade.isNotEmpty() -> selectedButtonColor
                        gradeError -> errorButtonColor
                        else -> defaultButtonColor
                    }
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = tutorSubject.grade.ifEmpty { "Gr" }, fontSize = 14.sp)
            }

            // Determine the list of available grade levels based on the subject
            val gradeList = when (tutorSubject.subject) {
                "Biology", "Chemistry", "Physics" -> availableGradeLevelsBPC
                else -> availableGradeLevels
            }

            // Grade dropdown menu
            DropdownMenu(
                expanded = expandedGrade,
                onDismissRequest = { expandedGrade = false },
                shadowElevation = 0.dp,
                containerColor = Color.Transparent,
                modifier = Modifier.width(75.dp)
            ) {
                gradeList.forEach { grade -> // Iterate through available grade levels
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .shadow(
                                6.dp,
                                shape = RoundedCornerShape(8.dp),
                                clip = true
                            ) // Apply shadow properly
                            .background(Color.White, shape = RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text(grade) },
                            onClick = {
                                onGradeChange(grade) // Update the grade when a selection is made
                                onSpecChange("") // Reset specialization when grade level changes
                                expandedGrade = false // Close the dropdown
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Specialization Selection Button and Dropdown (only for grades 10, 11, or 12)
        if (tutorSubject.grade == "10" || tutorSubject.grade == "11" || tutorSubject.grade == "12") {
            // Specialization Selection Button
            Box(modifier = Modifier.weight(2.9f)) {
                Button(
                    onClick = {
                        expandedSpec = true
                    }, // Show the specialization dropdown when clicked
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            tutorSubject.specialization.isNotEmpty() -> selectedButtonColor
                            specError -> errorButtonColor
                            else -> defaultButtonColor
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = tutorSubject.specialization.ifEmpty { "Level" }, fontSize = 14.sp)
                }

                // Determine the list of available specializations based on the grade
                val specList = when (tutorSubject.grade) {
                    "10" -> grade10Specs
                    "11", "12" -> grade1112Specs
                    else -> emptyList() // No specializations for non-grade 10-12
                }

                // Specialization dropdown menu
                DropdownMenu(
                    expanded = expandedSpec,
                    onDismissRequest = { expandedSpec = false },
                    shadowElevation = 0.dp,
                    containerColor = Color.Transparent,
                    modifier = Modifier.width(120.dp)
                ) {
                    specList.forEach { spec -> // Iterate through available specializations
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .shadow(
                                    6.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    clip = true
                                ) // Apply shadow properly
                                .background(Color.White, shape = RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text(spec) },
                                onClick = {
                                    onSpecChange(spec) // Update the specialization when a selection is made
                                    expandedSpec = false // Close the dropdown
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Box(modifier = Modifier.weight(2.9f)) {} // Empty box when no specialization is available
            onSpecChange("") // Reset specialization if not applicable
        }

        // Remove Button to delete the subject-grade-specialization item
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove Subject",
                tint = Color.Gray
            )
        }
    }
}

/**
 * Composable function to display a selected file in a card view with an optional image preview.
 *
 * This function displays the file name and an optional image preview if the selected file is an image.
 * It also includes a delete button to remove the selected file.
 *
 * @param fileName The name of the selected file to display.
 * @param fileUri The URI of the selected file used to load the file or image.
 * @param onRemove A callback function that is triggered when the delete button is pressed to remove the selected file.
 */

@Composable
fun SelectedFileItem(fileName: String, fileUri: Uri, onRemove: () -> Unit) {
    // Get the current context and content resolver to retrieve file metadata
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    // Retrieve the MIME type of the selected file using its URI
    val mimeType = contentResolver.getType(fileUri)
    val isImage = mimeType?.startsWith("image/") == true

    // Card displaying the file information and actions
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isImage) 150.dp else 50.dp) // make height smaller if the file is not an image
            .padding(vertical = 3.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardColors(
            containerColor = Color(0xFF54A4FF),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF54A4FF),
            disabledContentColor = Color.White
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // IconButton for removing the file
                IconButton(onClick = { onRemove() }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove File")
                }
            }

            // Check if the selected file is an image
            if (isImage) {
                // Display image preview if the file is an image
                AsyncImage(
                    model = fileUri, // Load the image from the URI
                    contentDescription = "Selected Image", // Description for the image
                    modifier = Modifier
                        .fillMaxWidth()  // Ensures the image takes up the full width
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop // Ensures the image fills width and crops excess
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
