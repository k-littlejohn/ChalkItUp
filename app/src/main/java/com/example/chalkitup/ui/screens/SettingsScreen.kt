package com.example.chalkitup.ui.screens

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.CalendarScopes
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
import com.example.chalkitup.ui.viewmodel.AuthViewModel
import com.example.chalkitup.ui.viewmodel.SettingsViewModel
import com.example.chalkitup.ui.viewmodel.OfflineDataManager
import com.example.chalkitup.ui.viewmodel.UserProfile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


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
    var userProfile by remember { mutableStateOf(UserProfile()) }
    val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
    val calendarService = if (googleAccount != null) CalendarService(context, googleAccount, userProfile) else null
    var calendarId by remember { mutableStateOf(CalendarId("")) }
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // temp logout button
        /////////////////// subscription google link



        SubscribeToCalendarButton(
            service = calendarService,
            calendarIdObject = calendarId,
            onUpdateCalendarId = { updatedCalendarId ->
                calendarId = updatedCalendarId
            }
        )

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
fun SubscribeToCalendarButton(
    service: CalendarService?,
    calendarIdObject: CalendarId,
    onUpdateCalendarId: (CalendarId) -> Unit
) {
    val context = LocalContext.current
    var showSuccessMessage by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (service == null) {
                Toast.makeText(context, "Calendar service is unavailable", Toast.LENGTH_LONG).show()
                return@Button
            }

            var calendarId = calendarIdObject.calendarId
            if (calendarId.isEmpty()) {
                val newCalendarId = createGoogleCalendar(service) ?: ""
                onUpdateCalendarId(CalendarId(newCalendarId)) // Update CalendarId object
                calendarId = newCalendarId
            }

            if (calendarId.isNotEmpty()) {
                openGoogleCalendarSubscriptionPage(context, calendarId)
                showSuccessMessage = true
            } else {
                Toast.makeText(context, "Failed to create calendar", Toast.LENGTH_LONG).show()
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

//fun subscribeToNewCalendar(service: com.google.api.services.calendar.Calendar, context: Context) {
//    val newCalendarId = createGoogleCalendar(service)
//
//    if (newCalendarId != null) {
//        // Make the calendar public before generating the subscription link
//        makeCalendarPublic(service, newCalendarId)
//
//        val subscriptionUrl = "https://calendar.google.com/calendar/ical/$newCalendarId/public/basic.ics"
//
//        copyToClipboard(context, subscriptionUrl)
//        openGoogleCalendarSubscriptionPage(context, newCalendarId)
//
//        Toast.makeText(context, "New calendar created! Subscription URL copied.", Toast.LENGTH_LONG).show()
//    } else {
//        Toast.makeText(context, "Failed to create calendar", Toast.LENGTH_LONG).show()
//    }
//}

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


class CalendarService(
    context: Context,
    account: GoogleSignInAccount,
    userProfile: UserProfile // Pass UserProfile as a parameter
) {

    private val APPLICATION_NAME = "ChalkItUp-" +
            (userProfile.firstName.ifEmpty { "Unknown" }) + " " +
            (userProfile.lastName.ifEmpty { "User" }) // Use userProfile instance
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
    fun getGoogleCalendarService(): com.google.api.services.calendar.Calendar? {
        return calendarService
    }
}

fun createGoogleCalendar(service: CalendarService?): String? {
    return try {
        // Extract the actual Google Calendar API service
        val googleCalendarService = service?.getGoogleCalendarService()

        if (googleCalendarService == null) {
            Log.e("GoogleCalendar", "Calendar service is null")
            return null
        }

        val calendar = com.google.api.services.calendar.model.Calendar().apply {
            summary = "ChalkItUp Calendar"
            timeZone = "America/Edmonton"
        }

        val createdCalendar = googleCalendarService.calendars().insert(calendar).execute()
        createdCalendar.id // Returns the new calendar's ID

    } catch (e: Exception) {
        Log.e("GoogleCalendar", "Error creating calendar", e)
        null
    }
}

data class CalendarId(val calendarId: String="")

fun onUpdateCalendarId(calendarId: CalendarId) {
    // Store the CalendarId object in Firestore or your local storage
    updateCalendarIdInFirestore(calendarId)
}
fun updateCalendarIdInFirestore(calendarId: CalendarId) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    if (userId != null) {
        val userProfileRef = FirebaseFirestore.getInstance().collection("users").document(userId)
        userProfileRef.update("calenderID", calendarId.calendarId)
            .addOnSuccessListener {
                Log.d("Firestore", "Calendar ID updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error updating Calendar ID", e)
            }
    }
}
