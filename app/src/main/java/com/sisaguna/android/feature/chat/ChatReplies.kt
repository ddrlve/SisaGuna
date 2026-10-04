package com.sisaguna.android.feature.chat

import com.sisaguna.android.data.model.FoodSafety
import com.sisaguna.android.data.model.HalalStatus
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.ui.domain.formatClock
import com.sisaguna.android.ui.i18n.l
import java.time.Instant

/** The quick questions above the keyboard: the four things interviewees asked first. */
enum class QuickQuestion(val id: String, val en: String) {
    AVAILABLE("Masih tersedia?", "Still available?"),
    FRESH("Dimasak jam berapa?", "When was it cooked?"),
    HALAL("Halal?", "Is it halal?"),
    DELIVERY("Bisa diantar?", "Can you deliver?");

    val text: String get() = l(id, en)
}

/**
 * Stand-in for the seller while there is no backend: answers from the listing and store data
 * the app already has, so the demo replies are true for the item being asked about.
 */
object ChatReplies {

    fun replyFor(question: String, merchant: Merchant, listing: Listing?, now: Instant = Instant.now()): String {
        val q = question.lowercase()
        return when {
            q.hasAny("masih", "tersedia", "ada ga", "ada gak", "stok", "available", "still", "left") -> available(listing)
            q.hasAny("masak", "dibuat", "jam berapa", "fresh", "segar", "basi", "cooked", "made", "when") -> freshness(listing, now)
            q.hasAny("halal") -> halal(merchant)
            q.hasAny("antar", "kirim", "kurir", "deliver", "courier") -> delivery(merchant)
            q.hasAny("alergi", "kacang", "allerg", "nut", "susu", "telur") -> allergens(merchant, listing)
            q.hasAny("terima kasih", "makasih", "thanks", "thank") -> l("Sama-sama kak, ditunggu ya! 🙏", "You're welcome, see you soon! 🙏")
            else -> l(
                "Halo kak, pesannya sudah kami terima. Kami balas secepatnya ya.",
                "Hi! We got your message and will reply as soon as we can.",
            )
        }
    }

    private fun available(listing: Listing?): String = when {
        listing == null -> l("Masih ada kak, cek menu yang aktif di halaman toko ya.", "Yes, check the live items on our store page.")
        listing.stock <= 0 -> l("Maaf kak, ${listing.title} sudah habis hari ini.", "Sorry, ${listing.title} is sold out today.")
        else -> l(
            "Masih ada kak! Sisa ${listing.stock} ${listing.unit.short}, bisa diambil sampai ${formatClock(listing.pickupEnd)}. Langsung checkout aja biar nggak keduluan.",
            "Yes! ${listing.stock} ${listing.unit.shortEn} left, collect by ${formatClock(listing.pickupEnd)}. Check out soon so you don't miss it.",
        )
    }

    private fun freshness(listing: Listing?, now: Instant): String {
        if (listing == null) return l("Semua menu kami dimasak hari ini kak.", "Everything is cooked today.")
        if (listing.tier != ListingTier.HUMAN) {
            return l("Ini untuk pakan atau kompos kak, bukan untuk dimakan.", "This is for feed or compost, not for eating.")
        }
        val made = listing.madeAt ?: return l("Dimasak hari ini kak, disimpan tertutup.", "Cooked today and kept covered.")
        val until = FoodSafety.assess(listing, now).safeUntil
        val storage = l(listing.storage.label.lowercase(), listing.storage.labelEn.lowercase())
        return l(
            "Dimasak jam ${formatClock(made)}, disimpan $storage." + (until?.let { " Aman dimakan sampai jam ${formatClock(it)}." } ?: ""),
            "Cooked at ${formatClock(made)}, stored $storage." + (until?.let { " Safe to eat until ${formatClock(it)}." } ?: ""),
        )
    }

    private fun halal(merchant: Merchant): String = when (merchant.halal) {
        HalalStatus.HALAL_CERTIFIED -> l("Iya kak, dapur kami sudah bersertifikat halal.", "Yes, our kitchen is halal certified.")
        HalalStatus.NON_HALAL -> l("Maaf kak, dapur kami non-halal.", "Sorry, our kitchen is non-halal.")
        else -> l(
            "Bahan kami tanpa babi kak, tapi dapur belum bersertifikat halal.",
            "No pork in our ingredients, but the kitchen isn't halal certified yet.",
        )
    }

    private fun delivery(merchant: Merchant): String =
        if (merchant.deliveryAvailable) {
            l(
                "Bisa kak, pilih \"Kirim kurir\" waktu checkout. Ada GoSend, GrabExpress, SPX Instant, dan Lalamove.",
                "Yes, choose \"Courier\" at checkout. GoSend, GrabExpress, SPX Instant and Lalamove are available.",
            )
        } else {
            l("Untuk toko kami ambil sendiri dulu ya kak.", "We're pickup only for now.")
        }

    private fun allergens(merchant: Merchant, listing: Listing?): String {
        val set = (listing?.allergens.orEmpty() + merchant.allergens)
        return if (set.isEmpty()) {
            l("Tidak ada alergen umum di menu ini kak.", "No common allergens in this item.")
        } else {
            l("Mengandung atau diolah dekat: ", "Contains or is prepared near: ") + set.joinToString { it.label.lowercase() } + "."
        }
    }

    private fun String.hasAny(vararg words: String) = words.any { it in this }
}

/** Partner side: short answers a seller can send without typing, mid-service. */
enum class SellerQuickReply(val id: String, val en: String) {
    AVAILABLE("Masih ada kak, silakan checkout", "Still available, go ahead and check out"),
    READY("Sudah siap diambil", "Ready for pickup"),
    COOKED("Dimasak tadi pagi, masih aman", "Cooked this morning, still safe"),
    SOLD_OUT("Maaf, sudah habis", "Sorry, it's sold out");

    val text: String get() = l(id, en)
}
