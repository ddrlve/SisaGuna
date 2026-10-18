package com.sisaguna.android.feature.chat

import com.sisaguna.android.ui.i18n.l

/** The quick questions above the keyboard: the four things interviewees asked first. */
enum class QuickQuestion(val id: String, val en: String) {
    AVAILABLE("Masih tersedia?", "Still available?"),
    FRESH("Dimasak jam berapa?", "When was it cooked?"),
    HALAL("Halal?", "Is it halal?"),
    DELIVERY("Bisa diantar?", "Can you deliver?");

    val text: String get() = l(id, en)
}

/** Partner side: short answers a seller can send without typing, mid-service. */
enum class SellerQuickReply(val id: String, val en: String) {
    AVAILABLE("Masih ada kak, silakan checkout", "Still available, go ahead and check out"),
    READY("Sudah siap diambil", "Ready for pickup"),
    COOKED("Dimasak tadi pagi, masih aman", "Cooked this morning, still safe"),
    SOLD_OUT("Maaf, sudah habis", "Sorry, it's sold out");

    val text: String get() = l(id, en)
}
