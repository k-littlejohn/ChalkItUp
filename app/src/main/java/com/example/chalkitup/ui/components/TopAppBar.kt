package com.example.chalkitup.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.res.painterResource
import com.example.chalkitup.R

// Top app bar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar(
    navController: NavController,
    onMenuClick: () -> Unit
) {
    // Holds the value of the current screen the user is on
    var currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    currentRoute = currentRoute?.substringBefore("/")

    // Use theme colors dynamically
    // Changes the background colour of the top bar based on what
    // screen the user is on
    val backgroundColor = when (currentRoute) {
        //"home" -> Color(0xFFFFFFFF)//MaterialTheme.colorScheme.primary
        "profile", "home", "start", "login", "signup", "forgotPassword" -> Color(0xFF54A4FF) // Fill top-screen white space
        "checkEmail" -> Color(0xFF06C59C) // Fill top-screen white space
        else -> Color.White
    }

    // Change text dynamically
    // Changes the text at the top based on what screen the user
    // is on
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = when (currentRoute) {
                    "home" -> ""
                    "profile" -> "Profile"
                    "settings" -> "Settings"
                    "booking" -> "Book a Session"
                    "messages" -> "Messages"
                    "newMessage" -> "New Chat"
                    "start", "login", "signup", "forgotPassword", "checkEmail" -> ""
                    else -> "ChalkItUp Tutors"
                }
            )
        },

        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = backgroundColor
        ),

        // Change LEFT button based on current screen
        navigationIcon = {
            when (currentRoute) {
                // On the home page currently,
                // there is a menu button in the top left that opens
                // the navigation drawer
                "home" -> {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
                // On the login and signup page currently,          // Here are the alternative back buttons on login & signup screen
                // there is a back button in the top left
                "login","signup" -> {
                    IconButton(onClick = { navController.navigate("start") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(30.dp))
                    }
                }
                // On unspecified pages there is no button in the top left
                else -> {
                    Unit
                }
            }
        },
        // Change RIGHT button based on current screen
        actions = {
            when (currentRoute) {
                // On the home page currently,
                // there is a settings button in the top right
                "home" -> {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
                "profile" -> {
                    IconButton(onClick = { navController.navigate("editProfile") }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "Edit Profile",
                            tint = Color(0xFF000080)
                        )
                    }
                }
            }
        }
    )
}

