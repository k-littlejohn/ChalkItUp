package com.example.chalkitup.ui.nav

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.chalkitup.ui.screens.BookingScreen
import com.example.chalkitup.ui.screens.HomeScreen
import com.example.chalkitup.ui.screens.LoginScreen
import com.example.chalkitup.ui.screens.MessagesScreen
import com.example.chalkitup.ui.screens.ProfileScreen
import com.example.chalkitup.ui.screens.SettingsScreen
import com.example.chalkitup.ui.screens.SignupScreen
import com.example.chalkitup.ui.screens.StartScreen
import com.example.chalkitup.ui.viewmodel.AuthViewModel

// Navigation Center, NavHost with navController
// On app launch, opens startScreen

@Composable
fun NavGraph(navController: NavHostController) {

    NavHost(navController = navController, startDestination = "start") {

        // Start Screen
        composable("start") {
            StartScreen(navController = navController)
        }

        // Login Screen
        composable("login") {
            val authViewModel: AuthViewModel = viewModel()
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // Signup Screen
        composable("signup") {
            val authViewModel: AuthViewModel = viewModel()
            SignupScreen(
                viewModel = authViewModel,
                onSignupSuccess = {
                    navController.navigate("home") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            )
        }

        // Home Screen
        composable("home") {
            HomeScreen(navController = navController)
        }

        // Booking Screen
        composable("booking") {
            BookingScreen(navController = navController)
        }

        // Messages Screen
        composable("messages") {
            MessagesScreen(navController = navController)
        }

        // Profile Screen
        composable("profile") {
            ProfileScreen(navController = navController)
        }

        // Settings Screen
        composable("settings") {
            SettingsScreen(navController = navController)
        }

    }
}