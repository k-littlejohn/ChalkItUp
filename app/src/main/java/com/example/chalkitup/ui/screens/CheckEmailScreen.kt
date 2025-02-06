package com.example.chalkitup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.example.chalkitup.ui.viewmodel.AuthViewModel

// UI for the check email screen

@Composable
fun CheckEmailScreen(
    viewModel: AuthViewModel,
    navController: NavController) {

    var successMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }


    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Check your email")
        Text("We sent you a verification link!")

        Text("Didn't receive an email?")
        TextButton(onClick = {
            successMessage = ""
            errorMessage = ""
            viewModel.resendVerificationEmail(
                onSuccess = { successMessage = it },
                onError = { errorMessage = it }
            )
        }
        ) {
            Text("Send another email")
        }

        Button(onClick = {
            // signout the user bc they are not verified!
            viewModel.signout()
            navController.navigate("start")
        }) {
            Text("Done")
        }


        if (successMessage.isNotEmpty()) {
            Text(successMessage)
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red)
        }

    }
}