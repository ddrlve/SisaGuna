package com.sisaguna.android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatPartyDto(val id: String, val name: String)

@Serializable
data class ChatLastMessageDto(val body: String = "", val side: String = "buyer", val at: String? = null)

@Serializable
data class ConversationDto(
    val id: String,
    val counterpart: ChatPartyDto,
    val lastMessage: ChatLastMessageDto? = null,
    val unread: Long = 0,
)

@Serializable
data class MessageDto(
    val id: Long,
    val conversationId: String,
    /** `buyer` or `merchant`. */
    val senderSide: String,
    val body: String,
    val listingId: String? = null,
    val clientId: String = "",
    val createdAt: String,
)

@Serializable
data class StartConversationBody(val merchantId: String)

/** [clientId] makes a retried send idempotent: the same id never creates a second message. */
@Serializable
data class SendMessageBody(val clientId: String, val body: String, val listingId: String? = null)

@Serializable
data class ReadBody(val lastReadId: Long)

@Serializable
data class WsTicketDto(val ticket: String, val expiresIn: Int = 30)
