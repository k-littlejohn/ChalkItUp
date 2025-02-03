package com.example.chalkitup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

// Navigation drawer ui

@Composable
fun NavigationDrawer(navController: NavController, drawerState: DrawerState) {
    val coroutineScope = rememberCoroutineScope()

    ModalDrawerSheet (
        drawerContainerColor = MaterialTheme.colorScheme.primary, // Drawer background color
    ) {
        // Header Section
        DrawerHeader()

        Box(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Subjects Offered",
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.onBackground // Text color
                )

                Spacer(modifier = Modifier.height(16.dp))

                NavigationDrawerItem(
                    label = {
                        Text(
                            "Math",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("home")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Available Tutors",
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                NavigationDrawerItem(
                    label = {
                        Text(
                            "Billy bob",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("home")
                    }
                )

            }
        }
    }
}

@Composable
fun DrawerHeader() {

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
            .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Chalk It Up", fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
