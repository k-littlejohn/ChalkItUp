package com.example.chalkitup.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.chalkitup.ui.components.BottomNavigationBar
import com.example.chalkitup.ui.components.MyTopBar
import com.example.chalkitup.ui.components.NavigationDrawer
import com.example.chalkitup.ui.nav.NavGraph
import com.example.chalkitup.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

// Main Screen acts as a base for other screens to be loaded into
// - ex. all screens loaded here will have a top app bar
// - call NavGraph to navigate through screens

/**
 * Composable function for the main screen of the app.
 *
 * This screen serves as the entry point for the app, managing navigation, UI layout,
 * and conditional visibility of top and bottom bars based on the current route.
 * It also observes the user's authentication state and navigates to the home screen if the user is logged in.
 */
@Composable
fun MainScreen() {
    //------------------------------VARIABLES----------------------------------------------

    // Initialize navigation controller for managing screen navigation.
    val navController = rememberNavController()

    // State for managing the drawer's open/closed state.
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Coroutine scope for launching drawer animations.
    val coroutineScope = rememberCoroutineScope()

    // ViewModel for managing authentication-related data.
    val authViewModel: AuthViewModel = viewModel()

    // State for tracking the current route to conditionally hide/show bars.
    var currentRoute by remember { mutableStateOf<String?>(null) }

    // Observe route changes and update the current route when navigation destination changes.
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentRoute = destination.route?.substringBefore("/")
        }
    }

    // List of routes where the bottom bar should be hidden.
    val hideBottomBarRoutes = listOf("start","login", "signup","forgotPassword")
    // Determine whether to show the bottom bar based on the current route.
    val showBottomBar = currentRoute !in hideBottomBarRoutes

//    // List of routes where the top bar should be hidden.
//    val hideTopBarRoutes = listOf("start","login","signup","checkEmail","forgotPassword")
//    // Determine whether to show the top bar based on the current route.
//    val showTopBar = currentRoute !in hideTopBarRoutes

    // Observe the user's authentication state to determine whether they are logged in.
    val isUserLoggedIn by authViewModel.isUserLoggedIn.observeAsState(initial = false)

    // Send the user to the home screen if they are already logged in
    LaunchedEffect(isUserLoggedIn) {
        if (isUserLoggedIn) {
            navController.navigate("home") // Navigate to the home screen
        }
    }

    //------------------------------VARIABLES-END---------------------------------------------

    // Modal navigation drawer that wraps the content of the screen.
    ModalNavigationDrawer(
        drawerContent = { NavigationDrawer(navController, drawerState) }, // Drawer content (menu)
        drawerState = drawerState, // Manage drawer state (open/closed)
        gesturesEnabled = false // Disable swipe gestures to open the drawer (if enabled, drawer is accessible from any screen)
    ) {
        Scaffold(
            topBar = {
                // Conditionally show the top bar based on the current route.
                if (true) { // Top bar is used to camouflage screens it's not deliberately shown on (currently no screens it is fully hidden)
                    MyTopBar(
                        navController = navController,
                        onMenuClick = {
                            // Open the drawer when the menu button is clicked
                            coroutineScope.launch { drawerState.open() }
                        }
                    )
                }
            },
            bottomBar = {
                // Conditionally show the bottom navigation bar based on the current route.
                if (showBottomBar) {
                    BottomNavigationBar(navController) // Display the bottom navigation bar
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                // Display the screens based on the navigation graph.
                NavGraph(navController)
            }
        }
    }
}