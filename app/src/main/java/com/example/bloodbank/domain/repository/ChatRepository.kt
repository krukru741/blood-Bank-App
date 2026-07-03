package com.example.bloodbank.domain.repository

import com.example.bloodbank.domain.model.ChatChannel
import com.example.bloodbank.domain.model.ChatMessage
import com.example.bloodbank.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * ChatRepository
 *
 * Contract for managing real-time chat between Donors and Recipients
 * using Firebase Realtime Database.
 */
interface ChatRepository {
    
    /**
     * Sends a message to a specific channel.
     */
    fun sendMessage(channelId: String, message: ChatMessage): Flow<Resource<Unit>>
    
    /**
     * Observes real-time messages for a specific channel.
     */
    fun observeMessages(channelId: String): Flow<Resource<List<ChatMessage>>>
    
    /**
     * Initializes a ChatChannel if it doesn't exist, linking the donor and recipient
     * to a specific blood request.
     */
    fun getOrCreateChannel(
        requestId: String,
        recipientId: String,
        donorId: String
    ): Flow<Resource<ChatChannel>>
}
