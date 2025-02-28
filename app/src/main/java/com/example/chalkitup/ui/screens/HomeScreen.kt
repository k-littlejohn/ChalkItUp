/*
Code will need to be updated once tutor availability is taken into consideration, for now it is
based on the appointment database in firebase.
 */

package com.example.chalkitup.ui.screens

import androidx.compose.ui.res.painterResource
import com.example.chalkitup.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.navigation.NavController
import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.graphics.Color
import com.kizitonwose.calendar.compose.*
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import java.lang.Exception
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.ui.*
import java.time.LocalDate
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState


@Composable
fun HomeScreen(navController: NavController) {
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CalendarScreen()
            UpcomingAppointments { appointment ->
                selectedAppointment = appointment
            }
        }
    }

    // Appointment with details popup
    selectedAppointment?.let { appointment ->
        AppointmentPopup(
            appointment = appointment,
            onDismiss = { selectedAppointment = null }
        )
    }
}

// Appointment Data Class
data class Appointment(
    val appointmentID: String = "",
    val studentID: String = "",
    val tutorID: String = "",
    val tutorName: String = "",
    val studentName: String = "",
    val date: String = "",
    val time: String = "",
    val subject: String = "",
    val mode: String = "",
    val comments: String = ""
)


@Composable
fun CalendarScreen() {
    var bookedDates by remember { mutableStateOf(emptyList<String>()) }
    // Grabs usersID (UID) from firebase, this allows appointments to show uniquely to user.
    val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

    // Used to Test ; shows your current user ID.
    println("User ID: $currentUserID")


    // Firebase cross references appointments with users.
    LaunchedEffect(currentUserID) {
        if (currentUserID != null) {
            val db = FirebaseFirestore.getInstance()
            val appointmentsRef = db.collection("appointments")

            try {
                val userAppointments = appointmentsRef
                    .whereEqualTo("studentID", currentUserID)
                    .get()
                    .await()

                val tutorAppointments = appointmentsRef
                    .whereEqualTo("tutorID", currentUserID)
                    .get()
                    .await()

                // Debugging logs. Shows how many appointments you have
                println("User Appointments: ${userAppointments.documents.size}")
                println("Tutor Appointments: ${tutorAppointments.documents.size}")

                bookedDates = (userAppointments.documents + tutorAppointments.documents)
                    .mapNotNull { it.getString("date")?.replace("\"", "") }

                // More Error handling, ensures the booked dates are listed correctly to match calender.
                println("Booked Dates: $bookedDates")

            } catch (e: Exception) {
                println("Error fetching appointments: ${e.message}")
            }
        }
    }

    val currentMonth = remember { YearMonth.now() }
    val daysOfWeek = remember { daysOfWeek() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    // Calendar is currently available for three months prior and the next three months
    val calendarState = rememberCalendarState(
        startMonth = currentMonth.minusMonths(3),
        endMonth = currentMonth.plusMonths(3),
        firstVisibleMonth = currentMonth,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.White)
            .border(1.dp, Color.Gray.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = calendarState.firstVisibleMonth.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.Start)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = (-5).dp),
            horizontalArrangement = Arrangement.Center
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day.name.take(3),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray
                )
            }
        }

        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                val formattedDate = day.date.format(dateFormatter)
                val isBooked = bookedDates.contains(formattedDate)
                val isCurrentMonth = YearMonth.from(day.date) == calendarState.firstVisibleMonth.yearMonth

                Box(
                    modifier = Modifier
                        .size(45.dp, 35.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isBooked) Color(0xFF06C59C) else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        color = when {
                            isBooked -> Color.White
                            isCurrentMonth -> Color.Black
                            else -> Color.LightGray
                        },
                    )
                }
            }
        )
    }
}



