package com.example.chalkitup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chalkitup.ui.viewmodel.AuthViewModel

// UI of signup screen

@Composable
fun SignupScreen(
    viewModel: AuthViewModel,
    navController: NavController
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf<UserType?>(null) } // Track user type: Student or Tutor
    var selectedSubjects by remember { mutableStateOf<Set<String>>(emptySet()) } // To store selected subjects
    var selectedGradeLevels by remember { mutableStateOf<Set<Int>>(emptySet()) } // To store selected grade levels

    val availableSubjects = listOf("Math", "Science", "English", "History", "Biology", "Physics") // Example subjects
    val availableGradeLevels = (7..12).toList() // Grade levels from 7 to 12

    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {

        Text("Sign Up")

        // User type selection (Student / Tutor)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = { userType = UserType.Student
                    selectedSubjects = emptySet() // Clear subjects list when switching to Student
                    selectedGradeLevels = emptySet() // Clear grade levels when switching to Student
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (userType == UserType.Student) Color.Blue else Color.Gray
                )
            ) {
                Text("Student")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = { userType = UserType.Tutor },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (userType == UserType.Tutor) Color.Blue else Color.Gray
                )
            ) {
                Text("Tutor")
            }
        }

        // Subject selection (only visible for Tutors)
        // - Firestore and functionality purposes, change signup UI
        if (userType == UserType.Tutor) {
            Text("Select Subjects:")
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                items(availableSubjects.size) { index ->
                    val subject = availableSubjects[index]
                    val isSelected = selectedSubjects.contains(subject)
                    Button(
                        onClick = {
                            selectedSubjects = if (isSelected) {
                                selectedSubjects - subject // Remove subject from selection
                            } else {
                                selectedSubjects + subject // Add subject to selection
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color.Green else Color.Gray
                        ),
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(subject)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))


            // Grade level selection (only visible for Tutors)
            // - Firestore and functionality purposes, change signup UI
            Text("Select Grade Levels:")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                availableGradeLevels.forEach { gradeLevel ->
                    val isSelected = selectedGradeLevels.contains(gradeLevel)
                    Button(
                        onClick = {
                            selectedGradeLevels = if (isSelected) {
                                selectedGradeLevels - gradeLevel // Remove grade level from selection
                            } else {
                                selectedGradeLevels + gradeLevel // Add grade level to selection
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color.Green else Color.Gray
                        ),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(gradeLevel.toString())
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
        TextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name") })
        TextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") })

        Button(onClick = {
            errorMessage = ""
            // Validate fields before attempting signup
            if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                errorMessage = "All fields must be filled"
            } else if (userType == null) {
                errorMessage = "Please select a user type"
            } else {
                //userType!!.name passes the enum value as a string
                viewModel.signupWithEmail(email, password, firstName, lastName,
                    userType!!.name, selectedSubjects.toList(), selectedGradeLevels.toList(),
                    onSuccess = {
                        if (userType == UserType.Tutor) {
                            navController.navigate("uploadCertification")
                        } else {
                            navController.navigate("checkEmail")
                        }
                    },
                    onError = { errorMessage = it }
                )
            }
        }) {
            Text("Sign Up")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red)
        }
    }
}

// Enum to represent user types
enum class UserType {
    Student,
    Tutor
}