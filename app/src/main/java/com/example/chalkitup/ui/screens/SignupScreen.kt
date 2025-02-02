package com.example.chalkitup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    onSignupSuccess: () -> Unit,
    navController: NavController
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf<UserType?>(null) } // Track user type: Student or Tutor

    Column(modifier = Modifier.padding(16.dp).fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {

        Text("Sign Up")

        // User type selection (Student / Tutor)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { userType = UserType.Student },
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
                viewModel.signupWithEmail(email, password, firstName, lastName, userType!!.name, onSignupSuccess) { errorMessage = it }
            }
        }) {
            Text("Sign Up")
        }

        // Navigate to login screen
        TextButton(onClick = { navController.navigate("login") }) {
            Text("Already have an account? Login")
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