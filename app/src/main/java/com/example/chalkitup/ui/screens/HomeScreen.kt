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
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.tasks.await
import java.lang.Exception


@Composable
fun HomeScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CalendarScreen()
        }
    }
}

@Composable
fun CalendarScreen() {
    var bookedDates by remember { mutableStateOf(emptyList<String>()) }
    // Grabs usersID (UID) from firebase, this allows appointments to show uniquely to user.
    val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

    // Used to Test ; shows your current user ID.
    println("User ID: $currentUserID")

    //Firebase cross references appointments with users.
    LaunchedEffect(currentUserID) {
        if (currentUserID != null) {
            val db = FirebaseFirestore.getInstance()
            val appointmentsRef = db.collection("appointments")

            try {
                val userAppointments = appointmentsRef
                    .whereEqualTo("userID", currentUserID)
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
