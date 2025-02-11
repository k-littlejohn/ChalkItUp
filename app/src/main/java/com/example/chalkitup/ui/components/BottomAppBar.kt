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

    // List of the items that are displayed on the Bottom Bar
    val items = listOf(
        BottomNavItem("home", Icons.Default.Home, "Home"),     // Home icon with label "Home"
        BottomNavItem("booking", Icons.Default.Add, "Book"),   // Book icon with label "Book"
        BottomNavItem("messages", Icons.Default.Face, "Messages"), // Messages icon with label "Messages"
        BottomNavItem("profile", Icons.Default.Person, "Profile")  // Profile icon with label "Profile"
    )

    // Get the current route from the NavController to determine which item is selected
    val currentRoute = navController.currentDestination?.route

    // Create the Bottom Navigation Bar
    NavigationBar {
        // Iterate over the list of items and create a NavigationBarItem for each
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) }, // Set the icon for the item
                label = { Text(item.label) }, // Set the label for the item
                selected = currentRoute == item.route, // Highlight item if it's the current route
                onClick = {
                    // Navigate to the corresponding screen when the item is clicked
                    navController.navigate(item.route) {
                        launchSingleTop = true // Only one instance of this screen is launched
                        restoreState = true // Restore the state of the screen when navigating back
                    }
                }
            )
        }
    }
}

// Data class to represent each item in the Bottom Navigation Bar
data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)
