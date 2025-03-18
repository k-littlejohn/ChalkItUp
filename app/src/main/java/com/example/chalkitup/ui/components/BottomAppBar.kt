package com.example.chalkitup.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material.icons.automirrored.filled.Message

// Bottom App Bar

@Composable
fun BottomNavigationBar(
    navController: NavController
) {

    // Temporary solution to get the user type
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var userType by remember { mutableStateOf<String?>(null) }

    // Fetch userType
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                userType = document.getString("userType")
            }
            .addOnFailureListener {
                userType = null
            }
    }

    val items = when (userType) {
        "Tutor" -> listOf(
            // List of the items that are displayed on the Bottom Bar for Tutors
            BottomNavItem("home", Icons.Default.Home, "Home"),         // Home icon with label "Home"
            BottomNavItem("tutorAvailability", Icons.Default.Add, "Availability"),       // Availability icon with label "Availability"
            BottomNavItem("messages", Icons.AutoMirrored.Filled.Message, "Messages"),// Messages icon with label "Messages"
            BottomNavItem("profile/", Icons.Default.Person, "Profile")  // Profile icon with label "Profile"
        )
        // List of the items that are displayed on the Bottom Bar for Students
        "Student" -> listOf(
            BottomNavItem("home", Icons.Default.Home, "Home"),         // Home icon with label "Home"
            BottomNavItem("booking", Icons.Default.Add, "Book"),       // Book icon with label "Book"
            BottomNavItem("messages", Icons.AutoMirrored.Filled.Message, "Messages"), // Messages icon with label "Messages"
            BottomNavItem("profile/", Icons.Default.Person, "Profile")  // Profile icon with label "Profile"
        )
        else -> emptyList()
    }

    // Get the current route from the NavController to determine which item is selected
    var currentRoute = navController.currentDestination?.route
    currentRoute = currentRoute?.substringBefore("/")

    val fillerBar = ("checkEmail" == currentRoute) || ("awaitingApproval" == currentRoute)

    // Create the Bottom Navigation Bar
    NavigationBar (
        containerColor = if (fillerBar) Color(0xFF54A4FF) else Color.LightGray,
        contentColor = if (fillerBar) Color(0xFF54A4FF) else Color.LightGray,
    ) {
        if (fillerBar) {
            Unit
        } else {
            // Iterate over the list of items and create a NavigationBarItem for each
            items.forEach { item ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            item.icon,
                            contentDescription = item.label
                        )
                    }, // Set the icon for the item
                    label = { Text(item.label) }, // Set the label for the item
                    selected = currentRoute == item.route, // Highlight item if it's the current route
                    onClick = {
                        // Navigate to the corresponding screen when the item is clicked
                        navController.navigate(item.route) {
                            launchSingleTop = true // Only one instance of this screen is launched
                            restoreState =
                                true // Restore the state of the screen when navigating back
                        }
                    }
                )
            }
        }
    }
}

// Data class to represent each item in the Bottom Navigation Bar
data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)
