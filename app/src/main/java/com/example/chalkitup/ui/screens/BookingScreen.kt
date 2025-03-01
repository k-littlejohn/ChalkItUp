package com.example.chalkitup.ui.screens

import android.graphics.drawable.Icon
import android.widget.Button
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.VectorProperty
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.chalkitup.ui.components.validateTutorSubjects
import java.time.format.DateTimeFormatter
import java.time.YearMonth
import com.example.chalkitup.R
import com.example.chalkitup.ui.viewmodel.AuthViewModel
import com.example.chalkitup.ui.viewmodel.CertificationViewModel
import com.example.chalkitup.ui.components.SelectedFileItem
import com.example.chalkitup.ui.components.SubjectGradeItem
import com.example.chalkitup.ui.components.SessionClassInfo
import com.example.chalkitup.ui.components.TutorSubject
import com.example.chalkitup.ui.components.TutorSubjectError
import com.example.chalkitup.ui.components.validateTutorSubjects
import java.util.Calendar


@Composable
fun BookingScreen(
    navController: NavController,
    certificationViewModel: CertificationViewModel,
    authViewModel: AuthViewModel,
)  {

    //------------------------------VARIABLES----------------------------------------------

    // Lists for subject and grade level selections.
    val availableSubjects = listOf("Math", "Science", "English", "Social", "Biology", "Physics", "Chemistry")
    val availableGradeLevels = listOf("7", "8", "9", "10", "11", "12")
    val availableGradeLevelsBPC = listOf("11", "12")
    val grade10Specs = listOf("- 1", "- 2", "Honours")
    val grade1112Specs = listOf("- 1", "- 2", "AP", "IB")
    val availablePrice = listOf("$20/hr", "$25/hr", "$30/hr", "$35/hr", "$40/hr", "$45/hr", "$50/hr", "$55/hr", "$60/hr", "$65/hr", "$70/hr", "$75/hr", "$80/hr", "$85/hr", "$90/hr", "$95/hr", "$100/hr", "$105/hr", "$110/hr", "$115/hr", "$120/hr")


    var userSubjects by remember { mutableStateOf<List<TutorSubject>>(emptyList()) } // To store selected subjects
    var sessionStartTime = String
    var sessionEndTime = String

    // State to track errors in tutor subject selections.
    var subjectError by remember { mutableStateOf(false) }      // Tracks Empty Field
    var userSubjectErrors by remember { mutableStateOf<List<TutorSubjectError>>(emptyList()) }

    var sessionClassInfo by remember { mutableStateOf<List<SessionClassInfo>>(emptyList()) }
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Chalk It Up")
    }

    // Container for the subject selection.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White)
            .border(1.dp, Color.Gray.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)), // Light border
        horizontalAlignment = Alignment.CenterHorizontally


    ){
        Text(
            text = "Booking Options",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(8.dp)
        )
        Text(
            text = "Please specify your subject and year / skill level"
                + " for each booking session. Then enter your available"
                + " start and end time as well.",
            textAlign = TextAlign.Left,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(8.dp)
        )

        // Button to add subjects to the students selection list.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(8.dp)
        ) {
            // Students can add only one subject per session.
            if (userSubjects.isEmpty()) {
                Text(
                    "Tap the  +  to add a subject",
                    color = Color.Gray
                )
            } else {
                Text(
                    "You cannot enter more than 1 subject",
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(50.dp))

            // Checks if a subject is added, if not the option to add one is displayed.
            if (userSubjects.isEmpty()) {
                // Button to add a new subject to the list.
                IconButton(
                    onClick = {
                        // Add an empty tutor subject entry.
                        subjectError = false
                        userSubjects =
                            userSubjects + TutorSubject("", "", "", "") // Add empty entry
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonColors(
                        Color(0xFF06C59C),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF06C59C),
                        disabledContentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Subject",
                        tint = Color.White
                    )
                }
            }
        }

        // Default text if no subjects have been added yet.
        if (userSubjects.isEmpty()) {
            Text(
                text = "No subject selected",
                color = Color.Gray
            )
        }

        // Display list of selected subjects and their grade levels.
        Box(modifier = Modifier.heightIn(20.dp, 500.dp)) {

            LazyColumn {
                itemsIndexed(userSubjects) { index, tutorSubject ->
                    // Display each tutor subject item and its details.
                    SubjectGradeItem( // SubjectGradeItem function is defined below.
                        tutorSubject = tutorSubject,
                        availableSubjects = availableSubjects,
                        availableGradeLevels = availableGradeLevels,
                        availableGradeLevelsBPC = availableGradeLevelsBPC,
                        availablePrice = availablePrice,
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
                        onPriceChange = { newPrice ->
                            userSubjects = userSubjects.toMutableList().apply {
                                this[index] = this[index].copy(price = newPrice)
                            }
                        },

                        onRemove = {
                            userSubjects =
                                userSubjects.toMutableList().apply { removeAt(index) }
                            userSubjectErrors = userSubjectErrors.toMutableList().apply {
                                if (index < size) removeAt(index)
                            }
                        },
                        subjectError = userSubjectErrors.getOrNull(index)?.subjectError
                            ?: false,
                        gradeError = userSubjectErrors.getOrNull(index)?.gradeError
                            ?: false,
                        specError = userSubjectErrors.getOrNull(index)?.specError
                            ?: false,
                        priceError= userSubjectErrors.getOrNull(index)?.priceError
                            ?:false


                    )
                }


            }

        }

    }

}
