package com.example.chalkitup.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.chalkitup.ui.components.MyTopBar
import com.example.chalkitup.ui.components.NavigationDrawer
import com.example.chalkitup.ui.nav.NavGraph
import kotlinx.coroutines.launch

// Main Screen acts as a base for other screens to be loaded into
// - All screens loaded here will have a top app bar
// - call NavGraph to navigate through screens

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerContent = { NavigationDrawer(navController, drawerState) },
        drawerState = drawerState
    ) {
        Scaffold(
            topBar = {
                MyTopBar(
                    navController = navController,
                    onMenuClick = {
                        coroutineScope.launch { drawerState.open() }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                NavGraph(navController)  // Loads screens inside this scaffold
            }
        }
    }
}