package com.example.chalkitup.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.chalkitup.R
import com.example.chalkitup.lifecycle.AppLifecycleObserver
import com.example.chalkitup.ui.screens.CertificationGrid
import com.example.chalkitup.ui.viewmodel.CertificationViewModel
import com.example.chalkitup.ui.viewmodel.admin.AdminHomeViewModel
import com.example.chalkitup.ui.viewmodel.admin.User

@Composable
fun AdminHome(
    navController: NavController,
    viewModel: AdminHomeViewModel,
    certificationViewModel: CertificationViewModel,
) {
    // Side drawer?
    // logout button at the bottom of drawer
    // Tutors, app insights/stats

    // Newly created tutors at the top
    // Priority -- need to be approved before accessing the app
    // have new field in tutor users -> adminApproved: true/false
    // have page for waiting to be approved tutors


    // List of all active (already approved) tutors
    // Filter options (by subject, hours/session count/current month, search)
    // clickable to view the tutors profile
    // remove button to remove the tutor from the app
    // email notification for removed tutors (account should be deleted after)

    val unapprovedTutors by viewModel.unapprovedTutors.collectAsState()
    val approvedTutors by viewModel.approvedTutors.collectAsState()

    val expandedTutorId = remember { mutableStateOf<String?>(null) }

    val showDialog = remember { mutableStateOf(false) }
    val tutorToRemove = remember { mutableStateOf<User?>(null) }

    // Gradient Background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF54A4FF), // 5% Blue
            Color.White, Color.White
        )
    )
    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush),
            contentAlignment = Alignment.TopCenter
        ) {

            Column(
                modifier = Modifier.padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    "ChalkItUp Admin",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(40.dp))

                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("New Tutors",
                        fontSize = 20.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("${unapprovedTutors.size} Tutors need to be approved")

                    unapprovedTutors.forEach { tutor ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(2.dp, Color.White),
                                colors = CardColors(
                                    containerColor = Color(0xFFd2e5fa),
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color.LightGray,
                                    disabledContentColor = Color.DarkGray
                                ),
                                onClick = {
                                    expandedTutorId.value = if (expandedTutorId.value == tutor.id) {
                                        null // Collapse the card if already expanded
                                    } else {
                                        tutor.id // Expand the clicked tutor's card
                                    }
                                }
                            ) {
                                Text(
                                    "${tutor.firstName} ${tutor.lastName}",
                                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp),
                                    fontSize = 18.sp,
                                )
                                // clickable -> dropdown with information
                                // clickable certifications
                                // approve button -> dialog // remove button -> dialog
                            }
                        }
                        if (expandedTutorId.value == tutor.id) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(2.dp, Color.White),
                                    colors = CardColors(
                                        containerColor = Color(0xFFd2e5fa),
                                        contentColor = Color.Black,
                                        disabledContainerColor = Color.LightGray,
                                        disabledContentColor = Color.DarkGray
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                    ) {
                                        Row {
                                            Text("Email:    ")
                                            Text(tutor.email, color = Color(0xFF2196F3),
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text("Subjects Offered")

                                        Spacer(modifier = Modifier.height(8.dp))

                                        SubjectDisplay(tutor)

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text("Certifications")

                                        Spacer(modifier = Modifier.height(8.dp))

                                        CertificationDisplay(tutor, certificationViewModel)

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row {
                                            Button(
                                                modifier = Modifier.weight(0.6f),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF06C59C),
                                                ),
                                                onClick = {
                                                    viewModel.approveTutor(tutor.id)
                                                    viewModel.fetchUnapprovedTutors()
                                                    viewModel.fetchApprovedTutors()

                                                }
                                            ) {
                                                Text("Approve", color = Color.White)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                modifier = Modifier.weight(0.4f),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFc93636),
                                                ),
                                                onClick = {
                                                    tutorToRemove.value = tutor
                                                    showDialog.value = true
                                                }
                                            ) {
                                                Text("Remove", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Text("Working Tutors",
                        fontSize = 20.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("${approvedTutors.size} Tutors currently having sessions")

                    approvedTutors.forEach { tutor ->

                        // profile picture



                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(2.dp, Color.White),
                                colors = CardColors(
                                    containerColor = Color(0xFFd2e5fa),
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color.LightGray,
                                    disabledContentColor = Color.DarkGray
                                ),
                                onClick = {
                                    expandedTutorId.value = if (expandedTutorId.value == tutor.id) {
                                        null // Collapse the card if already expanded
                                    } else {
                                        tutor.id // Expand the clicked tutor's card
                                    }
                                }
                            ) {
                                // profile picture
                                // add profile viewmodel to get pfp -- functions have id input?
                                Text(
                                    "${tutor.firstName} ${tutor.lastName}",
                                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp),
                                    fontSize = 18.sp,
                                )
                                // clickable -> dropdown with information
                                // clickable certifications
                                // approve button -> dialog // remove button -> dialog
                            }
                        }
                        if (expandedTutorId.value == tutor.id) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(2.dp, Color.White),
                                    colors = CardColors(
                                        containerColor = Color(0xFFd2e5fa),
                                        contentColor = Color.Black,
                                        disabledContainerColor = Color.LightGray,
                                        disabledContentColor = Color.DarkGray
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                    ) {
                                        Row {
                                            Text("Email:    ")
                                            Text(tutor.email, color = Color(0xFF2196F3),
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text("Subjects Offered")

                                        Spacer(modifier = Modifier.height(8.dp))

                                        SubjectDisplay(tutor)

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text("Certifications")

                                        Spacer(modifier = Modifier.height(8.dp))

                                        CertificationDisplay(tutor, certificationViewModel)

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row {
                                            Button(
                                                modifier = Modifier.weight(0.6f),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF06C59C),
                                                ),
                                                onClick = {
                                                    // go to tutors profile
                                                }
                                            ) {
                                                Text("View Profile", color = Color.White)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                modifier = Modifier.weight(0.4f),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFc93636),
                                                ),
                                                onClick = {
                                                    tutorToRemove.value = tutor
                                                    showDialog.value = true
                                                }
                                            ) {
                                                Text("Remove", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // clickable -> dropdown with information
                        // clickable certifications
                        // view profile button // remove this tutor button -> dialog
                    }

                    Button(onClick = {
                        viewModel.signout()
                        navController.navigate("start")
                    }) {
                        Text("Logout")
                    }


                }


            }
        }
    }

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            title = {
                Text("Confirm Deletion")
            },
            text = {
                Text("Are you sure you want to remove this tutor? All of this tutor's data will be removed from ChalkItUp.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        tutorToRemove.value?.let { tutor ->
                            //viewModel.removeTutor(tutor.id)
                            viewModel.fetchUnapprovedTutors()
                        }
                        showDialog.value = false
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

}

