package com.example.chalkitup.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.chalkitup.R
import com.example.chalkitup.ui.viewmodel.Certification
import com.example.chalkitup.ui.viewmodel.CertificationViewModel
import com.example.chalkitup.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    certificationViewModel: CertificationViewModel,
    navController: NavController
) {
    val scrollState = rememberScrollState()

    val userProfile by profileViewModel.userProfile.observeAsState()
    val isTutor by profileViewModel.isTutor.observeAsState()

    val certifications by certificationViewModel.certifications.collectAsState()

    val academicProgress by profileViewModel.academicProgress.observeAsState()

    val profilePictureUrl by profileViewModel.profilePictureUrl.observeAsState()

    LaunchedEffect(Unit) {
        profileViewModel.loadUserProfile() // Reload data when returning to profile screen
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

        // Profile Picture
        AsyncImage(
            model = profilePictureUrl ?: R.drawable.baseline_person_24, // Use default avatar
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(2.dp, Color.Gray, CircleShape)
        )

        userProfile?.let {
            Text("${it.firstName} ${it.lastName}")
            Text(it.email)

            Spacer(modifier = Modifier.height(8.dp))

            Text("Location: ${it.location.ifEmpty { "Not specified" }}")
            Text("Bio: ${it.bio.ifEmpty { "No bio available" }}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            if (isTutor == true) {
                // Tutor-specific information
                Text("Certifications:")

                if (certifications.isEmpty()) {
                    Text("No certifications found.")
                } else {
                    CertificationGrid(certifications)
                }

                userProfile?.let {
                    Text("Grades you can tutor:")
                    ItemGrid(it.grades.map { grade -> grade.toString() }, columns = 4)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Subjects you can tutor:")
                    ItemGrid(it.subjects, columns = 4)
                }
            } else {
                //----------------STUDENT PROFILE---------------------------------
                Text("Academic Progress:")

                if (academicProgress.isNullOrEmpty()) {
                    Text("No progress found.")
                } else {
                    ProgressGrid(academicProgress!!)
                }


                //----------------------------------------------------------------
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Edit Profile Button
            Button(onClick = { navController.navigate("editProfile") }) {
                Text("Edit Profile")
            }
        }
    }
}

// Grid layout for certifications (3 items per row)
@Composable
fun CertificationGrid(certifications: List<Certification>) {
    Box(modifier = Modifier.height(100.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(certifications) { certification ->
                CertificationItem(certification)
            }
        }
    }
}

// Grid layout for grades & subjects (4 items per row)
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
                    Text(text = item, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// Display individual certification images
@Composable
fun CertificationItem(certification: Certification) {
    val fileName = certification.fileName
    val fileExtension = fileName.substringAfterLast('.', "").lowercase()

    Box(
        modifier = Modifier
            .size(100.dp)
            .padding(4.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .background(Color.LightGray, shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
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
                    imageVector = Icons.Default.Done,
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

//Grid layout for progress (1 items per row)
@Composable
fun ProgressGrid(academicProgress: List<String>) {
    Box(modifier = Modifier.height(100.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(academicProgress) { fileUrl ->
                ProgressItem(fileUrl)
            }
        }
    }
}

// Display individual progress images
@Composable
fun ProgressItem(fileUrl: String) {
    Image(
        painter = rememberAsyncImagePainter(fileUrl),
        contentDescription = null,
        modifier = Modifier
            .size(100.dp)
            .padding(4.dp)
    )
}
