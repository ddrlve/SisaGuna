package com.sisaguna.android.feature.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.model.Allergen
import com.sisaguna.android.data.model.HalalStatus
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.data.repository.StoreDraft
import com.sisaguna.android.feature.address.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Quick picks for "how long from order to ready", in minutes. */
val prepMinuteOptions = listOf(10, 15, 30, 45)

data class StoreSetupForm(
    val name: String = "",
    val about: String = "",
    val openHours: String = "",
    val prepMinutes: Int = 15,
    val halal: HalalStatus = HalalStatus.UNVERIFIED,
    val allergens: Set<Allergen> = emptySet(),
    /** The pin the partner dropped; the backend needs coordinates to compute every buyer's distance. */
    val place: Place? = null,
    val submitted: Boolean = false,
    val saving: Boolean = false,
) {
    val nameError: String? get() = when {
        name.isBlank() -> "Nama toko wajib diisi"
        name.trim().length < 2 -> "Minimal 2 karakter"
        else -> null
    }
    val placeError: String? get() = if (place == null) "Pilih lokasi toko di peta" else null
    val isValid: Boolean get() = nameError == null && placeError == null

    fun visible(error: String?): String? = if (submitted) error else null
}

/** The form behind "Daftarkan toko". Approval is manual, so success means "submitted", not "open". */
@HiltViewModel
class StoreSetupViewModel @Inject constructor(
    private val listingRepository: ListingRepository,
    private val messenger: AppMessenger,
) : ViewModel() {

    private val _form = MutableStateFlow(StoreSetupForm())
    val form: StateFlow<StoreSetupForm> = _form.asStateFlow()

    fun update(transform: StoreSetupForm.() -> StoreSetupForm) = _form.update { it.transform() }

    fun toggleAllergen(allergen: Allergen) =
        update { copy(allergens = if (allergen in allergens) allergens - allergen else allergens + allergen) }

    fun submit(onDone: () -> Unit) {
        val f = _form.value
        if (!f.isValid) {
            _form.value = f.copy(submitted = true)
            return
        }
        if (f.saving) return
        val place = f.place ?: return
        _form.value = f.copy(saving = true)
        viewModelScope.launch {
            try {
                listingRepository.createStore(
                    StoreDraft(
                        name = f.name.trim(),
                        about = f.about.trim(),
                        address = place.fullAddress,
                        latitude = place.latitude,
                        longitude = place.longitude,
                        locationLabel = place.shortLabel.take(60),
                        openHours = f.openHours.trim(),
                        halal = f.halal,
                        allergens = f.allergens,
                        prepMinutes = f.prepMinutes,
                    ),
                )
                onDone()
            } catch (e: AppException) {
                messenger.show(e)
            } finally {
                _form.update { it.copy(saving = false) }
            }
        }
    }
}
