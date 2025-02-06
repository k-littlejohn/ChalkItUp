package com.example.chalkitup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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

// UI of login screen

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    navController: NavController
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {

        Text("Login")
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())

        Button(onClick = {
            errorMessage = ""
            // Validate fields before attempting login
            if (email.isEmpty() || password.isEmpty()) {
                errorMessage = "Email and password cannot be empty"
            } else {
                viewModel.loginWithEmail(email, password,
                    onSuccess = { navController.navigate("home") },
                    onEmailError = { navController.navigate("checkEmail") },
                    onError = { errorMessage = it })
            }
        }) {
            Text("Login")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red)
        }
    }
}
