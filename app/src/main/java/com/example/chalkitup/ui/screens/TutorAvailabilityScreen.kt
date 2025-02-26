package com.example.chalkitup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun EnterTutorAvailability(
    navController: NavController,
    viewModel: TutorAvailabilityViewModel
) {
    val tutorAvailability by viewModel.tutorAvailabilityList.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val selectedTimeSlots by viewModel.selectedTimeSlots.collectAsState()

    val currentMonth = remember { YearMonth.now() }
    val daysOfWeek = remember { daysOfWeek() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    val calendarState = rememberCalendarState(
        startMonth = currentMonth.minusMonths(0),
        endMonth = currentMonth.plusMonths(0),
        firstVisibleMonth = currentMonth,
    )

    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = calendarState.firstVisibleMonth.yearMonth.format(
                DateTimeFormatter.ofPattern(
                    "MMMM yyyy"
                )
            ),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

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
                val hasAvailability = tutorAvailability.any { it.day == formattedDate }

                Box(
                    modifier = Modifier
                        .size(45.dp, 35.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                hasAvailability -> Color(0xFF06C59C)  // Availability added color
                                else -> Color.Transparent
                            }
                        )
                        .clickable {
                            viewModel.selectDay(day.date.format(dateFormatter))
                            //showCalendar = false
                            showDialog = true
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

        // button for edit availability -> view/edit mode

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
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        selectedDay?.let { day ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = day, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f) // Take up remaining space
                            ) {
                                items(viewModel.timeIntervals) { timeSlot ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
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

                                        val isSelected = selectedTimeSlots.contains(timeSlot)
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
                                    Row {
                                        Box(
                                            modifier = Modifier
                                                .height(2.dp)
                                                .width(50.dp)
                                                .background(Color.White)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .height(2.dp)
                                                .fillMaxWidth()
                                                .background(Color(0xFF54A4FF))
                                        )
                                    }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.saveAvailability()
                                        showDialog = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Save")
                                }

                                Button(
                                    onClick = {
                                        showDialog = false
                                    },
                                    modifier = Modifier.weight(1f)
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

data class TutorAvailability(
    val day: String = "",  // Day of the month
    val timeSlots: List<String> = emptyList() // Multiple time slots for each day
)
