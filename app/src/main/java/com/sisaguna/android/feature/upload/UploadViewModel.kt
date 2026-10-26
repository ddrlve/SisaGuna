package com.sisaguna.android.feature.upload

import com.sisaguna.android.ui.i18n.l

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.remote.ApiException
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.model.Allergen
import com.sisaguna.android.data.model.FoodSafety
import com.sisaguna.android.data.model.HalalStatus
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.QuantityUnit
import com.sisaguna.android.data.model.SafetyAssessment
import com.sisaguna.android.data.model.SafetyCheck
import com.sisaguna.android.data.model.SafetyLevel
import com.sisaguna.android.data.model.StorageMethod
import com.sisaguna.android.data.repository.ListingDraft
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class UploadField { TITLE, DESCRIPTION, PRICE, ORIGINAL_PRICE, STOCK, PHOTO, SAFETY }

/** How long the listing stays up: 1/2/4/8 hours from publish. */
val pickupWindowOptions = listOf(1L, 2L, 4L, 8L)

/** "Dimasak berapa jam lalu?" quick picks. */
val madeHoursOptions = listOf(0, 1, 2, 3, 6, 12, 24, 36)

/** Feed/compost is weighed; the seller can type grams or kilos. */
enum class WeightUnit(val label: String) { GRAM("gram"), KILOGRAM("kg") }

const val MAX_PHOTOS = 5

data class UploadForm(
    /** First photo is the cover. Kept as [photoUri] for older callers. */
    val photos: List<String> = emptyList(),
    val videoUri: String? = null,
    val title: String = "",
    val tier: ListingTier = ListingTier.HUMAN,
    val description: String = "",
    val isFree: Boolean = false,
    val originalPrice: String = "",
    val price: String = "",
    /** Portions, for ready-to-eat food. */
    val stock: Int = 1,
    /** Weight, for feed/compost. */
    val weight: String = "",
    val weightUnit: WeightUnit = WeightUnit.KILOGRAM,
    val pickupHours: Long = 2,
    val madeHoursAgo: Int = 1,
    val storage: StorageMethod = StorageMethod.ROOM_TEMP,
    val checks: Set<SafetyCheck> = emptySet(),
    val halal: HalalStatus = HalalStatus.UNVERIFIED,
    val allergens: Set<Allergen> = emptySet(),
    val touched: Set<UploadField> = emptySet(),
    val submitted: Boolean = false,
) {
    val photoUri: String get() = photos.firstOrNull().orEmpty()
    val isFood: Boolean get() = tier == ListingTier.HUMAN
    val unit: QuantityUnit get() = if (isFood) QuantityUnit.PORTION else QuantityUnit.KILOGRAM

    private fun amount(text: String) = text.filter(Char::isDigit).toIntOrNull()

    val priceValue: Int? get() = amount(price)
    val originalPriceValue: Int? get() = amount(originalPrice)

    /** Entered weight in kilograms (grams converted). */
    val weightKg: Double?
        get() = weight.replace(',', '.').toDoubleOrNull()?.let { if (weightUnit == WeightUnit.GRAM) it / 1000.0 else it }

    /** Listing stock: portions, or whole kilos (rounded up, so 1.2 kg is sold as up to 2 × 1 kg). */
    val stockUnits: Int get() = if (isFood) stock else ceil(weightKg ?: 0.0).toInt()

    fun assessment(now: Instant): SafetyAssessment =
        FoodSafety.assess(tier, now.minus(madeHoursAgo.toLong(), ChronoUnit.HOURS), storage, checks, now)

    fun error(field: UploadField, now: Instant = Instant.now()): String? = when (field) {
        UploadField.TITLE -> when {
            title.isBlank() -> if (isFood) "Nama makanan wajib diisi" else "Nama bahan wajib diisi"
            title.trim().length < 4 -> "Minimal 4 karakter"
            else -> null
        }
        UploadField.DESCRIPTION -> if (description.trim().length < 15) "Ceritakan kondisinya (min. 15 karakter)" else null
        UploadField.ORIGINAL_PRICE -> if (!isFree && (originalPriceValue ?: 0) < 1_000) "Isi harga normal (min. Rp 1.000)" else null
        UploadField.PRICE -> when {
            isFree -> null
            priceValue == null || priceValue!! < 500 -> "Isi harga jual (min. Rp 500)"
            originalPriceValue != null && priceValue!! >= originalPriceValue!! -> "Harga jual harus lebih murah dari harga normal"
            else -> null
        }
        UploadField.STOCK -> when {
            isFood && stock < 1 -> "Minimal 1 porsi"
            !isFood && (weightKg ?: 0.0) <= 0.0 -> l("Isi berat dalam ${weightUnit.label}", "Enter the weight in ${weightUnit.label}")
            !isFood && (weightKg ?: 0.0) > 500 -> "Maksimal 500 kg per listing"
            else -> null
        }
        UploadField.PHOTO -> if (photos.isEmpty()) l("Tambahkan minimal 1 foto", "Add at least 1 photo") else null // the server requires it
        UploadField.SAFETY -> assessment(now).takeIf { isFood && it.level == SafetyLevel.UNSAFE }?.headline
    }

    fun visibleError(field: UploadField): String? = if (submitted || field in touched) error(field) else null

    val isValid: Boolean get() = UploadField.entries.all { error(it) == null }

    /** Suggests 60% off so new sellers price like surplus, not retail. */
    val suggestedPrice: Int?
        get() = originalPriceValue?.takeIf { it >= 1_000 }?.let { (it * 0.4 / 500).toInt() * 500 }
}

