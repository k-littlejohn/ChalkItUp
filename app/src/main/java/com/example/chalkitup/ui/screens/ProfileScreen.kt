package com.example.chalkitup.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.chalkitup.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val userProfile by viewModel.userProfile.observeAsState()
    val isTutor by viewModel.isTutor.observeAsState()
    val certifications by viewModel.certifications.observeAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Profile")

        // Display user information (common for both students and tutors)
        userProfile?.let {
            Text("Name: ${it.firstName} ${it.lastName}")
            Text("Email: ${it.email}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Conditionally render content based on user type
        if (isTutor == true) {
            // Tutor-specific information
            certifications?.let {
                if (it.isEmpty()) {
                    Text("No certifications found.")
                } else {
                    Text("Certifications:")
                    it.forEach { certificationPath ->
                        CertificationItem(certificationPath)
                    }
                }
            } ?: run {
                Text("Loading certifications...")
            }

            userProfile?.let {
                Text("Grades you can tutor: ${it.grades} ")
                Text("Subjects you can tutor: ${it.subjects}")
            }

        } else {
            // Student-specific information
        }
    }
}

// for loading tutor certifications
@Composable
fun CertificationItem(fileUrl: String) {
    Image(
        painter = rememberAsyncImagePainter(fileUrl),
        contentDescription = null,
        modifier = Modifier.size(100.dp)
    )
}
