package com.example.chalkitup.ui.screens

import android.util.Log
import android.widget.Space
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chalkitup.Connection
import com.example.chalkitup.ui.viewmodel.Appointment
import com.example.chalkitup.ui.viewmodel.BookingManager
import com.example.chalkitup.ui.viewmodel.HomeViewModel
import com.example.chalkitup.ui.viewmodel.WeatherViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel(),
    weatherViewModel: WeatherViewModel = viewModel()
) {
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }
    val userName by homeViewModel.userName.collectAsState()

    // Observe Weather Data
    val weatherState by weatherViewModel.weather

    // Extract Temperature and Condition (Cloud, Sunny etc)
    val temperature = weatherState?.current?.tempC?.toInt()?.toString() ?: "--"
    val condition = weatherState?.current?.condition?.text ?: "Unknown"

    // Gradient Background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF54A4FF), // 5% Blue
            Color.White, Color.White
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GreetingSection(userName!!)

                Spacer(modifier = Modifier.weight(4f))

                // Update WeatherWidget with Real Data
                WeatherWidget(
                    temperature = temperature,
                    condition = condition,
                    modifier = Modifier
                        .size(110.dp)
                        .align(Alignment.Top)
                )
            }

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
            navController = navController,
            appointment = appointment,
            onDismiss = { selectedAppointment = null }
        )
    }
}


// User Greeting Above Calendar Based On Time of Day.
@Composable
fun GreetingSection(userName: String, modifier: Modifier = Modifier) {
    val greetingText = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Column(modifier = modifier) {
        Text(
            text = "$greetingText,",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black
        )
        Text(
            text = userName,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black
            )
    }
}

//Weather Widget, API Used: weatherapi.com
@Composable
fun WeatherWidget(temperature: String, condition: String, modifier: Modifier = Modifier) {
    val weatherIcon = when (condition.lowercase(Locale.ROOT)) {
        "cloudy" -> R.drawable.ic_cloudy
        "rainy" -> R.drawable.ic_rainy
        "sunny" -> R.drawable.ic_sunny
        "snow" -> R.drawable.ic_snow
        else -> R.drawable.ic_cloudy
    }

    Box(
        modifier = modifier
            .width(90.dp)
            .height(140.dp)
            .clip(
                RoundedCornerShape(
                    topEnd = 40.dp,
                    bottomStart = 40.dp
                )
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E2678), Color(0xFF756EF3)) // Deep blue to violet
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = weatherIcon),
                contentDescription = "Weather Icon",
                modifier = Modifier
                    .size(56.dp)
                    .padding(top = 2.dp)
            )
            Text(
                text = condition,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                //modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = "$temperature°C",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

// Calendar Screen Shows Appointments For Both User Types
@Composable
fun CalendarScreen(
    homeViewModel: HomeViewModel = viewModel()
) {
    val bookedDates by homeViewModel.bookedDates.collectAsState()

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
fun UpcomingAppointments(
    homeViewModel: HomeViewModel = viewModel(),
    onAppointmentClick: (Appointment) -> Unit
) {


    //Start of UI for Upcoming Appointments
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Upcoming Appointments",
            fontSize = 20.sp,
            color = Color.Black
        )

        val context = LocalContext.current
        val connection = Connection.getInstance(context)
        val isConnected by connection.connectionStatus.collectAsState(initial = false)
        if(isConnected) {
            val appointments by homeViewModel.appointments.collectAsState()
            BookingManager.clearBookings()
            appointments.forEach { appointment ->
                // Format the date properly using "MMM d"
                val formattedDate = try {
                    val date = LocalDate.parse(
                        appointment.date,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
                    )
                    date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US)) // Outputs "Mar 6"
                } catch (e: Exception) {
                    "Invalid Date"
                }

                UpcomingAppointmentItem(
                    title = appointment.subject,
                    date = formattedDate,
                    tutor = appointment.tutorName,
                    student = appointment.studentName,
                    mode = appointment.mode,
                    time = appointment.time,
                    backgroundColor = Color.White,
                    onClick = { onAppointmentClick(appointment) }
                )
                BookingManager.addBooking(appointment)
                Log.d("offlineApp", "App logged: $appointment.appointmentID")
            }
        }
        else{
            val appointments=BookingManager.readBookings()
            appointments.forEach { appointment ->
                    // Format the date properly using "MMM d"
                val formattedDate = try {
                    val date = LocalDate.parse(
                        appointment.date,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
                    )
                    date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US)) // Outputs "Mar 6"
                } catch (e: Exception) {
                    "Invalid Date"
                }

                UpcomingAppointmentItem(
                    title = appointment.subject,
                    date = formattedDate,
                    tutor = appointment.tutorName,
                    student = appointment.studentName,
                    mode = appointment.mode,
                    time = appointment.time,
                    backgroundColor = Color.White,
                    onClick = { onAppointmentClick(appointment) }
                )

                Log.d("offlineApp", "OfflineApp logged: $appointment.appointmentID")


            }
        }
    }
}

