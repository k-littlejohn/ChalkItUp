package com.example.chalkitup.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.chalkitup.R


@Composable
fun ProfilePictureIcon(
    profilePictureUrl: String?
) {
    if (profilePictureUrl.isNullOrEmpty()) {
        // Display default avatar
        Image (
            painter = painterResource(id = R.drawable.chalkitup),
            contentDescription = "Default Profile Picture",
            modifier = Modifier
                .size(40.dp)
                .border(1.dp, Color.Gray, CircleShape)
                .clip(CircleShape)
        )
    } else {
        // Display user's profile picture
        AsyncImage(
            model = profilePictureUrl,
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(40.dp)
                .border(1.dp, Color.Gray, CircleShape)
                .clip(CircleShape)
        )
    }
}