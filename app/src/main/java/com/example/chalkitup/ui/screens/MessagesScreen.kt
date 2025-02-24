package com.example.chalkitup.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

@Composable
fun MessagesScreen(
    navController: NavController,
    userId: String  // Pass user ID as a parameter
) {
    // State to hold user type
    var userType by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }


    // Scroll state for vertical scrolling
    val scrollState = rememberScrollState()

    // Fetch user type from Firestore
    LaunchedEffect(userId) {
        try {
            val userDocument = Firebase.firestore.collection("users").document(userId).get().await()
            userType = userDocument.getString("userType")
        } catch (exception: Exception) {
            Log.e("MessagesScreen", "Error fetching user type: ${exception.message}")
        }
        isLoading = false
    }

    // Show loading indicator while fetching data
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
        )
    } else {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(scrollState)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Your Questions")


        }
    }

    // UI components based on user type
    when (userType) {
        // Student-specific UI
        "Student" -> { StudentMessageScreen(navController) }

        // Tutor-specific UI
        "Tutor" -> { TutorMessageScreen(navController)
        }
    }


}

// Student-specific message screen
@Composable
fun StudentMessageScreen(navController: NavController) {
    // "Ask a new question" button
    Button(onClick = {
        navController.navigate("askQuestion")
    },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Ask a new question")
    }
    // Add more here....
}

// Tutor-specific message screen
@Composable
fun TutorMessageScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome message for tutors
        Text("Welcome, Tutor!", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        // Add more tutor-specific components here
    }
}