@Composable
fun UpcomingAppointments(onAppointmentClick: (Appointment) -> Unit) {
    val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
    var appointments by remember { mutableStateOf(emptyList<Appointment>()) }

    LaunchedEffect(currentUserID) {
        if (currentUserID != null) {
            val db = FirebaseFirestore.getInstance()
            val appointmentsRef = db.collection("appointments")
            val usersRef = db.collection("users")

            try {
                val userAppointments = appointmentsRef
                    .whereEqualTo("studentID", currentUserID)
                    .get()
                    .await()
                    .documents

                val tutorAppointments = appointmentsRef
                    .whereEqualTo("tutorID", currentUserID)
                    .get()
                    .await()
                    .documents

                val allAppointments = (userAppointments + tutorAppointments).mapNotNull { doc ->
                    val appointment = doc.toObject(Appointment::class.java)?.copy(appointmentID = doc.id)

                    appointment?.let {
                        val tutorSnapshot = usersRef.document(it.tutorID).get().await()
                        val tutorFirstName = tutorSnapshot.getString("firstName") ?: "Unknown"
                        val tutorLastName = tutorSnapshot.getString("lastName") ?: ""

                        val studentSnapshot = usersRef.document(it.studentID).get().await()
                        val studentFirstName = studentSnapshot.getString("firstName") ?: "Unknown"
                        val studentLastName = studentSnapshot.getString("lastName") ?: ""

                        it.copy(
                            tutorName = "$tutorFirstName $tutorLastName",
                            studentName = "$studentFirstName $studentLastName"
                        )
                    }
                }

                appointments = allAppointments

            } catch (e: Exception) {
                println("Error fetching appointments: ${e.message}")
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Upcoming Appointments", style = MaterialTheme.typography.headlineSmall)

        appointments.forEach { appointment ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onAppointmentClick(appointment) },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(Color.LightGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Tutor: ${appointment.tutorName}", fontWeight = FontWeight.Bold)
                    Text(text = "Date: ${appointment.date}")
                    Text(text = "Time: ${appointment.time}")
                    Text(text = "Subject: ${appointment.subject}")
                    Text(text = "Location: ${appointment.mode}")
                }
            }
        }
    }
}


@Composable
fun AppointmentPopup(appointment: Appointment, onDismiss: () -> Unit) {
    var rebooking by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(appointment.date) }
    var availableDates by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            rebooking = false // Reset on dismiss
            onDismiss()
        },
        title = { Text(text = "Appointment Details") },
        text = {
            Column {
                Text(text = "Tutor: ${appointment.tutorName}")
                Text(text = "Date: ${selectedDate}")
                Text(text = "Time: ${appointment.time}")
                Text(text = "Subject: ${appointment.subject}")
                Text(text = "Location: ${appointment.mode}")
                Text(text = "Comments: ${appointment.comments}")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    rebooking = false // Reset
                    onDismiss()
                }
            ) { Text("Close") }
        },
        dismissButton = {
            Column {
                Button(
                    onClick = {
                        cancelAppointment(appointment) {
                            rebooking = false // Reset after cancelling
                            onDismiss()
                        }
                    },
                    enabled = !rebooking
                ) {
                    Text("Cancel Appointment")
                }
                Button(
                    onClick = {
                        rebooking = true
                        availableDates = true
                    },
                    enabled = !rebooking
                ) {
                    Text("Rebook Appointment")
                }
            }
        }
    )

    if (availableDates) {
        SelectDates(
            currentAppointmentDate = selectedDate,
            onDateUpdated = { newDate ->
                selectedDate = newDate
                rebookAppointment(appointment, newDate) {
                    rebooking = false
                    availableDates = false
                    onDismiss()
                }
            },
            onDismiss = {
                rebooking = false
                availableDates = false
            }
        )
    }
}


@Composable
fun SelectDates(
    currentAppointmentDate: String,
    onDateUpdated: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = remember { LocalDate.now() }
    val selectedDate = remember { mutableStateOf(LocalDate.parse(currentAppointmentDate)) }

    val availableDates = List(5) { today.plusDays(it.toLong()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select a New Date") },
        text = {
            Column {
                Text("Selected Date: ${selectedDate.value.format(dateFormatter)}")
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(availableDates) { date ->
                        Button(
                            onClick = { selectedDate.value = date },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = date.format(dateFormatter))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onDateUpdated(selectedDate.value.format(dateFormatter))
                onDismiss()
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


fun cancelAppointment(appointment: Appointment, onComplete: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val appointmentRef = db.collection("appointments").document(appointment.appointmentID)

    appointmentRef.delete().addOnSuccessListener {
        println("Appointment canceled successfully!")
        onComplete()
    }.addOnFailureListener { e ->
        println("Error canceling appointment: ${e.message}")
        onComplete()
    }
}


fun rebookAppointment(
    appointment: Appointment,
    newDate: String,
    onComplete: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val appointmentRef = db.collection("appointments").document(appointment.appointmentID)

    appointmentRef.delete().addOnCompleteListener {
        if (it.isSuccessful) {
            println("Appointment deleted successfully, proceeding with rebook.")

            val newAppointment = appointment.copy(date = newDate, appointmentID = "")
            db.collection("appointments").add(newAppointment)
                .addOnSuccessListener {
                    println("Appointment rebooked successfully!")
                    onComplete()
                }
                .addOnFailureListener { e ->
                    println("Error rebooking appointment: ${e.message}")
                    onComplete()
                }
        } else {
            println("Error deleting appointment: ${it.exception?.message}")
            onComplete()
        }
    }
}