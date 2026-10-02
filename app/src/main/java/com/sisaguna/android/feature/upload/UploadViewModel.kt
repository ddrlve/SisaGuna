package com.sisaguna.android.feature.upload

import androidx.lifecycle.ViewModel
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.repository.ListingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UploadField { TITLE, DESCRIPTION, PRICE, ORIGINAL_PRICE, STOCK }

/** How long the listing stays up: 1/2/4/8 hours from publish. */
val pickupWindowOptions = listOf(1L, 2L, 4L, 8L)

data class UploadForm(
    val photoUri: String = "",
    val title: String = "",
    val tier: ListingTier = ListingTier.HUMAN,
    val description: String = "",
    val isFree: Boolean = false,
    val originalPrice: String = "",
    val price: String = "",
    val stock: Int = 1,
    val pickupHours: Long = 2,
    val touched: Set<UploadField> = emptySet(),
    val submitted: Boolean = false,
) {
    private fun amount(text: String) = text.filter(Char::isDigit).toIntOrNull()

    val priceValue: Int? get() = amount(price)
    val originalPriceValue: Int? get() = amount(originalPrice)

    fun error(field: UploadField): String? = when (field) {
        UploadField.TITLE -> when {
            title.isBlank() -> "Nama makanan wajib diisi"
            title.trim().length < 4 -> "Minimal 4 karakter"
            else -> null
        }
        UploadField.DESCRIPTION -> if (description.trim().length < 15) "Ceritakan kondisi makanan (min. 15 karakter)" else null
        UploadField.ORIGINAL_PRICE -> if (!isFree && (originalPriceValue ?: 0) < 1_000) "Isi harga normal (min. Rp 1.000)" else null
        UploadField.PRICE -> when {
            isFree -> null
            priceValue == null || priceValue!! < 500 -> "Isi harga jual (min. Rp 500)"
            originalPriceValue != null && priceValue!! >= originalPriceValue!! -> "Harga jual harus lebih murah dari harga normal"
            else -> null
        }
        UploadField.STOCK -> if (stock < 1) "Minimal 1 porsi" else null
    }

    fun visibleError(field: UploadField): String? = if (submitted || field in touched) error(field) else null

    val isValid: Boolean get() = UploadField.entries.all { error(it) == null }

    /** Suggests 50–70% off so new sellers price like surplus, not retail. */
    val suggestedPrice: Int?
        get() = originalPriceValue?.takeIf { it >= 1_000 }?.let { (it * 0.4 / 500).toInt() * 500 }
}

@HiltViewModel
class UploadViewModel(
    private val listingRepository: ListingRepository,
    private val clock: () -> Instant,
) : ViewModel() {

    @Inject constructor(listingRepository: ListingRepository) : this(listingRepository, Instant::now)

    private val _form = MutableStateFlow(UploadForm())
    val form: StateFlow<UploadForm> = _form.asStateFlow()

    val isDirty: Boolean get() = _form.value.let { it.title.isNotBlank() || it.description.isNotBlank() || it.photoUri.isNotBlank() }

    fun update(transform: UploadForm.() -> UploadForm) {
        _form.value = _form.value.transform()
    }

    fun touch(field: UploadField) = update { copy(touched = touched + field) }

    /** Returns the new listing id, or null (and reveals all errors) when invalid. */
    fun publish(): String? {
        val f = _form.value
        if (!f.isValid) {
            _form.value = f.copy(submitted = true)
            return null
        }
        val now = clock()
        val listing = Listing(
            id = "u-" + UUID.randomUUID().toString().take(8),
            merchantId = ListingRepository.MY_MERCHANT_ID,
            title = f.title.trim(),
            tier = f.tier,
            priceOriginal = f.originalPriceValue,
            priceDiscounted = if (f.isFree) null else f.priceValue,
            isFree = f.isFree,
            pickupEnd = now.plus(f.pickupHours, ChronoUnit.HOURS),
            imageUrl = f.photoUri,
            distanceKm = 0.3,
            description = f.description.trim(),
            stock = f.stock,
            pickupStart = now,
        )
        listingRepository.addListing(listing)
        return listing.id
    }
}
