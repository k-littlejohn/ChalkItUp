package com.example.chalkitup.ui.screens

//import androidx.privacysandbox.tools.core.generator.build
//import com.google.api.client.json.GsonFactory
//first create a service account for google
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import android.content.ClipboardManager
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.NavController
import com.example.chalkitup.ui.viewmodel.AuthViewModel
import com.example.chalkitup.ui.viewmodel.BookingManager
import com.example.chalkitup.ui.viewmodel.OfflineDataManager
import com.example.chalkitup.ui.viewmodel.SettingsViewModel
import com.example.chalkitup.ui.viewmodel.UserProfile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestFactory
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.AclRule
import com.google.api.services.calendar.model.AclRule.Scope
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import com.google.api.services.calendar.model.Calendar as GoogleCalendar
import java.util.Calendar as Cal


@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    navController: NavController,
    offlineViewModel: OfflineDataManager,
) {
    val context = LocalContext.current
    val account = GoogleSignIn.getLastSignedInAccount(context)

    var showDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val email = auth.currentUser?.email ?: ""
    //var userProfile by remember { mutableStateOf(UserProfile()) }
    //val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
    val calendarId = remember { CalendarDetailsSingleton.calendarId }
    LaunchedEffect(Unit) {
        CalendarDetailsSingleton.ensureCalendarExists()
    }

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        SubscriptionButton(context)

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

//----------------------- google calender API
fun authenticateServiceAccount() {
    try {
        // Path to the service account key file
        val serviceAccountKeyFile = "/Users/samanthaskiba/StudioProjects/W25_D1/app/src/main/java/com/example/chalkitup/ui/viewmodel/admin/chalkitup-bdceba61ebce.json"
        // Load the service account credentials
        val credentials = ServiceAccountCredentials.fromStream(FileInputStream(serviceAccountKeyFile))
        // Create the HTTP transport
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        // Use GsonFactory for JSON parsing
        val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()
        // Now you can use 'credentials' to authenticate API requests
        val requestFactory: HttpRequestFactory = httpTransport.createRequestFactory { credentials }
        // Example API endpoint (replace with your actual endpoint)
        val url = GenericUrl("https://www.googleapis.com/calendar/v3/calendars/primary/events")
        // Build the request
        val request: HttpRequest = requestFactory.buildGetRequest(url)
        // Execute the request
        val response = request.execute()
        println("Response: ${response.content}")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
//add the service account to the calendar
fun createCalendarAndAddServiceAccount() {
    try {
        val serviceAccountKeyFile = "/Users/samanthaskiba/StudioProjects/W25_D1/app/src/main/java/com/example/chalkitup/ui/viewmodel/admin/chalkitup-bdceba61ebce.json"
        val credentials = ServiceAccountCredentials.fromStream(FileInputStream(serviceAccountKeyFile))
        val googleCredential = GoogleCredential.fromStream(FileInputStream(serviceAccountKeyFile))
            .createScoped(listOf(CalendarScopes.CALENDAR))

        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()

        val calendarService = Calendar.Builder(httpTransport, jsonFactory, googleCredential)
            .setApplicationName("ChalkItUp")
            .build()

        val calendar = GoogleCalendar()
        calendar.summary = "ChalkItUp Calendar"
        calendar.timeZone = "America/Edmonton"

        val createdCalendar = calendarService.calendars().insert(calendar).execute()
        val calendarId = createdCalendar.id

        // Store calendar ID in Singleton
        CalendarDetailsSingleton.calendarId = calendarId

        println("Created and stored Calendar ID: $calendarId")

        // Grant access to the service account
        addServiceAccountToCalendar(calendarService, calendarId, credentials)

    } catch (e: Exception) {
        e.printStackTrace()
    }
}


fun addServiceAccountToCalendar(calendarService: Calendar, calendarId: String, credentials: ServiceAccountCredentials) {
    try {
        // Define the service account email (you can get this from the credentials)
        val serviceAccountEmail = credentials.clientEmail

        // Create the ACL rule for the service account
        val aclRule = AclRule()
        aclRule.scope = Scope().setType("user").setValue(serviceAccountEmail)
        aclRule.role = "owner"  // You can set the role to "reader" or "writer" as per your needs

        // Insert the ACL rule to give permissions to the service account
        calendarService.acl().insert(calendarId, aclRule).execute()

        println("Service account $serviceAccountEmail has been added to the calendar with owner permissions.")

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Data class to store the calendar ID
object CalendarDetailsSingleton {
    var calendarId: String = ""

    fun ensureCalendarExists() {
        if (calendarId.isEmpty()) {
            println("No calendar found. Creating a new one...")
            createCalendarAndAddServiceAccount()
        } else {
            println("Calendar already exists: $calendarId")
        }
    }
}

fun addAppointmentsToCalendar(calendarId: String) {
    try {
        val calendarService = getCalendarService()  // Get the Google Calendar service

        // Retrieve the list of appointments from the JSON file using BookingManager
        val appointments = BookingManager.readBookings()

        // Iterate through the appointments and create events for each one
        for (appointment in appointments) {
            // Define the event details
            val event = Event()
                .setSummary("Appointment with ${appointment.studentName}")
                .setDescription("Subject: ${appointment.subject}\nMode: ${appointment.mode}\nComments: ${appointment.comments}")
                .setLocation("Online or location TBD")

            // Set start and end times for the event
            val startDateTime = Cal.getInstance()
            val startParts = appointment.date.split("-")
            val timeParts = appointment.time.split(":")
            startDateTime.set(
                startParts[0].toInt(),
                startParts[1].toInt() - 1,  // Calendar month is 0-based, so subtract 1
                startParts[2].toInt(),
                timeParts[0].toInt(),
                timeParts[1].toInt()
            )
            val start = EventDateTime()
                .setDateTime(DateTime(startDateTime.time))
                .setTimeZone("America/Edmonton")  // Set your desired timezone

            val endDateTime = Cal.getInstance()
            endDateTime.set(
                startParts[0].toInt(),
                startParts[1].toInt() - 1,
                startParts[2].toInt(),
                timeParts[0].toInt(),
                timeParts[1].toInt() + 30  // 30-minute duration for the event
            )
            val end = EventDateTime()
                .setDateTime(DateTime(endDateTime.time))
                .setTimeZone("America/Edmonton")  // Set your desired timezone

            event.setStart(start)
                .setEnd(end)

            // Insert the event into the calendar
            val createdEvent = calendarService.events().insert(calendarId, event).execute()

            // Print out the event details
            println("Event created: ${createdEvent.htmlLink}")
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun getCalendarService(): Calendar {
    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()

    // Load credentials from the service account key file
    val credentials = GoogleCredentials.fromStream(FileInputStream("path_to_service_account_key.json"))
        .createScoped(CalendarScopes.CALENDAR)

    // Convert GoogleCredentials to HttpRequestInitializer
    val requestInitializer = credentials as com.google.api.client.http.HttpRequestInitializer

    // Initialize the Calendar service
    return Calendar.Builder(httpTransport, jsonFactory, requestInitializer)
        .setApplicationName("ChalkItUp")
        .build()
}
fun getCalendarSubscriptionLink(calendarId: String): String {
    try {
        val calendarService = getCalendarService() // Assume this is your existing function to get the Calendar service
        val calendar = calendarService.calendars().get(calendarId).execute()

        // Retrieve the public iCal URL from the calendar details
        val icalLink = calendar.get("iCalUID") as String
        println("iCalendar Link: $icalLink")

        return icalLink
    } catch (e: Exception) {
        e.printStackTrace()
        return ""
    }
}


class CalendarHelper(private val mService: Calendar) {
    @Throws(IOException::class)
    fun makeCalendarPublic(calendarId: String?) {
        // Create an ACL rule to make the calendar public
        val rule = AclRule()
        rule.setRole("reader") // Public will only have read access
        rule.setScope(Scope().setType("default"))

        // Apply the rule to the calendar
        try {
            val createdRule = mService.acl().insert(calendarId, rule).execute()
            println("Calendar made public: " + createdRule.id)
        } catch (e: IOException) {
            println("Error making calendar public: " + e.message)
            throw e
        }
    }
    fun generateSubscriptionLink(calendarId: String): String {
        // Format the subscription link as per Google Calendar's iCal format
        val subscriptionUrl =
            "https://calendar.google.com/calendar/ical/$calendarId/public/basic.ics"

        return subscriptionUrl
    }
}

//create a subscription button that copies subscription link and opens google calender subscriptions
@Composable
fun SubscriptionButton(context: Context) {
    val coroutineScope = rememberCoroutineScope()

    Button(onClick = {
        coroutineScope.launch {
            // Ensure a calendar exists
            CalendarDetailsSingleton.ensureCalendarExists()

            // Wait a bit to allow async operations to complete (not ideal, better to use proper async flow)
            delay(2000)

            val calendarId = CalendarDetailsSingleton.calendarId
            if (calendarId.isNotEmpty()) {
                println("Adding appointments to calendar: $calendarId")
                addAppointmentsToCalendar(calendarId)

                // Generate the subscription link
                val subscriptionLink = "https://calendar.google.com/calendar/ical/$calendarId/public/basic.ics"

                // Copy to clipboard
                copyToClipboard(subscriptionLink, context)
                Toast.makeText(context, "Subscription link copied!", Toast.LENGTH_SHORT).show()

                // Open Google Calendar subscription page
                openGoogleCalendar(context)
            } else {
                Toast.makeText(context, "Failed to create calendar", Toast.LENGTH_SHORT).show()
            }
        }
    }) {
        Text(text = "Subscribe to Calendar")
    }
}


// Function to copy the subscription link to clipboard
fun copyToClipboard(subscriptionLink: String, context: Context) {
    val clipboard = getSystemService(context, ClipboardManager::class.java)
    val clip = android.content.ClipData.newPlainText("Calendar Subscription", subscriptionLink)
    clipboard?.setPrimaryClip(clip)
}

// Function to open Google Calendar subscription page
fun openGoogleCalendar(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://calendar.google.com/calendar/r/subscriptions"))
    context.startActivity(intent)
}






//------------------------

