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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.chalkitup.R
import com.example.chalkitup.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    navController: NavController
) {
    val userProfile by viewModel.userProfile.observeAsState()
    val isTutor by viewModel.isTutor.observeAsState()
    val certifications by viewModel.certifications.observeAsState()
    val profilePictureUrl by viewModel.profilePictureUrl.observeAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            if (isTutor == true) {
                // Tutor-specific information
                Text("Certifications:")

                if (certifications.isNullOrEmpty()) {
                    Text("No certifications found.")
                } else {
                    CertificationGrid(certifications!!)
                }

                userProfile?.let {
                    Text("Grades you can tutor:")
                    ItemGrid(it.grades.map { grade -> grade.toString() }, columns = 4)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Subjects you can tutor:")
                    ItemGrid(it.subjects, columns = 4)
                }
            } else {
                // Student-specific information
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
fun CertificationGrid(certifications: List<String>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        items(certifications) { fileUrl ->
            CertificationItem(fileUrl)
        }
    }
}

// Grid layout for grades & subjects (4 items per row)
@Composable
fun ItemGrid(items: List<String>, columns: Int) {
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

// Display individual certification images
@Composable
fun CertificationItem(fileUrl: String) {
    Image(
        painter = rememberAsyncImagePainter(fileUrl),
        contentDescription = null,
        modifier = Modifier
            .size(100.dp)
            .padding(4.dp)
    )
}

