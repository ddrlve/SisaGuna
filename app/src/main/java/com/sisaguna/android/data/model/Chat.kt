package com.sisaguna.android.data.model

import java.time.Instant

/**
 * One message between the buyer and a store. Added after user interviews: buyers wanted to ask
 * the seller directly (is it still there, when was it cooked, is it halal) before paying.
 */
data class ChatMessage(
    val id: String,
    val merchantId: String,
    val fromMe: Boolean,
    val text: String,
    val sentAt: Instant,
    /** The listing the question was about, shown as a small card above the first message. */
    val listingId: String? = null,
    val isRead: Boolean = true,
)
