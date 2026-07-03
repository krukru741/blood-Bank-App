package com.example.bloodbank.domain.model

/**
 * ChatChannel
 *
 * Represents a chat conversation context linking a specific Blood Request
 * with the donor and recipient. This allows multiple donors to chat with
 * a single recipient regarding the same blood request independently.
 */
data class ChatChannel(
    val channelId: String,
    val requestId: String,
    val recipientId: String,
    val donorId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null
) {
    // Required empty constructor for Firebase Realtime Database
    constructor() : this("", "", "", "")
    
    companion object {
        /** Generates a deterministic channel ID based on request and donor. */
        fun generateId(requestId: String, donorId: String): String {
            return "${requestId}_$donorId"
        }
    }
}
