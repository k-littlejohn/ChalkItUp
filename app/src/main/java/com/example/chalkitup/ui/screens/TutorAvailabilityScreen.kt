package com.example.chalkitup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
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
    val bookedAppointments by viewModel.bookedAppointments.collectAsState()
    val tutorAvailability by viewModel.tutorAvailabilityList.collectAsState() // List of tutor's available time slots
    val selectedDay by viewModel.selectedDay.collectAsState() // Currently selected day
    val selectedTimeSlots by viewModel.selectedTimeSlots.collectAsState() // Selected time slots for the chosen day
    val isEditing by viewModel.isEditing.collectAsState() // Boolean flag indicating edit mode

    // Get the current month and store necessary date formatters
    val currentMonth = remember { YearMonth.now() }// Stores the current month
    val daysOfWeek = remember { daysOfWeek() } // Gets a list of days in a week (Mon-Sun)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") } // Formatter for database storage
    val displayDateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") } // Formatter for UI display (e.g., "Feb 11")

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
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Display the current month and year
                Text(
                    text = calendarState.firstVisibleMonth.yearMonth.format(
                        DateTimeFormatter.ofPattern(
                            "MMMM yyyy"
                        )
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp)
                )
                // Display the days of the week headers (Mon-Sun)
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        val hasAvailability = tutorAvailability.any { it.day == formattedDate } // Check if day has availability
                        val hasBooking = bookedAppointments.any { it.day == formattedDate } // Check if day has a booking
                        val isSelected = selectedDay == formattedDate // Check if day is selected

                        Box(
                            modifier = Modifier
                                .size(45.dp, 35.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    when {
                                        isSelected -> Color(0xfffad96e) // Yellow for selected day
                                        hasAvailability -> Color(0xFF06C59C) // Green if availability exists
                                        hasBooking -> Color(0xFFc183d4) // Pink if booked
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable {
                                    viewModel.selectDay(formattedDate) // Select day on click
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.date.dayOfMonth.toString(),
                                color = Color.White.takeIf { isSelected || hasAvailability || hasBooking }
                                    ?: Color.Black // Adjust text color based on background
                            )
                        }
                    }
                )
            }
        }

        // Surface for displaying selected day and action buttons
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 4.dp
        ) {
            // Row for selected day text and buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Display selected day in formatted text (e.g., "Feb 11")
                selectedDay?.let { day ->
                    val displayDate =
                        LocalDate.parse(day, dateFormatter).format(displayDateFormatter)
                    Text(
                        text = displayDate,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f) // Take up remaining space
                    )
                }

                // Action buttons (Edit, Save, Cancel)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isEditing) {
                        // Display Save and Cancel buttons in edit mode
                        Button(
                            onClick = { viewModel.saveAvailability() },
                            modifier = Modifier.padding(end = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54A4FF))
                        ) {
                            Text(text = "Save")
                        }
                        Button(
                            onClick = { viewModel.cancelEdit() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54A4FF))
                        ) {
                            Text(text = "Cancel")
                        }
                    } else {
                        // Display Edit button in view mode
                        Button(
                            onClick = { viewModel.toggleEditMode() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06C59C))
                        ) {
                            Text(text = "Edit")
                        }
                    }
                }
            }
        }

        // Surface for displaying time slots and selection boxes
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 4.dp
        ) {
            // Availability selection
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Box to define the height of the availability selection area
                Box(modifier = Modifier.height(400.dp))
                {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                        //.weight(1f)
                    ) {
                        // Iterate through the list of time intervals from the ViewModel
                        items(viewModel.timeIntervals) { timeSlot ->
                            // Row layout to display each time slot with a selection box
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Card(
                                    modifier = Modifier.width(50.dp),
                                    colors = CardColors(
                                        disabledContentColor = Color.Black,
                                        containerColor = Color.White,
                                        contentColor = Color.Black,
                                        disabledContainerColor = Color.White
                                    )
                                ) {
                                    Text(text = timeSlot)
                                }

                                // Check if the time slot is selected
                                val isSelected = selectedTimeSlots.contains(timeSlot)

                                // Check if the time slot is booked for the selected day
//                                val isBooked = bookedAppointments.any { it.day == selectedDay && it.timeSlots.contains(timeSlot) }
                                val isBooked = selectedDay?.let { day ->
                                    bookedAppointments.any { it.day == day && it.timeSlots.contains(timeSlot) }
                                } ?: false

                                // Clickable Box to act as a selectable time slot
                                Box(
                                    modifier = Modifier
                                        .height(50.dp)
                                        .fillMaxWidth()
                                        .background(when {
                                            isBooked -> Color(0xFFc183d4) // Pink if booked
                                            isSelected && isEditing -> Color(0xFF54A4FF) // Blue when selected in edit mode
                                            isSelected -> Color(0xFF06C59C) // Green when selected in view mode
                                            else -> Color.White // Default white background when unselected
                                        })
                                        .clickable(enabled = isEditing && !isBooked) { // Clickable only when in edit mode and not booked
                                            viewModel.toggleTimeSlotSelection(timeSlot) // Toggles selection state
                                        }
                                )
                            }

                            // Separator Row to visually divide time slots (thin white line)
                            Row {
                                // This box is redundant currently but takes up space underneath the time slot text
                                // Useful if we want to change the line colors between the text and selection boxes
                                Box(
                                    modifier = Modifier
                                        .height(1.dp)
                                        .width(50.dp)
                                        .background(Color.White)
                                )
                                Box(
                                    modifier = Modifier
                                        .height(1.dp)
                                        .fillMaxWidth()
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Data model for storing tutor availability
data class TutorAvailability(
    val day: String = "", // Selected day
    val timeSlots: List<String> = emptyList() // List of available time slots for that day
)
