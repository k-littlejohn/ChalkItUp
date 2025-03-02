package com.example.chalkitup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Composable function for displaying and managing the Subject, Grade, and Specialization selection for a tutor.
 *
 * This component allows a tutor to select their teaching subject, grade level, and specialization (if applicable),
 * and handles the visual state for errors, dropdown menus, and the removal of the selection.
 *
 * Current usage: EditProfileScreen, SignupScreen, ProfileScreen
 *
 * @param tutorSubject The current tutor's subject, grade, and specialization details.
 * @param availableSubjects The list of available subjects for the tutor to choose from.
 * @param availableGradeLevels The list of available grade levels for general subjects.
 * @param availableGradeLevelsBPC The list of available grade levels for Biology, Chemistry, and Physics.
 * @param grade10Specs The list of specializations available for grade 10.
 * @param grade1112Specs The list of specializations available for grades 11 and 12.
 * @param onSubjectChange A callback function to handle subject selection change.
 * @param onGradeChange A callback function to handle grade level selection change.
 * @param onSpecChange A callback function to handle specialization selection change.
 * @param onRemove A callback function to handle removal of the subject-grade-specialization selection.
 * @param subjectError A boolean flag indicating if there's an error with the subject selection.
 * @param gradeError A boolean flag indicating if there's an error with the grade level selection.
 * @param specError A boolean flag indicating if there's an error with the specialization selection.
 * @param priceError A boolean flag indicating if there's an error with the price selection.
 */
