package com.example.chalkitup.ui.screens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.chalkitup.R
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ButtonDefaults
import com.example.chalkitup.ui.theme.*




// Title, then description.
data class TextEntry(val title: String, val description: String)

@Composable
fun StartScreen(navController: NavController) {
    var currentImageIndex by remember { mutableStateOf(0) }

    val images = listOf(
        R.drawable.login1,
        R.drawable.login2,
        R.drawable.login3,
        R.drawable.login4
    )

    val textEntries = listOf(
        TextEntry("Choose Your Learning Style", "1-on-1 tutoring sessions (online or in-person)"),
        TextEntry("Flexible Scheduling", "Choose dates and times that fit your schedule"),
        TextEntry("Stay Connected", "Communicate effortlessly with your tutor"),
        TextEntry("Track Your Progress", "Upload grades, track lessons, view achievements")
    )

    // Auto-Rotate the Images & Text On 4second Delay
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000L)
            currentImageIndex = (currentImageIndex + 1) % images.size
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        // Image & Background To Image (circle)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.image_background),
                contentDescription = "Background",
                modifier = Modifier.size(340.dp)
            )
            Image(
                painter = painterResource(id = images[currentImageIndex]),
                contentDescription = "Rotating Image",
                modifier = Modifier.size(310.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Dynamic Changing Text With Image
        Text(
            text = textEntries[currentImageIndex].title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = textEntries[currentImageIndex].description,
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Progress Indicator Bar
        IndicatorBar(currentIndex = currentImageIndex) { index ->
            currentImageIndex = index
        }
        // Moves login/signup Down The Higher dp
        Spacer(modifier = Modifier.height(70.dp))

        // Login Button & Text
        Button(
            onClick = { navController.navigate("login") },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TealGreen,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(0.dp)

        ) {
            Text(
                text = "LOGIN",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

// Signup Text
        Row {
            Text(
                text = "Don't have an account? ",
                color = Color.Black, //
                fontSize = 14.sp
            )
            Text(
                text = "Sign Up",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { navController.navigate("signup") }
            )
        }

    }
}

// Progress Indicator Bar, Added Clickable Function
@Composable
fun IndicatorBar(currentIndex: Int, onIndicatorClick: (Int) -> Unit) {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        for (i in 0..3) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .clickable { onIndicatorClick(i) }
                    .background(if (i == currentIndex) Color.DarkGray else Color.LightGray)
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}
