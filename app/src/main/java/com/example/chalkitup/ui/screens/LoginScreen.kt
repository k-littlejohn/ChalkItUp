package com.example.chalkitup.ui.screens

//import androidx.compose.foundation.layout.windowInsetsPadding // newly added
// import androidx.compose.foundation.layout.WindowInsets // newly added
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.layout.BoxScopeInstance.matchParentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.chalkitup.ui.viewmodel.AuthViewModel
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.chalkitup.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.ui.layout.matchParentSize



val AtkinsonFont = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_light, FontWeight.Light),
    Font(R.font.atkinson_bold, FontWeight.Bold),
    Font(R.font.atkinson_extrabold, FontWeight.ExtraBold)
)

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    navController: NavController
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Gradient Background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF54A4FF), // 5% Blue
            Color.White, Color.White, Color.White, Color.White // 95% White
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Background Gradient
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(gradientBrush)
        )

        // Foreground UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Back Button
            IconButton(
                onClick = { navController.navigate("start") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome Back",
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    text = "Login To Your Account",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Forgot Password
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = Color.Blue,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { navController.navigate("forgotPassword") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Login Button
                Button(
                    onClick = {
                        errorMessage = ""
                        if (email.isEmpty() || password.isEmpty()) {
                            errorMessage = "Email and password cannot be empty"
                        } else {
                            viewModel.loginWithEmail(
                                email, password,
                                onSuccess = { navController.navigate("home") },
                                onEmailError = { navController.navigate("checkEmail/verify") },
                                onError = { errorMessage = it }
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06C59C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(text = "LOGIN", color = Color.White, fontSize = 16.sp)
                }

                // Display error messages
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage, color = Color.Red, fontSize = 14.sp)
                }

                // Divider
                Spacer(modifier = Modifier.height(26.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
                    Text(
                        text = " or sign in with ",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