@Composable
fun SubjectGradeItem(
    tutorSubject: TutorSubject,
    availableSubjects: List<String>,
    availableGradeLevels: List<String>,
    availableGradeLevelsBPC: List<String>,
    availablePrice: List<String>,
    grade10Specs: List<String>,
    grade1112Specs: List<String>,
    onSubjectChange: (String) -> Unit,
    onGradeChange: (String) -> Unit,
    onSpecChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onRemove: () -> Unit,
    subjectError: Boolean,
    gradeError: Boolean,
    specError: Boolean,
    priceError: Boolean
) {
    // State variables to control the visibility of dropdown menus for subject, grade, and specialization
    val scrollState = rememberScrollState() // Main form scroll state - entire screen
    val termsScrollState = rememberScrollState() // Terms & Conditions scroll state - inside Terms and Cond. box
    var expandedSubject by remember { mutableStateOf(false) }
    var expandedGrade by remember { mutableStateOf(false) }
    var expandedSpec by remember { mutableStateOf(false) }
    var expandPrice by remember { mutableStateOf(false) }

    // Define colors for the buttons based on their state (selected, error, default)
    val selectedButtonColor = Color(0xFF54A4FF)
    val defaultButtonColor = Color.LightGray
    val errorButtonColor = Color.Red

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Subject Selection Button and Dropdown
        Box(modifier = Modifier.weight(3.5f)) {
            Button(
                onClick = { expandedSubject = true }, // Show the subject dropdown when clicked
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        tutorSubject.subject.isNotEmpty() -> selectedButtonColor
                        subjectError -> errorButtonColor
                        else -> defaultButtonColor
                    }
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = tutorSubject.subject.ifEmpty { "Subject" }, fontSize = 14.sp)
            }

            // Subject dropdown menu
            DropdownMenu(
                expanded = expandedSubject,
                onDismissRequest = { expandedSubject = false },
                shadowElevation = 0.dp,
                containerColor = Color.Transparent,
                modifier = Modifier.width(125.dp)
            ) {
                availableSubjects.forEach { subj -> // Iterate through available subjects
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .shadow(
                                6.dp,
                                shape = RoundedCornerShape(8.dp),
                                clip = true
                            ) // Apply shadow properly
                            .background(Color.White, shape = RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text(subj) },
                            onClick = {
                                onSubjectChange(subj) // Update the subject when a selection is made
                                onGradeChange("") // Reset grade and specialization when subject changes
                                onSpecChange("")
                                onPriceChange("")
                                expandedSubject = false // Close the dropdown
                            },
                            //modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp)
                        )
                    }
                }
            }

        }

        Spacer(modifier = Modifier.width(8.dp))

        // Grade Level Selection Button and Dropdown
        Box(modifier = Modifier.weight(1.8f)) {
            Button(
                onClick = { expandedGrade = true }, // Show the grade dropdown when clicked
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        tutorSubject.grade.isNotEmpty() -> selectedButtonColor
                        gradeError -> errorButtonColor
                        else -> defaultButtonColor
                    }
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = tutorSubject.grade.ifEmpty { "Gr" }, fontSize = 14.sp)
            }

            // Determine the list of available grade levels based on the subject
            val gradeList = when (tutorSubject.subject) {
                "Biology", "Chemistry", "Physics" -> availableGradeLevelsBPC
                else -> availableGradeLevels
            }

            // Grade dropdown menu
            DropdownMenu(
                expanded = expandedGrade,
                onDismissRequest = { expandedGrade = false },
                shadowElevation = 0.dp,
                containerColor = Color.Transparent,
                modifier = Modifier.width(75.dp)
            ) {
                gradeList.forEach { grade -> // Iterate through available grade levels
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .shadow(
                                6.dp,
                                shape = RoundedCornerShape(8.dp),
                                clip = true
                            ) // Apply shadow properly
                            .background(Color.White, shape = RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text(grade) },
                            onClick = {
                                onGradeChange(grade) // Update the grade when a selection is made
                                onSpecChange("") // Reset specialization when grade level changes
                                expandedGrade = false // Close the dropdown
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Specialization Selection Button and Dropdown (only for grades 10, 11, or 12)
        if (tutorSubject.grade == "10" || tutorSubject.grade == "11" || tutorSubject.grade == "12") {
            // Specialization Selection Button
            Box(modifier = Modifier.weight(2.9f)) {
                Button(
                    onClick = {
                        expandedSpec = true
                    }, // Show the specialization dropdown when clicked
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            tutorSubject.specialization.isNotEmpty() -> selectedButtonColor
                            specError -> errorButtonColor
                            else -> defaultButtonColor
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = tutorSubject.specialization.ifEmpty { "Level" }, fontSize = 14.sp)
                }

                // Determine the list of available specializations based on the grade
                val specList = when (tutorSubject.grade) {
                    "10" -> grade10Specs
                    "11", "12" -> grade1112Specs
                    else -> emptyList() // No specializations for non-grade 10-12
                }

                // Specialization dropdown menu
                DropdownMenu(
                    expanded = expandedSpec,
                    onDismissRequest = { expandedSpec = false },
                    shadowElevation = 0.dp,
                    containerColor = Color.Transparent,
                    modifier = Modifier.width(120.dp)
                ) {
                    specList.forEach { spec -> // Iterate through available specializations
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .shadow(
                                    6.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    clip = true
                                ) // Apply shadow properly
                                .background(Color.White, shape = RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text(spec) },
                                onClick = {
                                    onSpecChange(spec) // Update the specialization when a selection is made
                                    expandedSpec = false // Close the dropdown
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Box(modifier = Modifier.weight(2.9f)) {} // Empty box when no specialization is available
            onSpecChange("") // Reset specialization if not applicable
        }
    }

    //--------------------------price start-----------------
    Column()
        {
        Row(
            modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically)
        {
            Box(modifier = Modifier.weight(3.5f)) {

                Button(
                    onClick = { expandPrice = true }, // Show the subject dropdown when clicked
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            tutorSubject.price.isNotEmpty() -> selectedButtonColor
                            priceError -> errorButtonColor
                            else -> defaultButtonColor
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = tutorSubject.price.ifEmpty { "Price" }, fontSize = 14.sp)
                }

                DropdownMenu(
                    expanded = expandPrice,
                    onDismissRequest = { expandPrice = false },
                    shadowElevation = 0.dp,
                    containerColor = Color.Transparent,
                    modifier = Modifier.width(75.dp)
                ) {
                    availablePrice.forEach { price -> // Iterate through available grade levels
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .shadow(
                                    6.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    clip = true
                                ) // Apply shadow properly
                                .background(Color.White, shape = RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text(price) },
                                onClick = {
                                    onPriceChange(price) // Update the price when a selection is made
                                    expandPrice = false // Close the dropdown
                                }
                            )
                        }
                    }
                }
            }

            ///------------------------------price fin

            // Remove Button to delete the subject-grade-specialization item
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove Subject",
                    tint = Color.Gray
                )
            }
        }
    }
}

/**
 * Data class representing a subject, grade, and specialization that a tutor can offer.
 *
 * This class holds information about a tutor's subject, grade level, and specialization
 * for a specific grade.
 */
data class TutorSubject(
    val subject: String = "",
    val grade: String = "",
    val specialization: String = "",
    val price: String=""
)

/**
 * Data class to represent errors in tutor subject details.
 *
 * This class contains flags for errors related to subject, grade, and specialization.
 */
data class TutorSubjectError(
    val subjectError: Boolean,
    val gradeError: Boolean,
    val specError: Boolean,
    val priceError: Boolean
)

/**
 * Validates a list of tutor subjects to check for missing or incorrect information.
 *
 * This function checks if each tutor subject has valid subject, grade, and specialization details.
 * If any of these fields are missing or invalid, an error flag is set.
 *
 * @param tutorSubjects The list of tutor subjects to validate.
 * @return A list of TutorSubjectError objects representing validation errors for each subject.
 */
fun validateTutorSubjects(tutorSubjects: List<TutorSubject>): List<TutorSubjectError> {
    return tutorSubjects.map { subject ->
        TutorSubjectError(
            subjectError = subject.subject.isEmpty(),
            gradeError = subject.grade.isEmpty(),
            priceError = subject.price.isEmpty(),
            specError = (subject.grade in listOf(
                "10",
                "11",
                "12"
            ) && subject.specialization.isEmpty())

        )
    }
}