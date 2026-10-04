package com.sisaguna.android.data.model

import java.time.Instant

/** The signed-in buyer in [ChatMessage.buyer]; real buyer ids arrive with the backend. */
const val BUYER_ME = "me"

/**
 * One message in a buyer-store conversation. Added after user interviews: buyers wanted to
 * ask the seller directly (is it still there, when was it cooked, is it halal) before paying.
 *
 * A conversation is the pair ([buyer], [merchantId]). The buyer sees it under the store's
 * name, the seller (partner mode) under the buyer's; [fromBuyer] says which side sent it.
 */
data class ChatMessage(
    val id: String,
    val merchantId: String,
    val fromBuyer: Boolean,
    val text: String,
    val sentAt: Instant,
    /** The listing the question was about, shown as a small card at the top of the chat. */
    val listingId: String? = null,
    /** Read by the recipient (the store for buyer messages, the buyer for store messages). */
    val isRead: Boolean = true,
    val buyer: String = BUYER_ME,
)
