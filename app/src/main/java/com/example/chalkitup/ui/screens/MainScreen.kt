package com.example.chalkitup.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.chalkitup.ui.components.BottomNavigationBar
import com.example.chalkitup.ui.components.MyTopBar
import com.example.chalkitup.ui.components.NavigationDrawer
import com.example.chalkitup.ui.nav.NavGraph
import kotlinx.coroutines.launch

// Main Screen acts as a base for other screens to be loaded into
// - ex. all screens loaded here will have a top app bar
// - call NavGraph to navigate through screens

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // hide bottom bar in start, login, and signup screens
    var currentRoute by remember { mutableStateOf<String?>(null) }

    // Observe route changes
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentRoute = destination.route
        }
    }

    val hideBottomBarRoutes = listOf("start","login", "signup","checkEmail","uploadCertification")
    val showBottomBar = currentRoute !in hideBottomBarRoutes

    val hideTopBarRoutes = listOf("start","login","signup","checkEmail","uploadCertification")
    val showTopBar = currentRoute !in hideTopBarRoutes

    ModalNavigationDrawer(
        drawerContent = { NavigationDrawer(navController, drawerState) },
        drawerState = drawerState,
        gesturesEnabled = false
    ) {
        Scaffold(
            topBar = {
                if (showTopBar) {
                    MyTopBar(
                        navController = navController,
                        onMenuClick = {
                            coroutineScope.launch { drawerState.open() }
                        }
                    )
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    BottomNavigationBar(navController)
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                NavGraph(navController)  // Loads screens inside this scaffold
            }
        }
    }
}