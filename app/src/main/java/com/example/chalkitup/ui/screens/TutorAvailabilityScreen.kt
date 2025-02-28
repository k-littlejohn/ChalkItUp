package com.example.chalkitup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.chalkitup.ui.viewmodel.TutorAvailabilityViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Displays the tutor's availability selection screen.
 *
 * This function presents a calendar interface where tutors can select available
 * time slots for specific days. Tutors can click on a day to open a dialog
 * displaying available time slots from <9 AM to 9 PM> in 30-minute increments.
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

    // Collects the list of tutor availability from the ViewModel
    val tutorAvailability by viewModel.tutorAvailabilityList.collectAsState()
    // Collects the currently selected day
    val selectedDay by viewModel.selectedDay.collectAsState()
    // Collects the currently selected time slots for the selected day
    val selectedTimeSlots by viewModel.selectedTimeSlots.collectAsState()

    // Get the current month for calendar display
    val currentMonth = remember { YearMonth.now() }
    // Get the days of the week for calendar headers
    val daysOfWeek = remember { daysOfWeek() }
    // Formatter for displaying dates in "yyyy-MM-dd" format
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    // State for handling calendar display
    val calendarState = rememberCalendarState(
        startMonth = currentMonth.minusMonths(0),
        endMonth = currentMonth.plusMonths(0),
        firstVisibleMonth = currentMonth,
    )

    // State to control the visibility of the availability selection dialog
    var showDialog by remember { mutableStateOf(false) }

    //------------------------------VARIABLES-END---------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display the current month in the header
        Text(
            text = calendarState.firstVisibleMonth.yearMonth.format(
                DateTimeFormatter.ofPattern(
                    "MMMM yyyy"
                )
            ),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        // Display the abbreviated days of the week as headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day.name.take(3), // Display first 3 letters (Mon, Tue, etc.)
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray
                )
            }
        }

        // Horizontal calendar with selectable days
        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                val formattedDate = day.date.format(dateFormatter)
                val hasAvailability = tutorAvailability.any { it.day == formattedDate }

                // Display the day inside a selectable box
                Box(
                    modifier = Modifier
                        .size(45.dp, 35.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                hasAvailability -> Color(0xFF06C59C) // Green if availability exists
                                else -> Color.Transparent
                            }
                        )
                        .clickable {
                            viewModel.selectDay(day.date.format(dateFormatter))
                            showDialog = true // Open time slot selection dialog
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        color = Color.White.takeIf { hasAvailability }
                            ?: Color.Black
                    )
                }
            }
        )

        // Dialog for selecting time slots for the chosen day
        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxHeight(0.75f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        selectedDay?.let { day ->
                            // Display selected day
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = day, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }

                            // List of available time slots
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f) // Take up remaining space
                            ) {
                                items(viewModel.timeIntervals) { timeSlot ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Display time slot label
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

                                        // Determine if the time slot is selected
                                        val isSelected = selectedTimeSlots.contains(timeSlot)

                                        // Clickable box to select/deselect time slots
                                        Box(
                                            modifier = Modifier
                                                .height(50.dp)
                                                .fillMaxWidth()
                                                .background(if (isSelected) Color(0xFF54A4FF) else Color.White)
                                                .clickable {
                                                    viewModel.toggleTimeSlotSelection(
                                                        timeSlot
                                                    )
                                                }
                                        )
                                    }

                                    // Separator line between time slots
                                    Row {
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
                                                .background(Color(0xFF54A4FF))
                                        )
                                    }
                                }
                            }
                            // Save and Cancel buttons
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.saveAvailability() // Save selected slots to Firestore
                                        showDialog = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06C59C)),
                                    ) {
                                    Text(text = "Save")
                                }

                                Button(
                                    onClick = {
                                        showDialog = false // Close dialog without saving
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06C59C)),
                                    ) {
                                    Text(text = "Cancel")
                                }
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
