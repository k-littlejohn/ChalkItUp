package com.example.chalkitup.ui.offline

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.chalkitup.ui.screens.MainScreen
import com.example.chalkitup.ui.theme.ChalkitupTheme
import com.google.firebase.auth.FirebaseAuth
import com.example.chalkitup.ui.viewmodel.UserProfile
import com.google.android.libraries.places.api.Places
import com.google.firebase.Firebase
import com.google.firebase.initialize

// Initializes app on launch
// -> launches MainScreen()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase and other services
        Firebase.initialize(this)
        Places.initialize(this, "AIzaSyCp6eJq4S6fiAbSb-yOaiGfmZc1imPAxAM")

        Connection(this).isConnected

        val secureStorage = SecureStorage(this)
        val lastUser = secureStorage.getUser()

        setContent {
            ChalkitupTheme {
                Surface {
                    CheckAuthStatus(this, lastUser = lastUser)
                }
            }
        }
    }
}

@Composable
fun CheckAuthStatus(context: Context, lastUser: UserProfile?) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // Get network status from the Connection singleton
    val isConnected = Connection.getInstance(context).isConnected

    // Create a state to hold the loaded user profile
    var authUser by remember { mutableStateOf<UserProfile?>(null) }

    // If online and user exists, load user profile
    LaunchedEffect(currentUser, isConnected) {
        if (isConnected && currentUser != null) {
            UserProfile.fromUser(currentUser) { loadedUser ->
                authUser = loadedUser
            }
        }
    }

    // Handle UI based on authentication & connection status
    when {
        isConnected && authUser != null -> MainScreen()
        isConnected && currentUser == null -> MainScreen()
        !isConnected && lastUser != null -> MainScreen()
        else -> MainScreen()
    }
}
