package com.example.chalkitup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chalkitup.R
import com.example.chalkitup.ui.viewmodel.TutorAvailabilityViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Displays the tutor's availability selection screen.
 *
 * This function presents a calendar interface where tutors can select available
 * time slots for specific days. Tutors can click on a day to
 * display available time slots from <9 AM to 9 PM> in 30-minute increments.
 * Selected time slots are highlighted and stored, while deselected slots are removed.
 * Tutors can save or cancel their selections using the provided buttons.
 *
 * @param navController The navigation controller for navigating between screens.
 * @param viewModel The ViewModel that manages tutor availability data and logic.
 */

@Composable
fun EnterTutorAvailability(
    navController: NavController,
    viewModel: TutorAvailabilityViewModel
) {
    //------------------------------VARIABLES----------------------------------------------

    // Scroll state for the entire screen
    val scrollState = rememberScrollState()

    // Collecting states from ViewModel
    //val bookedAppointments by viewModel.bookedAppointments.collectAsState()
    val tutorAvailability by viewModel.tutorAvailabilityList.collectAsState() // List of tutor's available time slots
    val selectedDay by viewModel.selectedDay.collectAsState() // Currently selected day
    val selectedTimeSlots by viewModel.selectedTimeSlots.collectAsState() // Selected time slots for the chosen day
    val isEditing by viewModel.isEditing.collectAsState() // Boolean flag indicating edit mode

    // Get the current month and store necessary date formatters
    val currentMonth = remember { YearMonth.now() }// Stores the current month
    val daysOfWeek = remember { daysOfWeek() } // Gets a list of days in a week (Mon-Sun)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") } // Formatter for database storage

    // Calendar state for handling month navigation
    // Currently the user can only view & edit the current month
    // I want to make previous and next <2?> months viewable but not editable -Jeremelle
    val calendarState = rememberCalendarState(
        startMonth = currentMonth.minusMonths(0),
        endMonth = currentMonth.plusMonths(0),
        firstVisibleMonth = currentMonth,
    )

    //------------------------------VARIABLES-END---------------------------------------------

    // Select the first day of the month by default
    LaunchedEffect(calendarState.firstVisibleMonth) {
        val firstDay = calendarState.firstVisibleMonth.yearMonth.atDay(1).format(dateFormatter)
        viewModel.selectDay(firstDay)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                // Display the current month and year
                Text(
                    text = calendarState.firstVisibleMonth.yearMonth.format(
                        DateTimeFormatter.ofPattern(
                            "MMMM yyyy"
                        )
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Display the days of the week headers (Mon-Sun)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day.name.take(2),
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
                        val hasAvailability = tutorAvailability.any { it.day == formattedDate } // Check if day has availability
                        val isSelected = selectedDay == formattedDate // Check if day is selected
                        val isToday = day.date == LocalDate.now()
                        val isCurrentMonth = YearMonth.from(day.date) == calendarState.firstVisibleMonth.yearMonth

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> Color(0xfffad96e) // Yellow for selected day
                                        hasAvailability -> Color(0xFFc183d4) // Green if availability exists
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isToday) 2.dp else 0.dp,
                                    color = if (isToday) Color(0xFFc183d4) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.selectDay(formattedDate) // Select day on click
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.date.dayOfMonth.toString(),
                                color = when {
                                    isSelected || hasAvailability -> Color.White
                                    isCurrentMonth -> Color.Black
                                    else -> Color.LightGray
                                },
                            )
                        }
                    }
                )
            }
        }

        // Row for selected day text and buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Action buttons (Edit, Save, Cancel)
            if (isEditing) {
                // Display Save and Cancel buttons in edit mode
                Button(
                    onClick = { viewModel.saveAvailability() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.width(160.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFd2e5fa))
                ) {
                    Text(text = "Save", color = Color.Black)
                }
                Button(
                    onClick = { viewModel.cancelEdit() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.width(160.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFd2e5fa))
                ) {
                    Text(text = "Cancel", color = Color.Black)
                }
            } else {
                Box(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.toggleEditMode() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "Edit Profile",
                        tint = Color(0xFF000080),
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column (modifier = Modifier.width(82.dp)) { }
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(100.dp)
            ) {
                Text("Online")
                Divider(
                    color = Color.Gray, // Color of the divider
                    thickness = 2.dp, // Thickness of the divider
                )
            }
            Column (modifier = Modifier.width(20.dp)) { }
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(100.dp)
            ) {
                Text("In Person")
                Divider(
                    color = Color.Gray, // Color of the divider
                    thickness = 2.dp, // Thickness of the divider
                )
            }
            Column (modifier = Modifier.width(5.dp)) { }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(modifier = Modifier.height(500.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(viewModel.timeIntervals) { time ->
                        // Find the corresponding TimeSlot object (if it exists)
                        val timeSlot = selectedTimeSlots.find { it.time == time }

                        // Check if the time slot is booked
                        val isBooked = timeSlot?.booked ?: false

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Time slot label
                            Card(
                                modifier = Modifier.width(80.dp),
                                colors = CardColors(
                                    disabledContentColor = Color.Black,
                                    containerColor = Color.Transparent,
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color.Transparent
                                )
                            ) {
                                Text(text = time)
                            }

                            // Online selection box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .background(
                                        if (isBooked) Color(0xfffad96e) // Pink if booked
                                        else if (timeSlot?.online == true) Color(0xFF54A4FF) // Blue if online is selected
                                        else Color.Transparent // White if unselected
                                    )
                                    .clickable(enabled = isEditing && !isBooked) {
                                        viewModel.toggleTimeSlotSelection(time, "online")
                                    },
                            )

                            // In-Person selection box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .background(
                                        if (isBooked) Color(0xfffad96e) // Pink if booked
                                        else if (timeSlot?.inPerson == true) Color(0xFF06C59C) // Blue if inPerson is selected
                                        else Color.Transparent // White if unselected
                                    )
                                    .clickable(enabled = isEditing && !isBooked) {
                                        viewModel.toggleTimeSlotSelection(time, "inPerson")
                                    },
                            )
                        }

                        // Separator Row
                        Row {
                            Box(
                                modifier = Modifier
                                    .height(1.dp)
                                    .width(50.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .height(1.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}