package com.example.chalkitup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.chalkitup.ui.screens.MainScreen
import com.example.chalkitup.ui.theme.ChalkitupTheme

// Initializes app on launch
// -> launches MainScreen()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChalkitupTheme { // Color Theme
                Surface {
                    MainScreen()
                }
            }
        }
    }
}