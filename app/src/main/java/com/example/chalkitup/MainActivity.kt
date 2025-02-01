package com.example.chalkitup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chalkitup.ui.screens.HomeScreen
import com.example.chalkitup.ui.screens.LoginScreen
import com.example.chalkitup.ui.screens.SignupScreen
import com.example.chalkitup.ui.viewmodel.AuthViewModel

// Navigation Center, NavHost with navController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Set up NavController and NavHost
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "login") {
                // Login Screen
                composable("login") {
                    val authViewModel: AuthViewModel = viewModel() // Use viewModel()
                    LoginScreen(
                        viewModel = authViewModel,
                        onLoginSuccess = {
                            // Navigate to the Home screen after login
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        navController = navController // Pass the navController here
                    )
                }

                // Signup Screen
                composable("signup") {
                    val authViewModel: AuthViewModel = viewModel() // Use viewModel()
                    SignupScreen(
                        viewModel = authViewModel,
                        onSignupSuccess = {
                            // Navigate to the Home screen after signup
                            navController.navigate("home") {
                                popUpTo("signup") { inclusive = true }
                            }
                        },
                        navController = navController // Pass the navController here
                    )
                }

                // Home Screen (Main screen after login/signup)
                composable("home") {
                    HomeScreen()
                }

                // contd...
            }
        }
    }
}