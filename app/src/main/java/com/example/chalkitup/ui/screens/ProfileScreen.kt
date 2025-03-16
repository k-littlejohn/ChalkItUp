package com.example.chalkitup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.chalkitup.R
import com.example.chalkitup.lifecycle.AppLifecycleObserver
import com.example.chalkitup.ui.viewmodel.Certification
import com.example.chalkitup.ui.viewmodel.CertificationViewModel
import com.example.chalkitup.ui.viewmodel.ProfileViewModel
//import com.google.firebase.firestore.AggregateField.count
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Image


/**
 * Composable function for the Profile Screen.
 *
 * This screen displays the user's profile information, including their name, email, location, bio,
 * certifications, academic performance (for students), and subjects they can tutor (for tutors).
 * The profile screen also includes the ability to view and download certifications.
 *
 * @param profileViewModel The ViewModel responsible for handling user profile data.
 * @param certificationViewModel The ViewModel responsible for handling certifications and file downloads.
 * @param navController The NavController for navigating between screens.
 */
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    certificationViewModel: CertificationViewModel,
    navController: NavController
) {
    //------------------------------VARIABLES----------------------------------------------

    // Observing state changes for user profile data, tutor status, certifications, and profile picture URL.
    val userProfile by profileViewModel.userProfile.observeAsState()
    val isTutor by profileViewModel.isTutor.observeAsState()
    val certifications by certificationViewModel.certifications.collectAsState()
    val profilePictureUrl by profileViewModel.profilePictureUrl.observeAsState()

    // Scroll state.
    val scrollState = rememberScrollState()

    // Context to access system resources, such as accessing file URIs.
    val context = LocalContext.current

    // File URI for the certification that the user intends to open.
    val fileUri by certificationViewModel.fileUri.observeAsState()

    //------------------------------VARIABLES-END---------------------------------------------

    // Trigger to reload user profile when the profile screen is launched.
    LaunchedEffect(Unit) {
        profileViewModel.loadUserProfile() // Fetches the user profile when entering the profile screen
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Display user information (common for both students and tutors)
        Spacer(modifier = Modifier.height(8.dp))
        // Display profile picture with a default avatar if none exists.
        AsyncImage(
            model = profilePictureUrl ?: R.drawable.chalkitup,
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(130.dp) // og 100
                .clip(CircleShape)
                .border(2.dp, Color.Gray, CircleShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Display user details.
        userProfile?.let {
            Text(
                text = "${it.firstName} ${it.lastName}",
                fontSize = 30.sp, // Adjust the size as needed
                fontWeight = FontWeight.SemiBold // Makes the text bold
            )
            Text(it.email)

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .border(
                        width = 3.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF2196F3))
                        ),
                        shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 12.dp)
                    )
                    .padding(14.dp)
            ) {
                Text(
                    text = it.bio.ifEmpty { "" },
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

            Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            //------------------------------TUTOR-SPECIFIC----------------------------------------------

            if (isTutor == true) {

                Text("Certifications:") // Heading for certifications section

                // If no certifications found, display a message.
                if (certifications.isEmpty()) {
                    Text("No certifications found.")
                } else {
                    // Display certifications in a grid.
                    CertificationGrid(certifications,
                        onItemClick = { fileName ->
                            certificationViewModel.downloadFileToCache(context, fileName) // Download selected certification file
                        }
                    )
                }

                // Register a lifecycle observer to reset file URI when app resumes.
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = AppLifecycleObserver {
                        // Reset the fileUri when app is resumed.
                        certificationViewModel.resetFileUri()
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Observe and launch file opening if a file URI is available.
                LaunchedEffect(fileUri) {
                    fileUri?.let { uri ->
                        certificationViewModel.openFile(context, uri) // Open the certification file
                    }
                }

                // ------------- Subjects Offered (Now Swipe-able) -------------
                userProfile?.let { profile ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Subjects Offered", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(profile.subjects) { subject ->
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.LightGray)
                            ) {
                                // Temporary icons for subjects
                                val subjectIcon = when (subject.subject) {
                                    "Math" -> R.drawable.ic_math2
                                    "Physics" -> R.drawable.ic_physics2
                                    "Chemistry" -> R.drawable.ic_chemistry2
                                    "Social" -> R.drawable.ic_social2
                                    "English" -> R.drawable.ic_english2
                                    "Science" -> R.drawable.ic_science2
                                    "Biology" -> R.drawable.ic_biology
                                    else -> R.drawable.chalkitup
                                }

                                Image(
                                    painter = painterResource(id = subjectIcon),
                                    contentDescription = subject.subject,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .align(Alignment.BottomCenter)
                                ) {
                                    Text(
                                        text = subject.subject,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                    }


                    Spacer(modifier = Modifier.height(16.dp))

                    // ------------- Qualifications -------------
                    Text("Qualifications", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                    QualificationCard(
                        icon = Icons.Default.CheckCircle,
                        title = "Average Grade",
                        value = "A",
                        valueColor = Color(0xFF06C59C),
                        cardColor = Color(0xFF42A5F5)
                    )

                    QualificationCard(
                        icon = Icons.Default.Timer,
                        title = "Total Tutor Hours",
                        value = "168",
                        valueColor = Color(0xFF06C59C),
                        cardColor = Color(0xFF42A5F5)
                    )

                    QualificationCard(
                        icon = Icons.Default.Star,
                        title = "Overall Rating",
                        value = "4.4/5",
                        valueColor = Color(0xFF06C59C),
                        cardColor = Color(0xFF42A5F5)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    //Will be moving to subjects offered, keeping for now.
                    // ------------- Subjects Tutor Can Teach -------------
                    Text("Subjects you can tutor:", fontWeight = FontWeight.Bold)
                    val formattedSubjects = profile.subjects.map { subject ->
                        listOf(subject.subject, subject.grade, subject.specialization)
                            .filter { it.isNotEmpty() }
                            .joinToString(" ") // Format subject info
                    }
                    Box(modifier = Modifier.heightIn(20.dp, 500.dp)) {
                        LazyColumn {
                            items(formattedSubjects) { subject ->
                                Text(
                                    text = subject,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }

            //------------------------------TUTOR-SPECIFIC-END---------------------------------------------
            } else {
                //------------------------------STUDENT-SPECIFIC---------------------------------------------
                val addedProgress: MutableList<String> = mutableListOf()
                userProfile?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Academic Performance:",
                        fontSize = 15.sp, // Adjust the size as needed
                        fontWeight = FontWeight.Bold // Makes the text bold
                    )
                    it.progress.forEachIndexed { _, progress ->
                        if (progress.title.isNotEmpty() && progress.grade.isNotEmpty()) {
                            addedProgress.add(progress.title + ": " + progress.grade)
                        }
                    }
                if (addedProgress.isNotEmpty()) {
                    ItemGrid(addedProgress, 1)
                }
                else{
                    Text("No Progress Update")
                        }

                    }
                }
                //------------------------------STUDENT-SPECIFIC-END--------------------------------------------

            // ------------------------------ INTERESTS ----------------------------------------------
            val addedInterests: MutableList<String> = mutableListOf()
            // Display user's interests.
            userProfile?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Interests:",
                    fontSize = 15.sp, // Adjust the size as needed
                    fontWeight = FontWeight.Bold // Makes the text bold
                    )
                it.interests.forEachIndexed{ _, interest ->
                    if (interest.isSelected){
                        addedInterests.add(interest.name)
                    }
                }
            }
            if (addedInterests.isNotEmpty()) {
                ItemGrid(addedInterests, 2)
            }
            else{
                Text("No Interests Have been Listed")
            }
            //-----------------end of interest display
        }
    }


    //----------------------------------------------------------------

    //list interests
//                Spacer(modifier = Modifier.height(16.dp))
//                Text("Interests:")
//
//                if (interests.isNullOrEmpty()) {
//                    Text("No progress found.")
//                } else {
//                    ProgressGrid(interests!!)
//                }

//    Spacer(modifier = Modifier.height(16.dp))
//    // Edit Profile Button
//    Button(onClick = { navController.navigate("editProfile") })
//    { Text("Edit Profile") }
}

@Composable
fun QualificationCard(
    icon: ImageVector,
    title: String,
    value: String,
    valueColor: Color,
    cardColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(cardColor, shape = RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .background(valueColor, shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = value,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}




@Composable
fun CertificationGrid(
    certifications: List<Certification>,
    onItemClick: (String) -> Unit
) {
    Box(modifier = Modifier.height(100.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(certifications) { certification ->
                CertificationItem(certification,
                    onItemClick = { fileName ->
                        onItemClick(fileName) // Handle item click
                    }
                )
            }
        }
    }
}

/**
 * Composable function to display a grid of items (e.g., academic progress or interests).
 *
 * This function displays items in a grid with a specified number of columns.
 *
 * @param items List of items to be displayed in the grid.
 * @param columns Number of columns to display the items in.
 */
@Composable
fun ItemGrid(items: List<String>, columns: Int) {
    Box(modifier = Modifier.height(100.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items) { item ->
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(text = item, textAlign = TextAlign.Center) // Display item text
                }
            }
        }
    }
}

/**
 * Composable function to display an individual certification item.
 *
 * This function displays either an image or a file icon for each certification
 * based on the file type (e.g., image or non-image file).
 *
 * @param certification The certification item to be displayed.
 * @param onItemClick Function to handle click events on the certification item.
 */
@Composable
fun CertificationItem(
    certification: Certification,
    onItemClick: (String) -> Unit
) {
    val fileName = certification.fileName
    val fileExtension = fileName.substringAfterLast('.', "").lowercase()

    Box(
        modifier = Modifier
            .clickable {
                onItemClick(fileName) // Handle item click
            }
            .size(100.dp)
            .padding(4.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .background(Color.LightGray, shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (fileExtension in listOf("jpg", "jpeg", "png", "gif", "bmp")) {
            // Display image if the file is an image type
            AsyncImage(
                model = certification.fileUrl,
                contentDescription = "Certification Image",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            // Show file icon & filename for non-image files
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Done, // change this to better icon -Jeremelle
                    contentDescription = "File Icon",
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = fileName,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}




