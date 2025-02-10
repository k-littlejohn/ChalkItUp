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
// - top app bar buttons and text can be changed depending on the current screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar(
    navController: NavController,
    onMenuClick: () -> Unit
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Use theme colors dynamically
    val backgroundColor = when (currentRoute) {
        "home" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    CenterAlignedTopAppBar(
        title = {
            Text(
                text = when (currentRoute) {
                    "home" -> "Home"
                    "profile" -> "Profile"
                    "settings" -> "Settings"
                    "booking" -> "Book a Session"
                    "messages" -> "Messages"
                    else -> "ChalkItUp Tutors"
                }
            )
        },

        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = backgroundColor
        ),

        // Change buttons based on current screen
        navigationIcon = {
            when (currentRoute) {
                "home" -> {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
                "login","signup" -> {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                else -> {
                    Unit
                }
            }
        },
        actions = {
            when (currentRoute) {
                "home" -> {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }
        }
    )
}

