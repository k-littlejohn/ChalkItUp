package com.example.chalkitup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chalkitup.ui.components.validateTutorSubjects
import com.example.chalkitup.ui.components.SessionClassInfo
import com.example.chalkitup.ui.components.SubjectGradeItemNoPrice
import com.example.chalkitup.ui.components.TutorSubject
import com.example.chalkitup.ui.components.TutorSubjectError
import com.example.chalkitup.ui.viewmodel.BookingViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

@Composable
fun BookingScreen(
    navController: NavController,
    viewModel: BookingViewModel
) {
    //------------------------------VARIABLES----------------------------------------------

    // Scroll state for the entire screen
    val scrollState = rememberScrollState()

    // Lists for subject and grade level selections.
    val availableSubjects = listOf("Math", "Science", "English", "Social", "Biology", "Physics", "Chemistry")
    val availableGradeLevels = listOf("7", "8", "9", "10", "11", "12")
    val availableGradeLevelsBPC = listOf("11", "12")
    val grade10Specs = listOf("- 1", "- 2", "Honours")
    val grade1112Specs = listOf("- 1", "- 2", "AP", "IB")

    var userSubjects by remember {
        mutableStateOf(
            listOf(TutorSubject("", "", "", "inBooking")) // Default list with one item
        )
    } // To store selected subject

    // State to track errors in tutor subject selections.
    //var subjectError by remember { mutableStateOf(false) }      // Tracks Empty Field
    var userSubjectErrors by remember { mutableStateOf<List<TutorSubjectError>>(emptyList()) }

    var sessionClassInfo by remember { mutableStateOf<List<SessionClassInfo>>(emptyList()) }

    var priceRange by remember { mutableStateOf(20f..60f) } // Default price range

    // State for week navigation and day selection
    val currentMonth = LocalDate.now().monthValue
    var selectedWeekStart by remember { mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) }
    val daysInWeek = (0..6).map { selectedWeekStart.plusDays(it.toLong()) }

    // ViewModel States
    val selectedDay by viewModel.selectedDay.collectAsState()
    val selectedStartTime by viewModel.selectedStartTime.collectAsState()
    val selectedEndTime by viewModel.selectedEndTime.collectAsState()
    val availability by viewModel.availability.collectAsState()
    //val tutorAvailabilityMap by viewModel.tutorAvailabilityMap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var comments by remember { mutableStateOf("") }
    var sessionType by remember { mutableStateOf("In-Person") } // Default to In-Person

    // Container for the subject selection.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White)
            .verticalScroll(scrollState)
            .border(1.dp, Color.Gray.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)), // Light border
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        Text(
//            text = "Booking Options",
//            textAlign = TextAlign.Center,
//            style = MaterialTheme.typography.headlineMedium,
//            fontWeight = FontWeight.Bold,
//            modifier = Modifier
//                .padding(8.dp)
//        )
        Text(
            text = "Select the subject and price range for your session",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(8.dp)
        )
//        --may want to keep to add more subjects per session in the future
//        --would need to account for the remove button ^^
//        // Button to add subjects to the students selection list.
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier
//                .padding(8.dp)
//        ) {
//                IconButton(
//                    onClick = {
//                        // Add an empty tutor subject entry.
//                        subjectError = false
//                        userSubjects =
//                            userSubjects + TutorSubject("", "", "", "inBooking") // Add empty entry
//                    },
//                    modifier = Modifier.size(36.dp),
//                    colors = IconButtonColors(
//                        Color(0xFF06C59C),
//                        contentColor = Color.White,
//                        disabledContainerColor = Color(0xFF06C59C),
//                        disabledContentColor = Color.White
//                    )
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Add,
//                        contentDescription = "Add Subject",
//                        tint = Color.White
//                    )
//                }
//        }

        Text(
            text = "Subject",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        // Display list of selected subjects and their grade levels.
        Box(modifier = Modifier.heightIn(20.dp, 500.dp)) {
            LazyColumn {
                itemsIndexed(userSubjects) { index, tutorSubject ->
                    // Display each tutor subject item and its details.
                    SubjectGradeItemNoPrice(
                        tutorSubject = tutorSubject,
                        availableSubjects = availableSubjects,
                        availableGradeLevels = availableGradeLevels,
                        availableGradeLevelsBPC = availableGradeLevelsBPC,
                        grade10Specs = grade10Specs,
                        grade1112Specs = grade1112Specs,
                        onSubjectChange = { newSubject ->
                            userSubjects = userSubjects.toMutableList().apply {
                                this[index] = this[index].copy(subject = newSubject)
                            }
                        },
                        onGradeChange = { newGrade ->
                            userSubjects = userSubjects.toMutableList().apply {
                                this[index] = this[index].copy(grade = newGrade)
                            }
                        },
                        onSpecChange = { newSpec ->
                            userSubjects = userSubjects.toMutableList().apply {
                                this[index] = this[index].copy(specialization = newSpec)
                            }
                        },
                        subjectError = userSubjectErrors.getOrNull(index)?.subjectError
                            ?: false,
                        gradeError = userSubjectErrors.getOrNull(index)?.gradeError
                            ?: false,
                        specError = userSubjectErrors.getOrNull(index)?.specError
                            ?: false,
                    )
                }
            }
        }

        // Price Range Slider
        Text(
            text = "Price Range: \$${priceRange.start.toInt()} - \$${priceRange.endInclusive.toInt()}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        // Range Slider with step increments of 5
        RangeSlider(
            value = priceRange,
            onValueChange = { newRange ->
                // Snap values to the nearest multiple of 5
                val roundedStart = (newRange.start / 5).roundToInt() * 5f
                val roundedEnd = (newRange.endInclusive / 5).roundToInt() * 5f
                priceRange = roundedStart..roundedEnd
            },
            valueRange = 20f..120f,
            steps = (120 / 5) - 1, // Ensures increments of 5
            onValueChangeFinished = {
                // Handle price range selection if needed
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Min/Max labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "\$20")
            Text(text = "\$120")
        }

        // Comments Box
        Text(
            text = "Additional Notes (Optional)",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 16.dp)
        )
        TextField(
            value = comments,
            onValueChange = { comments = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            placeholder = { Text("Enter any additional notes here...") }
        )

        // Session Type Selection
        Text(
            text = "Session Type",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = sessionType == "In-Person",
                    onClick = { sessionType = "In-Person" }
                )
                Text(text = "In-Person", modifier = Modifier.padding(start = 4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = sessionType == "Online",
                    onClick = { sessionType = "Online" }
                )
                Text(text = "Online", modifier = Modifier.padding(start = 4.dp))
            }
        }

        Button(
            onClick = {
                // Validate the selected subject
                val errors = validateTutorSubjects(userSubjects)
                userSubjectErrors = errors

                // Check if there are any errors
                val hasErrors = errors.any { it.subjectError || it.gradeError || it.specError }

                if (!hasErrors) {
                    // If no errors, set the subject in the ViewModel
                    val selectedSubject = userSubjects.firstOrNull()
                    if (selectedSubject != null) {
                        viewModel.setSubject(selectedSubject, priceRange)
                    }
                }
            },
            modifier = Modifier
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF06C59C),
                contentColor = Color.White
            )
        ) {
            Text("Continue")
        }

        // Week Navigation and Day Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val previousWeekStart = selectedWeekStart.minusWeeks(1)
                    if (previousWeekStart.monthValue == currentMonth) {
                        selectedWeekStart = previousWeekStart
                    }
                }
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous Week")
            }

            IconButton(
                onClick = {
                    val nextWeekStart = selectedWeekStart.plusWeeks(1)
                    if (nextWeekStart.monthValue == currentMonth) {
                        selectedWeekStart = nextWeekStart
                    }
                }
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Week")
            }
        }

        Row {
            daysInWeek.forEach { day ->
                DaySquare(
                    day = day,
                    isSelected = day == selectedDay,
                    onClick = { viewModel.selectDay(day) }
                )
            }
        }

        // Display Available Start Times
        selectedDay?.let { day ->
            val availableTimes = availability[day] ?: emptyList()
            println("Available times for $day: $availableTimes")

            if (availableTimes.isNotEmpty()) {
                LazyRow {
                    items(availableTimes) { time ->
                        TimeChip(
                            time = time.toString(),
                            isSelected = time == selectedStartTime,
                            onClick = { viewModel.selectStartTime(time) }
                        )
                    }
                }
            } else {
                Text(
                    text = "No availability for this day",
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Display Available End Times
        selectedStartTime?.let { startTime ->
            val validEndTimes = viewModel.getValidEndTimes(startTime, availability[selectedDay] ?: emptyList())
            println("Valid end times for $startTime: $validEndTimes")

            if (validEndTimes.isNotEmpty()) {
                LazyRow {
                    items(validEndTimes) { time ->
                        TimeChip(
                            time = time.toString(),
                            isSelected = time == selectedEndTime,
                            onClick = { viewModel.selectEndTime(time) }
                        )
                    }
                }
            } else {
                Text(
                    text = "No valid end times available",
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Submit Button
        Button(
            onClick = {
                if (selectedDay != null && selectedStartTime != null && selectedEndTime != null) { // and subject != null
                    viewModel.matchTutorForTimeRange(selectedDay!!,
                        selectedStartTime!!, selectedEndTime!!
                    ) { matchedTutorId ->
                        if (matchedTutorId != null) {
                            // Proceed with booking
                            val selectedSubject = userSubjects.firstOrNull()
                            viewModel.submitBooking(matchedTutorId, comments, sessionType, selectedSubject!!)
                        } // handle no match error
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            enabled = selectedDay != null && selectedStartTime != null && selectedEndTime != null
        ) {
            Text("Submit Booking")
        }

        // Loading and Error Handling
        if (isLoading) {
            CircularProgressIndicator()
        }
        error?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
@Composable
fun DaySquare(day: LocalDate, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(if (isSelected) Color(0xFF06C59C) else Color.LightGray)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = day.dayOfMonth.toString(), color = if (isSelected) Color.White else Color.Black)
    }
}

@Composable
fun TimeChip(time: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .background(
                if (isSelected) Color(0xFF06C59C) else Color.LightGray,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = time, color = if (isSelected) Color.White else Color.Black)
    }
}