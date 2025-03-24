package com.example.chalkitup.ui.screens

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.Events
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Collections
import android.content.ClipData
import android.content.ClipboardManager
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
//import androidx.privacysandbox.tools.core.generator.build
import com.example.chalkitup.ui.viewmodel.Appointment
import com.example.chalkitup.ui.viewmodel.AuthViewModel
import com.example.chalkitup.ui.viewmodel.BookingManager
import com.example.chalkitup.ui.viewmodel.SettingsViewModel
import com.example.chalkitup.ui.viewmodel.OfflineDataManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import com.google.api.services.calendar.model.Calendar as GoogleCalendar // Rename import to avoid conflict
import com.google.api.services.calendar.model.EventDateTime


@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    navController: NavController,
    offlineViewModel: OfflineDataManager,

) {
    val context = LocalContext.current
    val account = GoogleSignIn.getLastSignedInAccount(context)

    val service = account?.let { CalendarService(context, it) }

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
        SubscribeToCalendarButton(service)

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
fun SubscribeToCalendarButton(service: CalendarService?) {
    val context = LocalContext.current
    var showSuccessMessage by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (service != null) {
                val calendarId = service.getPrimaryCalendarId()
                if (calendarId != null) {
                    openGoogleCalendarSubscriptionPage(context, calendarId)
                    showSuccessMessage = true
                }
            }
        }
    ) {
        Text("Subscribe to Google Calendar")
    }

    if (showSuccessMessage) {
        AlertDialog(
            onDismissRequest = { showSuccessMessage = false },
            title = { Text("Subscription Successful") },
            text = { Text("Google Calendar subscription link opened.") },
            confirmButton = {
                Button(onClick = { showSuccessMessage = false }) {
                    Text("OK")
                }
            }
        )
    }
}



fun openGoogleCalendarSubscriptionPage(context: Context, calendarId: String) {
    val url = "https://calendar.google.com/calendar/u/0/r/settings/addbyurl?cid=$calendarId"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "No browser found to open the link", Toast.LENGTH_SHORT).show()
    }
}


fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Google Calendar Subscription URL", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Subscription URL copied to clipboard", Toast.LENGTH_SHORT).show()
}

fun createGoogleCalendar(service: com.google.api.services.calendar.Calendar): String? {
    return try {
        val calendar = com.google.api.services.calendar.model.Calendar().apply {
            summary = "My New Calendar"
            timeZone = "America/New_York" // Set your timezone
        }

        val createdCalendar = service.calendars().insert(calendar).execute()
        createdCalendar.id // Returns the new calendar's ID
    } catch (e: Exception) {
        Log.e("GoogleCalendar", "Error creating calendar", e)
        null
    }
}

fun getUserCalendars(service: com.google.api.services.calendar.Calendar) {
    val calendarList = service.calendarList().list().execute()

    for (calendar in calendarList.items) {
        Log.d("GoogleCalendar", "Calendar Name: ${calendar.summary}, ID: ${calendar.id}")
    }
}
fun getPrimaryCalendarId(service: com.google.api.services.calendar.Calendar): String? {
    return try {
        val calendarList = service.calendarList().list().execute()
        val primaryCalendar = calendarList.items.find { it.primary ?: false }
        primaryCalendar?.id
    } catch (e: Exception) {
        Log.e("GoogleCalendar", "Error fetching Calendar ID", e)
        null
    }
}

fun subscribeToNewCalendar(service: com.google.api.services.calendar.Calendar, context: Context) {
    val newCalendarId = createGoogleCalendar(service)

    if (newCalendarId != null) {
        // Make the calendar public before generating the subscription link
        makeCalendarPublic(service, newCalendarId)

        val subscriptionUrl = "https://calendar.google.com/calendar/ical/$newCalendarId/public/basic.ics"

        copyToClipboard(context, subscriptionUrl)
        openGoogleCalendarSubscriptionPage(context, newCalendarId)

        Toast.makeText(context, "New calendar created! Subscription URL copied.", Toast.LENGTH_LONG).show()
    } else {
        Toast.makeText(context, "Failed to create calendar", Toast.LENGTH_LONG).show()
    }
}

fun makeCalendarPublic(service: com.google.api.services.calendar.Calendar, calendarId: String) {
    try {
        val rule = com.google.api.services.calendar.model.AclRule().apply {
            role = "reader" // Allows public read access
            scope = com.google.api.services.calendar.model.AclRule.Scope().apply {
                type = "default" // Means public access
            }
        }

        service.acl().insert(calendarId, rule).execute()
        Log.d("GoogleCalendar", "Calendar is now public!")
    } catch (e: Exception) {
        Log.e("GoogleCalendar", "Error making calendar public", e)
    }
}


class CalendarService(context: Context, account: GoogleSignInAccount) {

    private val APPLICATION_NAME = "ChalkItUp"
    private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()

    private var calendarService: com.google.api.services.calendar.Calendar? = null

    init {
        try {
            val transport = GoogleNetHttpTransport.newTrustedTransport()
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(CalendarScopes.CALENDAR)
            ).setSelectedAccount(account.account)

            calendarService = com.google.api.services.calendar.Calendar.Builder(
                transport, JSON_FACTORY, credential
            ).setApplicationName(APPLICATION_NAME).build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPrimaryCalendarId(): String? {
        return try {
            val calendarList = calendarService?.calendarList()?.list()?.execute()
            val primaryCalendar = calendarList?.items?.find { it.primary ?: false }
            primaryCalendar?.id
        } catch (e: Exception) {
            Log.e("GoogleCalendar", "Error fetching Calendar ID", e)
            null
        }
    }
}
