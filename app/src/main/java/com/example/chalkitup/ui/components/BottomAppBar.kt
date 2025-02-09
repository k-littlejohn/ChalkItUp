package com.example.chalkitup.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController

// Bottom App Bar

@Composable
fun BottomNavigationBar(navController: NavController) {

    // List of the items that are on the Bottom Bar
    val items = listOf(
        BottomNavItem("home", Icons.Default.Home, "Home"),
        BottomNavItem("booking", Icons.Default.Add, "Book"),
        BottomNavItem("messages", Icons.Default.Face, "Messages"),
        BottomNavItem("profile", Icons.Default.Person, "Profile")
    )

    // Holds the value of the current screen the user is on
    val currentRoute = navController.currentDestination?.route

    // Populate the bottom bar with the list of items
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

// Data class for navigation items
data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)
