package com.example.chalkitup.ui.components

import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

// Top app bar UI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar(
    navController: NavController,
    onMenuClick: () -> Unit
) {
    // Holds the value of the current screen the user is on
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Use theme colors dynamically
    // Changes the background colour of the top bar based on what
    // screen the user is on
    val backgroundColor = when (currentRoute) {
        "home" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    // Change text dynamically
    // Changes the text at the top based on what screen the user
    // is on
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = when (currentRoute) {
                    "home" -> "Home"
                    "profile" -> "Profile"
                    "settings" -> "Settings"
                    "booking" -> "Book a Session"
                    "messages" -> "Messages"
                    else -> "Chalk It Up"
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
                // On the login and signup page currently,
                // there is a back button in the top left
                "login","signup" -> {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            }
        }
    )
}

