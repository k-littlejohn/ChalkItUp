package com.example.chalkitup.ui.nav

import ForgotPasswordScreen
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.chalkitup.ui.screens.BookingScreen
import com.example.chalkitup.ui.screens.CheckEmailScreen
import com.example.chalkitup.ui.screens.EditProfileScreen
import com.example.chalkitup.ui.screens.HomeScreen
import com.example.chalkitup.ui.screens.LoginScreen
import com.example.chalkitup.ui.screens.MessagesScreen
import com.example.chalkitup.ui.screens.AskQuestionScreen
import com.example.chalkitup.ui.screens.EnterTutorAvailability
import com.example.chalkitup.ui.screens.ProfileScreen
import com.example.chalkitup.ui.screens.SettingsScreen
import com.example.chalkitup.ui.screens.SignupScreen
import com.example.chalkitup.ui.screens.StartScreen
import com.example.chalkitup.ui.viewmodel.AuthViewModel
import com.example.chalkitup.ui.viewmodel.BookingViewModel
import com.example.chalkitup.ui.viewmodel.CertificationViewModel
import com.example.chalkitup.ui.viewmodel.EditProfileViewModel
import com.example.chalkitup.ui.viewmodel.ProfileViewModel
import com.example.chalkitup.ui.viewmodel.SettingsViewModel
import com.example.chalkitup.ui.viewmodel.TutorAvailabilityViewModel
import com.google.firebase.auth.FirebaseAuth

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
                navController = navController,
                viewModel = authViewModel
            )
        }

        // Signup Screen
        composable("signup") {
            val authViewModel: AuthViewModel = viewModel()
            val certificationViewModel: CertificationViewModel = viewModel()
            SignupScreen(
                navController = navController,
                certificationViewModel = certificationViewModel,
                authViewModel = authViewModel
            )
        }

        // Home Screen
        composable("home") {
            HomeScreen(navController = navController)
        }

        // Booking Screen
        composable("booking") {
            val viewModel: BookingViewModel = viewModel()
            BookingScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        // Messages Screen
        composable("messages") {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                MessagesScreen(
                    navController = navController,
                    userId = user.uid
                )
            }
        }

        // Ask Question Screen
        composable("askQuestion") {
            AskQuestionScreen(navController = navController)
        }

        // Profile Screen
        composable("profile") {
            val certificationViewModel: CertificationViewModel = viewModel()
            val profileViewModel: ProfileViewModel = viewModel()
            ProfileScreen(
                navController = navController,
                certificationViewModel = certificationViewModel,
                profileViewModel = profileViewModel)
        }

        // Edit Profile Screen
        composable("editProfile") {
            val certificationViewModel: CertificationViewModel = viewModel()
            val editProfileViewModel: EditProfileViewModel = viewModel()
            EditProfileScreen(
                navController = navController,
                editProfileViewModel = editProfileViewModel,
                certificationViewModel = certificationViewModel)
        }

        // Settings Screen
        composable("settings") {
            val settingsViewModel: SettingsViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()
            SettingsScreen(
                navController = navController,
                settingsViewModel = settingsViewModel,
                authViewModel = authViewModel
            )
        }

        // Check Email Screen
        composable("checkEmail/{checkType}") { backStackEntry ->
            val checkType = backStackEntry.arguments?.getString("checkType") ?: "verify"
            val authViewModel: AuthViewModel = viewModel()
            CheckEmailScreen(
                navController = navController,
                checkType = checkType,
                viewModel = authViewModel
            )
        }

        // Forgot Password Screen
        composable("forgotPassword") {
            val authViewModel: AuthViewModel = viewModel()
            ForgotPasswordScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }

        // Tutor Availability Screen
        composable("tutorAvailability") {
            val viewmodel: TutorAvailabilityViewModel = viewModel()
            EnterTutorAvailability(
                navController = navController,
                viewModel = viewmodel
            )
        }

    }
}