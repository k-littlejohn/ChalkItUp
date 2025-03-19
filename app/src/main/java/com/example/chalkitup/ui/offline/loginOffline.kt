package com.example.chalkitup.ui.offline

import android.content.Context
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
import com.example.chalkitup.ui.screens.GoogleSignInScreen
import androidx.compose.ui.platform.LocalContext
import com.example.chalkitup.Connection
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.io.File

//import kotlinx.coroutines.flow.internal.NoOpContinuation.context
//import kotlin.coroutines.jvm.internal.CompletedContinuation.context

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
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    // Get network status from the Connection singleton
    val context = LocalContext.current
    val isConnected = Connection.getInstance(context).isConnected
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
                        if (isConnected) {
                            errorMessage = ""
                            if (email.isEmpty() || password.isEmpty()) {
                                errorMessage = "Email and password cannot be empty"
                            } else {
                                viewModel.loginWithEmail(
                                    context, email, password,
                                    onSuccess = { navController.navigate("home") },
                                    onEmailError = { navController.navigate("checkEmail/verify") },
                                    onTermsError = { navController.navigate("termsAndCond") },
                                    onError = { errorMessage = it },
                                    awaitingApproval = { navController.navigate("awaitingApproval") },
                                    isAdmin = { navController.navigate("adminHome") }
                                )
                            }
                        }
                        else {
                            //go to user auth databse manager and check previous authentication
                            errorMessage = ""
                            if (email.isEmpty() || password.isEmpty()) {
                                errorMessage = "Email and password cannot be empty"
                            } else {
                                OfflineDataManager.offlineLoginWithEmail(
                                    context,
                                    email,
                                    password,
                                    onSuccess = { navController.navigate("home") },
                                    onEmailError = { navController.navigate("checkEmail/verify") },
                                    onTermsError = { navController.navigate("termsAndCond") },
                                    onError = { errorMessage = it },
                                    awaitingApproval = { navController.navigate("awaitingApproval") },
                                    isAdmin = { navController.navigate("adminHome") },
                                )
                            }
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
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
                    Text(
                        text = " or ",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Google Sign-In Button
                GoogleSignInScreen()
                //Spacer(modifier = Modifier.height(16.dp)) // Keep if we are placing other logins

            }
        }
    }
}


object OfflineDataManager {
    private const val FILE_NAME = "user_data.json"

    fun logUser(context: Context, username: String, password: String, status: String, userType: String) {
        val userData = JSONObject().apply {
            put("username", username)
            put("password", password)
            put("status", status)
            put("type", userType)
        }
        writeToFile(context, userData.toString())
    }

    fun changeStatus(context: Context, newStatus: String) {
        val userData = readFromFile(context) ?: return
        val json = JSONObject(userData)
        json.put("status", newStatus)
        writeToFile(context, json.toString())
    }

    fun checkOfflineLogin(context: Context, username: String, password: String): String? {
        val userData = readFromFile(context) ?: return null
        val json = JSONObject(userData)
        return if (json.getString("username") == username && json.getString("password") == password) {
            json.getString("status")
        } else {
            null
        }
    }
    fun checkUserType(context: Context, username: String, password: String): String? {
        val userData = readFromFile(context) ?: return null
        val json = JSONObject(userData)
        return if (json.getString("username") == username && json.getString("password") == password) {
            json.optString("type", "user")
        } else {
            null
        }
    }
    fun offlineLoginWithEmail(
        context: Context,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onEmailError: () -> Unit,
        onTermsError: () -> Unit,
        onError: (String) -> Unit,
        awaitingApproval: () -> Unit,
        isAdmin: () -> Unit
    ) {
        val status = checkOfflineLogin(context, email, password)
        when (status) {
            "true" -> onSuccess()
            "need_email" -> onEmailError()
            "need_approval" -> awaitingApproval()

            else -> onError("Invalid credentials or no offline data available")
        }
        val userType = checkUserType(context, email, password)
        when(userType){
            "admin" -> isAdmin()

        }
    }

    private fun writeToFile(context: Context, data: String) {
        val file = File(context.filesDir, FILE_NAME)
        file.writeText(data)
    }

    private fun readFromFile(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        return if (file.exists()) file.readText() else null
    }

}