@Composable
fun CertificationDisplay(tutor: User, certificationViewModel: CertificationViewModel) {
    val certifications by certificationViewModel.certifications.collectAsState()

    LaunchedEffect(Unit) {
        certificationViewModel.getCertifications(tutor.id)
    }

    // Context to access system resources, such as accessing file URIs.
    val context = LocalContext.current

    // File URI for the certification that the user intends to open.
    val fileUri by certificationViewModel.fileUri.observeAsState()

    // If no certifications found, display a message.
    if (certifications.isEmpty()) {
        Text("No certifications found.")
    } else {
        // Display certifications in a grid.
        CertificationGrid(certifications,
            onItemClick = { fileName ->
                certificationViewModel.downloadFileToCache(context, fileName, tutor.id) // Download selected certification file
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
}

@Composable
fun SubjectDisplay(tutor: User) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tutor.subjects) { subject ->
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            ) {
                // Temporary icons for subjects
                val subjectIcon = when (subject.subject) {
                    "Math" -> R.drawable.ic_math
                    "Physics" -> R.drawable.ic_physics
                    "Chemistry" -> R.drawable.ic_chemistry
                    "Social" -> R.drawable.ic_social
                    "English" -> R.drawable.ic_english
                    "Science" -> R.drawable.ic_science
                    else -> R.drawable.chalkitup // havent chosen an icon yet.
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
}
