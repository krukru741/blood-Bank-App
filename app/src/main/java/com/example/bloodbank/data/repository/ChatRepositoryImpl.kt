package com.example.bloodbank.data.repository

import com.example.bloodbank.domain.model.ChatChannel
import com.example.bloodbank.domain.model.ChatMessage
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.repository.ChatRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : ChatRepository {

    private val chatsRef = database.getReference("chats")
    private val channelsRef = database.getReference("channels")

    override fun sendMessage(channelId: String, message: ChatMessage): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            // Push message to /chats/channelId/messages
            val messageRef = chatsRef.child(channelId).child("messages").child(message.messageId)
            messageRef.setValue(message).await()
            
            // Update channel last message snippet
            channelsRef.child(channelId).updateChildren(
                mapOf(
                    "lastMessage" to message.text,
                    "lastMessageTime" to message.timestamp
                )
            ).await()
            
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }

    override fun observeMessages(channelId: String): Flow<Resource<List<ChatMessage>>> = callbackFlow {
        trySend(Resource.Loading)
        
        val messagesRef = chatsRef.child(channelId).child("messages")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val msg = child.getValue(ChatMessage::class.java)
                    if (msg != null) {
                        messages.add(msg)
                    }
                }
                // Sort by timestamp
                messages.sortBy { it.timestamp }
                trySend(Resource.Success(messages))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(error.toException())))
            }
        }
        
        messagesRef.addValueEventListener(listener)
        
        awaitClose {
            messagesRef.removeEventListener(listener)
        }
    }

    override fun getOrCreateChannel(
        requestId: String,
        recipientId: String,
        donorId: String
    ): Flow<Resource<ChatChannel>> = flow {
        emit(Resource.Loading)
        try {
            val channelId = ChatChannel.generateId(requestId, donorId)
            val channelSnapshot = channelsRef.child(channelId).get().await()
            
            if (channelSnapshot.exists()) {
                val channel = channelSnapshot.getValue(ChatChannel::class.java)
                if (channel != null) {
                    emit(Resource.Success(channel))
                    return@flow
                }
            }
            
            // Channel doesn't exist or failed to parse, create new one
            val newChannel = ChatChannel(
                channelId = channelId,
                requestId = requestId,
                recipientId = recipientId,
                donorId = donorId
            )
            channelsRef.child(channelId).setValue(newChannel).await()
            emit(Resource.Success(newChannel))
            
        } catch (e: Exception) {
            emit(Resource.Error(com.example.bloodbank.data.error.FirebaseErrorMapper.mapToAppError(e)))
        }
    }
}