@HiltViewModel
class UploadViewModel(
    private val listingRepository: ListingRepository,
    private val messenger: AppMessenger,
    private val clock: () -> Instant,
    initialTier: ListingTier = ListingTier.HUMAN,
) : ViewModel() {

    @Inject constructor(listingRepository: ListingRepository, messenger: AppMessenger, savedStateHandle: SavedStateHandle) : this(
        listingRepository,
        messenger,
        Instant::now,
        savedStateHandle.get<String>(Screen.Upload.ARG_TIER)?.let { name -> ListingTier.entries.firstOrNull { it.name == name } } ?: ListingTier.HUMAN,
    )

    private val _form = MutableStateFlow(UploadForm(tier = initialTier))
    val form: StateFlow<UploadForm> = _form.asStateFlow()

    /** The draft and key of an attempt that may have reached the server; the same draft retries with the same key. */
    private var attempt: Pair<ListingDraft, String>? = null

    private val _publishing = MutableStateFlow(false)
    val publishing: StateFlow<Boolean> = _publishing.asStateFlow()

    val now: Instant get() = clock()

    val isDirty: Boolean get() = _form.value.let { it.title.isNotBlank() || it.description.isNotBlank() || it.photos.isNotEmpty() }

    fun update(transform: UploadForm.() -> UploadForm) {
        _form.value = _form.value.transform()
    }

    fun touch(field: UploadField) = update { copy(touched = touched + field) }

    fun addPhotos(uris: List<String>) = update { copy(photos = (photos + uris).distinct().take(MAX_PHOTOS)) }
    fun removePhoto(uri: String) = update { copy(photos = photos - uri) }
    fun makeCover(uri: String) = update { copy(photos = listOf(uri) + (photos - uri)) }

    fun toggleCheck(check: SafetyCheck) = update { copy(checks = if (check in checks) checks - check else checks + check) }

    /** "Alihkan ke pakan ternak" when the food is past its human-safe window. */
    fun switchToFeed() = update { copy(tier = ListingTier.ANIMAL_FEED) }

    /**
     * Reveals all errors when invalid; otherwise uploads the media and creates the listing, then calls
     * [onPublished] with its id. A server refusal (e.g. past the safe window) is shown and the form stays.
     */
    fun publish(onPublished: (String) -> Unit) {
        val f = _form.value
        val now = clock()
        if (!UploadField.entries.all { f.error(it, now) == null }) {
            _form.value = f.copy(submitted = true)
            return
        }
        if (_publishing.value) return
        _publishing.value = true
        val draft = ListingDraft(
            title = f.title.trim(),
            description = f.description.trim(),
            tier = f.tier,
            isFree = f.isFree,
            price = f.priceValue,
            priceOriginal = f.originalPriceValue,
            stock = f.stockUnits,
            halal = if (f.isFood) f.halal else HalalStatus.OTHER,
            allergens = f.allergens,
            madeAt = if (f.isFood) now.minus(f.madeHoursAgo.toLong(), ChronoUnit.HOURS) else null,
            storage = f.storage,
            safetyChecks = f.checks,
            pickupHours = f.pickupHours.toInt(),
            photoUris = f.photos,
            videoUri = f.videoUri,
        )
        val key = attempt?.takeIf { it.first == draft }?.second ?: UUID.randomUUID().toString()
        attempt = draft to key
        viewModelScope.launch {
            try {
                val created = listingRepository.createListing(draft, key)
                attempt = null
                onPublished(created.id)
            } catch (e: AppException) {
                // A refusal (not a lost connection) means the next tap is a new intent.
                if (e is ApiException) attempt = null
                messenger.show(e)
            } finally {
                _publishing.value = false
            }
        }
    }
}
