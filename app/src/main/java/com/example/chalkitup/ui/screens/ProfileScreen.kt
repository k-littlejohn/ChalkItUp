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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.google.firebase.firestore.AggregateField.count

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

        // Display profile picture with a default avatar if none exists.
        AsyncImage(
            model = profilePictureUrl ?: R.drawable.baseline_person_24, // Use default avatar
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(2.dp, Color.Gray, CircleShape)
        )

        // Display user details.
        userProfile?.let {
            Text("${it.firstName} ${it.lastName}")
            Text(it.email)

            Spacer(modifier = Modifier.height(8.dp))

            Text("Bio: ${it.bio.ifEmpty { "No bio available" }}")
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

                // Display the subjects the tutor can teach.
                userProfile?.let { profile ->
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Subjects you can tutor:")
                    val formattedSubjects = profile.subjects.map { subject ->
                        listOf(subject.subject, subject.grade, subject.specialization)
                            .filter { it.isNotEmpty() }
                            .joinToString(" ") // Format subject info
                    }
                    Box(modifier = Modifier.heightIn(20.dp,500.dp)) {
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

                Text("Academic Performance:")
                userProfile?.let { profile ->
                    if (profile.progress_item.isEmpty()) {
                        Text("NO PROGRESS LISTED")
                    } else {
                        val progress_numb=profile.progress_item.size
                        for (i in 0 until progress_numb) {
                            ItemGrid(listOf(profile.progress_item[i]), columns = 1)
                            ItemGrid(listOf(profile.progress_grade[i]), columns = 1)
                        }

                    }
                }
                //------------------------------STUDENT-SPECIFIC-END--------------------------------------------
            }
            val addedInterests: MutableList<String> = mutableListOf()
            // Display user's interests.
            userProfile?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Interests:")
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

            Spacer(modifier = Modifier.height(16.dp))
            // Edit Profile Button
            Button(onClick = { navController.navigate("editProfile") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06C59C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Edit Profile")
            }
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

/**
 * Composable function to display a grid of certifications.
 *
 * This grid is displayed with three items per row. It allows users to view certifications
 * and click on them to initiate a download or open the file.
 *
 * @param certifications List of certifications to be displayed.
 * @param onItemClick Function to handle click events on certification items.
 */
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
