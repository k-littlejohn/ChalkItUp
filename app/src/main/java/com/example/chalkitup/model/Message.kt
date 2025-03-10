package com.example.chalkitup.model


data class Message(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0
)
