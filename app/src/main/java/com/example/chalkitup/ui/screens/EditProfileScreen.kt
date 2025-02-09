package com.example.chalkitup.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chalkitup.ui.viewmodel.EditProfileViewModel

@Composable
fun EditProfileScreen(navController: NavController, viewModel: EditProfileViewModel) {
    val userProfile by viewModel.userProfile.observeAsState()
    val isTutor by remember(userProfile) {
        derivedStateOf { userProfile?.userType == "Tutor" }
    }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedSubjects by remember { mutableStateOf(listOf<String>()) }
    var selectedGrades by remember { mutableStateOf(listOf<Int>()) }

    LaunchedEffect(userProfile) {
        userProfile?.let {
            firstName = it.firstName
            lastName = it.lastName
            email = it.email
            selectedSubjects = it.subjects
            selectedGrades = it.grades
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Edit Profile")

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") }
        )

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") }
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            enabled = false // Prevent email from being edited
        )

        // Tutor-Specific Fields
        if (isTutor) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Subjects You Teach")

            MultiSelectDropdown(
                availableOptions = listOf("Math", "Science", "English", "History", "Physics"),
                selectedOptions = selectedSubjects,
                onSelectionChange = { selectedSubjects = it }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Grade Levels")

            MultiSelectDropdown(
                availableOptions = (1..12).map { it.toString() },
                selectedOptions = selectedGrades.map { it.toString() },
                onSelectionChange = { selectedGrades = it.map { it.toInt() } }
            )
        } else {
            // Student-Specific Fields

        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(onClick = {
                viewModel.updateProfile(firstName, lastName, selectedSubjects, selectedGrades)
                navController.popBackStack() // Navigate back
            }) {
                Text("Save Changes")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = { navController.popBackStack() }) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun MultiSelectDropdown(
    availableOptions: List<String>,
    selectedOptions: List<String>,
    onSelectionChange: (List<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = selectedOptions.joinToString(", ")

    Column {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            label = { Text("Select") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            availableOptions.forEach { option ->
                val isSelected = option in selectedOptions
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null // Handled by menu item click
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option)
                        }
                    },
                    onClick = {
                        val newSelection = if (isSelected) {
                            selectedOptions - option
                        } else {
                            selectedOptions + option
                        }
                        onSelectionChange(newSelection)
                    }
                )
            }
        }
    }
}



