package com.example.bloodbank.domain.model

import java.util.UUID

/**
 * ChatMessage
 *
 * Represents a single message in a chat channel between a Donor and Recipient.
 */
data class ChatMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    // Required empty constructor for Firebase Realtime Database
    constructor() : this("", "", "", 0L)
}
