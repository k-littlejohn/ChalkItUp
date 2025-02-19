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
import androidx.navigation.NavController // unused atm
import androidx.compose.runtime.*
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

//
@Composable
fun CalendarScreen() {
    var bookedDates by remember { mutableStateOf(emptyList<String>()) }

    // Gets booked appointments from Firebase
    LaunchedEffect(true) {
        val db = FirebaseFirestore.getInstance()
        val appointmentsRef = db.collection("appointments")

        val result = appointmentsRef.get().await()
        bookedDates = result.documents.mapNotNull { it.getString("date") }
    }

    val currentMonth = remember { YearMonth.now() }
    val daysOfWeek = remember { daysOfWeek() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    // Shows the current month, but I have limited to view 3 months prior and 3 forward
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
            .border(1.dp, Color.Gray.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)), // Light border around calendar
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display Month and Year
        Text(
            text = calendarState.firstVisibleMonth.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.Start) // Aligns month to the left
        )


        // Row to display Sun - Sat
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = (-5).dp), // Needed to move text left to align
            horizontalArrangement = Arrangement.Center) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day.name.take(3), // Show first 3 letters (e.g., Sun, Mon)
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f),
                      //  .padding(start = 4.dp), // Adjust padding slightly if needed
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
                        .size(45.dp, 35.dp) // Oval shape size
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
