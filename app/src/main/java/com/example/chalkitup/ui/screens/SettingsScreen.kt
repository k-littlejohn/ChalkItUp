package com.example.chalkitup.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chalkitup.ui.viewmodel.AuthViewModel
import com.example.chalkitup.ui.viewmodel.SettingsViewModel
import com.example.chalkitup.ui.viewmodel.OfflineDataManager
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    navController: NavController,
    offlineViewModel: OfflineDataManager,
    context: Context = LocalContext.current
) {
    var showDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val email = auth.currentUser?.email ?: ""
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // temp logout button
        Button(onClick = {
            authViewModel.signout()
            navController.navigate("start")}
        ) {
            Text("Logout")
        }

        // Show alert dialog when "Delete Account" button clicked
        OutlinedButton(onClick = { showDialog = true })
        {
            Text("Delete Account")
        }

        if (showDialog) {
            AlertDialog(
                title = { Text(text = "Delete this account?") },
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    OutlinedButton(
                        onClick = {
                            settingsViewModel.deleteAccount(
                                onSuccess = {
                                    successMessage = "Account Deleted!"
                                    showDialog = false
                                    navController.navigate("start")
                                    offlineViewModel.removeUser(
                                        email
                                    )
                                },
                                onError = {
                                    errorMessage = "Failed to delete Account"
                                    showDialog = false
                                    navController.navigate("home")
                                }
                            )
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
    // Show toast message when account deleted
    LaunchedEffect(successMessage) {
        if (successMessage.isNotEmpty()) {
            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
        }
    }

    // Show toast message when account deletion failed
    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotEmpty()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }
}