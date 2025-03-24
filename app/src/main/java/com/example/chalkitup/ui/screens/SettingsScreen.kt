package com.example.chalkitup.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chalkitup.ui.viewmodel.Appointment
import com.example.chalkitup.ui.viewmodel.AuthViewModel
import com.example.chalkitup.ui.viewmodel.BookingManager
import com.example.chalkitup.ui.viewmodel.SettingsViewModel
import com.example.chalkitup.ui.viewmodel.OfflineDataManager
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    navController: NavController,
    offlineViewModel: OfflineDataManager,
    context: Context = LocalContext.current
) {
    var showDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val email = auth.currentUser?.email ?: ""
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // temp logout button
        /////////////////// subscription google link
        SubscribeToCalendarButton(appointments = BookingManager.readBookings())

        ////////////////////
        Button(onClick = {
            authViewModel.signout()
            navController.navigate("start")}
        ) {
            Text("Logout")
        }

        // Show alert dialog when "Delete Account" button clicked
        OutlinedButton(onClick = { showDialog = true })
        {
            Text("Delete Account")
        }

        if (showDialog) {
            AlertDialog(
                title = { Text(text = "Delete this account?") },
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    OutlinedButton(
                        onClick = {
                            settingsViewModel.deleteAccount(
                                onSuccess = {
                                    successMessage = "Account Deleted!"
                                    showDialog = false
                                    navController.navigate("start")
                                    offlineViewModel.removeUser(
                                        email
                                    )
                                },
                                onError = {
                                    errorMessage = "Failed to delete Account"
                                    showDialog = false
                                    navController.navigate("home")
                                }
                            )
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
    // Show toast message when account deleted
    LaunchedEffect(successMessage) {
        if (successMessage.isNotEmpty()) {
            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
        }
    }

    // Show toast message when account deletion failed
    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotEmpty()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }
}



@Composable
fun SubscribeToCalendarButton(appointments: List<Appointment>) {
    val context = LocalContext.current
    var showSuccessMessage by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf(false) }

    Button(
        onClick = {
            // Generate the calendar link and attempt to open it
            val isSuccess = generateCalendarSubscriptionLink(context, appointments)

            if (isSuccess) {
                showSuccessMessage = true
                showErrorMessage = false
            } else {
                showErrorMessage = true
                showSuccessMessage = false
            }
        }
    ) {
        Text("Subscribe to Calendar")
    }

    // Show a success message after subscription attempt
    if (showSuccessMessage) {
        AlertDialog(
            onDismissRequest = { showSuccessMessage = false },
            title = { Text("Subscription Success") },
            text = { Text("You have successfully subscribed to the calendar.") },
            confirmButton = {
                Button(onClick = { showSuccessMessage = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Show an error message if the subscription fails
    if (showErrorMessage) {
        AlertDialog(
            onDismissRequest = { showErrorMessage = false },
            title = { Text("Error") },
            text = { Text("There was an error subscribing to the calendar. Please try again later.") },
            confirmButton = {
                Button(onClick = { showErrorMessage = false }) {
                    Text("OK")
                }
            }
        )
    }
}

object GoogleCalendarHelper {

    // Base URL for adding events to Google Calendar
    private const val GOOGLE_CALENDAR_URL = "https://www.google.com/calendar/render?action=TEMPLATE"

    // Function to generate Google Calendar event URL
    fun createCalendarEventUrl(appointments: List<Appointment>): String {
        val calendarUrl = StringBuilder(GOOGLE_CALENDAR_URL)

        // Loop through each appointment and generate the URL for each
        appointments.forEach { appointment ->
            // Parse the appointment date and time
            val date = appointment.date // "2025-03-23"
            val timeRange = appointment.time // "3:00 PM - 4:00 PM"

            // Parse the start and end times from the "3:00 PM - 4:00 PM" format
            val timeParts = timeRange.split(" - ")
            val startTime = timeParts[0] // "3:00 PM"
            val endTime = timeParts[1] // "4:00 PM"

            // Combine date and time to form the start and end date-time strings
            val startDateTime = "$date $startTime"
            val endDateTime = "$date $endTime"

            // Format the start and end date-time to be in ISO 8601 format (e.g., "2025-03-23T15:00:00Z")
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
            val isoDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

            val startDateTimeFormatted = try {
                val startDate = dateTimeFormat.parse(startDateTime)
                startDate?.let { isoDateTimeFormat.format(it) }
            } catch (e: Exception) {
                // Handle parsing failure (e.g., if time format is incorrect)
                e.printStackTrace()
                startDateTime // fallback to original input if parsing fails
            }

            val endDateTimeFormatted = try {
                val endDate = dateTimeFormat.parse(endDateTime)
                endDate?.let { isoDateTimeFormat.format(it) }
            } catch (e: Exception) {
                // Handle parsing failure (e.g., if time format is incorrect)
                e.printStackTrace()
                endDateTime // fallback to original input if parsing fails
            }

            // Build URL parameters
            val params = mapOf(
                "text" to appointment.subject,  // Event subject
                "details" to appointment.comments,  // Event details
                "location" to appointment.mode,  // Event location
                "dates" to "$startDateTimeFormatted/$endDateTimeFormatted",  // Event start and end date/time
                //"sprop" to "https://www.example.com",  // Optional source URL, if needed
                //"sprop=name:ChalItUp"  // Optional name, could be your app's name or identifier
            )

            // Append the URL parameters to the base URL
            params.forEach { (key, value) ->
                calendarUrl.append("&$key=${Uri.encode(value)}")
            }
        }

        return calendarUrl.toString()
    }
    // Convert time string to ISO 8601 format (HH:mm)
    private fun convertTimeToIso8601(time: String, timeFormat: SimpleDateFormat): String {
        val date = timeFormat.parse(time) ?: return "00:00"
        val isoFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return isoFormat.format(date)
    }

    // Extract end time from the time range string
    private fun getEndTime(timeRange: String): String {
        val parts = timeRange.split(" - ")
        return if (parts.size == 2) parts[1] else parts[0] // Return the second time in the range
    }
}


fun generateCalendarSubscriptionLink(context: Context, appointments: List<Appointment>): Boolean {
    // Generate the URL to subscribe to the calendar
    val calendarUrl = GoogleCalendarHelper.createCalendarEventUrl(appointments)

    // Log the calendar URL for debugging
    Log.d("GoogleCalendar", "Calendar URL: $calendarUrl")

    // Example: Opening the generated URL in a browser or WebView
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(calendarUrl))

    return if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
        true
    } else {
        // Handle the case where no app can handle the intent (e.g., Google Calendar)
        Log.e("GoogleCalendar", "No app found to handle the calendar URL")
        false
    }
}


