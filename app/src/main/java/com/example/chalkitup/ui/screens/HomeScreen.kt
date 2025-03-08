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
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Brush
import java.util.Locale
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos


@Composable
fun HomeScreen(navController: NavController) {
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }
    var userName by remember { mutableStateOf("User") }

    // Username from Firebase Database
    LaunchedEffect(Unit) {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
        currentUserID?.let {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(it)
            val snapshot = userRef.get().await()
            userName = snapshot.getString("firstName") ?: "User"
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            GreetingSection(userName)
            Spacer(modifier = Modifier.height(26.dp))
            CalendarScreen()
            UpcomingAppointments { appointment ->
                selectedAppointment = appointment
            }
        }
    }

    // Appointment With More Details Popup
    selectedAppointment?.let { appointment ->
        AppointmentPopup(
            appointment = appointment,
            onDismiss = { selectedAppointment = null }
        )
    }
}

// User Greeting Above Calendar Based On Time of Day.
@Composable
fun GreetingSection(userName: String) {
    val greetingText = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "$greetingText, \n$userName",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black,
            modifier = Modifier.align(Alignment.TopStart)
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

// Calendar Screen Shows Appointments For Both User Types
@Composable
fun CalendarScreen() {
    var bookedDates by remember { mutableStateOf(emptyList<String>()) }
    val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

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

                bookedDates = (userAppointments.documents + tutorAppointments.documents)
                    .mapNotNull { it.getString("date")?.replace("\"", "") }

            } catch (e: Exception) {
                println("Error fetching appointments: ${e.message}")
            }
        }
    }

    // Current Visibility Set Up Is 3 Months Prior and 3 Months Forward
    val calendarState = rememberCalendarState(
        startMonth = YearMonth.now().minusMonths(3),
        endMonth = YearMonth.now().plusMonths(3),
        firstVisibleMonth = YearMonth.now(),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Calendars UI Full Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFE3F2FD), Color(0xFFF1F8E9))
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {

                Spacer(modifier = Modifier.height(6.dp))

                // Month Title Inside of Calendar
                Text(
                    text = calendarState.firstVisibleMonth.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Day Labels (n:2 = SU, MO, TU, etc.), Considered Sun, Mon etc, However Looked Crowded.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    daysOfWeek().forEach { day ->
                        Text(
                            text = day.name.take(2).uppercase(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = Color.DarkGray
                        )
                    }
                }

                // Calendar Grid Showcasing Booked Days Along With Current Day Outlined
                HorizontalCalendar(
                    state = calendarState,
                    dayContent = { day ->
                        val formattedDate = day.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        val isBooked = bookedDates.contains(formattedDate)
                        val isToday = day.date == LocalDate.now()
                        val isCurrentMonth = YearMonth.from(day.date) == calendarState.firstVisibleMonth.yearMonth

                        // UI , Rounded Corners, Filled isBooked Appointments, Outlined Day
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isBooked -> Color(0xFF06C59C)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isToday) 2.dp else 0.dp,
                                    color = if (isToday) Color(0xFF06C59C) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                ),
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
    }
}


// Display Of Upcoming Appointments in Database, The Current Goal Is To Remove Days That Have Passed,
// Displaying Days in Order From Soonest to Latest.
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

                // Filter Out Past Appointments and Sort By Date (Earliest First)
                val today = LocalDate.now()
                appointments = allAppointments
                    .filter { appointment ->
                        val appointmentDate = try {
                            LocalDate.parse(appointment.date, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
                        } catch (e: Exception) {
                            null
                        }
                        appointmentDate != null && appointmentDate.isAfter(today.minusDays(1)) // Excludes past appointments
                    }
                    .sortedBy { appointment ->
                        LocalDate.parse(appointment.date, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
                    }

            } catch (e: Exception) {
                println("Error fetching appointments: ${e.message}")
            }
        }
    }

    //Start of UI for Upcoming Appointments
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Upcoming Appointments",
            fontSize = 20.sp,
            color = Color.Black
        )

        appointments.forEach { appointment ->
            // Format the date properly using "MMM d"
            val formattedDate = try {
                val date = LocalDate.parse(appointment.date, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
                date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US)) // Outputs "Mar 6"
            } catch (e: Exception) {
                "Invalid Date"
            }

            UpcomingAppointmentItem(
                title = appointment.subject,
                date = formattedDate,
                tutor = appointment.tutorName,
                mode = appointment.mode,
                time = appointment.time,
                backgroundColor = Color.White,
                onClick = { onAppointmentClick(appointment) }
            )
        }
    }
}

// UI for Upcoming Appointments
@Composable
fun UpcomingAppointmentItem(
    title: String,
    date: String,
    tutor: String,
    mode: String,
    time: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(90.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Extracting Day and Month for UI
                val dateParts = date.split(" ")
                val month = dateParts.getOrNull(0)?.uppercase(Locale.ROOT) ?: "--"
                val day = dateParts.getOrNull(1) ?: "--"

                // Vertical Line Beside Appointment
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    // Line Beside Date is Currently Green
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(70.dp)
                            .background(Color(0xFF06C59C), RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF06C59C)
                        )
                        Text(
                            text = month,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF06C59C)
                        )
                    }
                }

                // Appointment Details Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF54A4FF)
                    )
                    Text(
                        text = tutor, // Tutor's Name Below Subject,  ( May Remove, Looking Crowded)
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = mode, // Online/In-Person
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = time, // Time of Appointment
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                // Right Arrow Icon
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "More",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bottom shadow effect
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(10.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.1f))
                    )
                )
        )
    }
}






@Composable
fun AppointmentPopup(appointment: Appointment, onDismiss: () -> Unit) {
    var rebooking by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(appointment.date) }
    var availableDates by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            rebooking = false // Reset
            onDismiss()
        },
        title = { Text(text = "Appointment Details") },
        text = {
            Column {
                Text(text = "Tutor: ${appointment.tutorName}")
                Text(text = "Date: $selectedDate")
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
                            rebooking = false // Reset After Cancelling
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

// This Is A Placeholder, I Am Waiting For Booking to Be Fully Functional
// Once Booking is Functional With Tutors Availability This Will Need To Be Changed
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

// When Cancelling and Rebooking The Tutors Availability Will Need To Be Returned Back To Selection
// The Following cancelAppointment and rebookAppointment Is Also A Placeholder For Now.
// They Work Just Not In The Context We Need
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