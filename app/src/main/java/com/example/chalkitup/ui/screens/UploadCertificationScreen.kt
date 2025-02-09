package com.example.chalkitup.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chalkitup.ui.viewmodel.CertificationViewModel

// functionality can be moved to signup page instead of having seperate screen

@Composable
fun UploadCertificationScreen(
    viewModel: CertificationViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addSelectedFiles(uris)
        }
    }

    val selectedFiles by viewModel.selectedFiles.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Upload Certifications", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        // Button to select files
        Button(onClick = { launcher.launch("*/*") }) {
            Text("Select Files")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display selected files
        if (selectedFiles.isNotEmpty()) {
            Text(text = "Selected Files:", style = MaterialTheme.typography.titleMedium)

            LazyColumn (modifier = Modifier
                .weight(1f)) {
                items(selectedFiles) { uri ->
                    val fileName = viewModel.getFileNameFromUri(context, uri)
                    SelectedFileItem(fileName = fileName, onRemove = { viewModel.removeSelectedFile(uri) })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button to upload selected files
            Button(onClick = {
                navController.navigate("checkEmail")
                viewModel.uploadFiles(context)
            })
            {
                Text("Done")
            }
        } else {
            Text(text = "No files selected.", color = Color.Gray)
        }

    }
}

@Composable
fun SelectedFileItem(fileName: String, onRemove: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.MailOutline, contentDescription = "Document")

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = fileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { onRemove() }) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove File")
            }
        }
    }
}
