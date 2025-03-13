package com.example.chalkitup.domain.model;

data class User(
    val id: String = "", // Firestore document ID
    val firstName: String = "",
    val lastName: String = "",
    val userType: String = "",
    var userProfilePictureUrl: String = ""
)