// UI for Upcoming Appointments
@Composable
fun UpcomingAppointmentItem(
    homeViewModel: HomeViewModel = viewModel(),
    title: String,
    date: String,
    tutor: String,
    student: String,
    mode: String,
    time: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    val userType by homeViewModel.userType.collectAsState()

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
                    Text ( // Tutor's Name Below Subject,  ( May Remove, Looking Crowded)
                        text = if (userType == "Tutor") student
                        else tutor,
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
fun AppointmentPopup(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel(),
    appointment: Appointment, onDismiss: () -> Unit
) {
    val userType by homeViewModel.userType.collectAsState()

    //var rebooking by remember { mutableStateOf(false) }
//    var selectedDate by remember { mutableStateOf(appointment.date) }
    //var availableDates by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            //rebooking = false // Reset
            onDismiss()
        },
        title = { Text(text = "Appointment Details") },
        text = {
            Column {
                if (userType == "Tutor") {
                    Text(text = "Student: ${appointment.studentName}")
                } else {
                    Text(text = "Tutor: ${appointment.tutorName}")
                }
                Text(text = "Date: ${appointment.date}")
                Text(text = "Time: ${appointment.time}")
                Text(text = "Subject: ${appointment.subject}")
                Text(text = "Location: ${appointment.mode}")
                Text(text = "Comments: ${appointment.comments}")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    //rebooking = false // Reset
                    onDismiss()
                }
            ) { Text("Close") }
        },
        dismissButton = {
            Column {
                Button(
                    onClick = {
                        homeViewModel.cancelAppointment(appointment) {
                            //rebooking = false // Reset After Cancelling
                            onDismiss()
                            homeViewModel.fetchAppointments()
                        }
                    },
                    //enabled = !rebooking
                ) {
                    Text("Cancel Appointment")
                }
                if (userType == "Student") {
                    Button(
                        onClick = {
//                            bookingViewModel.rebookAppointment(appointment)
                            homeViewModel.cancelAppointment(appointment) {
                                //rebooking = false // Reset After Cancelling
                                onDismiss()
                                navController.navigate("booking")
                            }
                            //rebooking = true
                            //availableDates = true
                        },
                        //enabled = !rebooking
                    ) {
                        Text("Rebook Appointment")
                    }
                }
            }
        }
    )

//    if (availableDates) {
//        SelectDates(
//            currentAppointmentDate = selectedDate,
//            onDateUpdated = { newDate ->
//                selectedDate = newDate
//                rebookAppointment(appointment, newDate) {
//                    rebooking = false
//                    availableDates = false
//                    onDismiss()
//                }
//            },
//            onDismiss = {
//                rebooking = false
//                availableDates = false
//            }
//        )
//    }
}

// This Is A Placeholder, I Am Waiting For Booking to Be Fully Functional
// Once Booking is Functional With Tutors Availability This Will Need To Be Changed
//@Composable
//fun SelectDates(
//    currentAppointmentDate: String,
//    onDateUpdated: (String) -> Unit,
//    onDismiss: () -> Unit
//) {
//    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//    val today = remember { LocalDate.now() }
//    val selectedDate = remember { mutableStateOf(LocalDate.parse(currentAppointmentDate)) }
//
//    val availableDates = List(5) { today.plusDays(it.toLong()) }
//
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = { Text("Select a New Date") },
//        text = {
//            Column {
//                Text("Selected Date: ${selectedDate.value.format(dateFormatter)}")
//                Spacer(modifier = Modifier.height(8.dp))
//                LazyColumn {
//                    items(availableDates) { date ->
//                        Button(
//                            onClick = { selectedDate.value = date },
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Text(text = date.format(dateFormatter))
//                        }
//                    }
//                }
//            }
//        },
//        confirmButton = {
//            Button(onClick = {
//                onDateUpdated(selectedDate.value.format(dateFormatter))
//                onDismiss()
//            }) {
//                Text("Confirm")
//            }
//        },
//        dismissButton = {
//            Button(onClick = onDismiss) {
//                Text("Cancel")
//            }
//        }
//    )
//}

// When Cancelling and Rebooking The Tutors Availability Will Need To Be Returned Back To Selection
// The Following cancelAppointment and rebookAppointment Is Also A Placeholder For Now.
// They Work Just Not In The Context We Need
//fun cancelAppointment(appointment: Appointment, onComplete: () -> Unit) {
//    val db = FirebaseFirestore.getInstance()
//    val appointmentRef = db.collection("appointments").document(appointment.appointmentID)
//
//    appointmentRef.delete().addOnSuccessListener {
//        println("Appointment canceled successfully!")
//        onComplete()
//    }.addOnFailureListener { e ->
//        println("Error canceling appointment: ${e.message}")
//        onComplete()
//    }
//}

//fun rebookAppointment(
//    appointment: Appointment,
//    newDate: String,
//    onComplete: () -> Unit
//) {
//    val db = FirebaseFirestore.getInstance()
//    val appointmentRef = db.collection("appointments").document(appointment.appointmentID)
//
//    appointmentRef.delete().addOnCompleteListener {
//        if (it.isSuccessful) {
//            println("Appointment deleted successfully, proceeding with rebook.")
//
//            val newAppointment = appointment.copy(date = newDate, appointmentID = "")
//            db.collection("appointments").add(newAppointment)
//                .addOnSuccessListener {
//                    println("Appointment rebooked successfully!")
//                    onComplete()
//                }
//                .addOnFailureListener { e ->
//                    println("Error rebooking appointment: ${e.message}")
//                    onComplete()
//                }
//        } else {
//            println("Error deleting appointment: ${it.exception?.message}")
//            onComplete()
//        }
//    }
//}
