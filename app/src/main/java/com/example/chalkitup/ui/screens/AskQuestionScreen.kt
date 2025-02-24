package com.example.chalkitup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun AskQuestionScreen(
    navController: NavController
) {
    val testList = listOf("CMPT 211", "CMPT 305", "CMPT 101", "CMPT 200", "CMPT 103", "CMPT 201")
    var selectedOption by remember { mutableStateOf("") }
    var questionInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ask a new question",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Blue
        )

        // Tutor selection
        Text("Which tutor would you like to ask a question to?",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Left
        )

        // Course selection with selected tutor
        Text("What course do you need help with?",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Left
        )

        DropDown(
            options = testList,
            label = "Select a course",
            onItemSelected = { selectedOption = it }
        )

        // Text Field to select type of question


        // Field to input question
        Text("What question do you have?",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Left
        )
        OutlinedTextField(
            value = questionInput, // Bind the state to the text field
            onValueChange = { questionInput = it }, // Update the state when user types
            label = { Text("Type your question here...") },
            modifier = Modifier.fillMaxWidth()
        )

        // Submit question button
        Button(
            onClick = { /* Handle submit */},
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedOption.isNotEmpty() // Disable submit button if nothing is selected
        ) {
            Text("Submit Question")
        }
    }
}


// Helper function: Dropdown menu for any list parameter
@Composable
fun DropDown(
     options: List<String>, // Full list of options
     label: String = "Select...",
     onItemSelected: (String) -> Unit // Callback when item is selected
) {
    var userInput by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    //val sortedOptions = options.sorted()
    val sortedOptions = options.sortedBy { it.split(" ")[1].toInt() }

    // Dynamically filter options based on input
    val filteredOptions = sortedOptions.filter {
        it.contains(userInput, ignoreCase = true) || userInput.isEmpty()
    }


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Column {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        // Collapse dropdown if OutlinedTextField loses focus
                        if (!focusState.isFocused) {
                            expanded = false
                        }
                    },
                value = userInput,
                onValueChange = {
                    userInput = it
                    expanded = true  // Keep dropdown open while typing
                },
                label = { Text(label) },
                trailingIcon = {
                    // Up icon when expanded & Down icon when collapsed
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Dropdown Icon",
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                }
            )

            // Unfiltered/filtered suggestions
            if (expanded && filteredOptions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp) // Height limit of dropdown
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                ) {
                    items(filteredOptions.size) { index ->
                        val option = filteredOptions[index]
                        Column {
                            Text(
                                text = option,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        userInput = option
                                        expanded = false    // Close dropdown
                                        onItemSelected(label)   // Notify parent about selection
                                    }
                                    .padding(16.dp)
                            )

                            // Add a divider between options
                            if (index < filteredOptions.size - 1) {
                                HorizontalDivider(
                                    color = Color.LightGray,
                                    thickness = 1.dp,
                                    //modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }

                    }
                }
            }
        }
    }

}