package com.sisaguna.android.feature.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.model.Address
import com.sisaguna.android.data.model.GeoPoint
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.repository.AddressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AddressUiState(
    val addresses: List<Address> = emptyList(),
    val selectedId: String? = null,
    val currentLocationLabel: String? = null,
) {
    /** What Home's location chip shows. */
    val chipLabel: String
        get() = currentLocationLabel ?: addresses.firstOrNull { it.id == selectedId }?.label ?: "Pilih lokasi"

    val selected: Address? get() = addresses.firstOrNull { it.id == selectedId }
}

/** Shared by the Home location sheet and Profile > Alamat. */
@HiltViewModel
class AddressViewModel @Inject constructor(
    private val repository: AddressRepository,
    private val messenger: AppMessenger,
) : ViewModel() {

    val uiState: StateFlow<AddressUiState> =
        combine(repository.addresses, repository.selectedId, repository.currentLocationLabel) { list, id, label ->
            AddressUiState(list, id, label)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            AddressUiState(repository.addresses.value, repository.selectedId.value, repository.currentLocationLabel.value),
        )

    fun select(id: String) = repository.select(id)

    fun useCurrentLocation(place: Place) =
        repository.selectCurrentLocation(place.shortLabel, GeoPoint(place.latitude, place.longitude))

    /** Saves a new ([existingId] null) or edited address on the server, then selects it. */
    fun save(existingId: String?, label: String, place: Place, note: String) {
        val address = Address(
            id = existingId.orEmpty(), // a new address gets its id from the server
            label = label.trim().ifBlank { "Alamat" },
            fullAddress = place.fullAddress,
            latitude = place.latitude,
            longitude = place.longitude,
            note = note.trim(),
        )
        viewModelScope.launch {
            try {
                repository.select(repository.upsert(address).id)
            } catch (e: AppException) {
                messenger.show(e)
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                repository.delete(id)
            } catch (e: AppException) {
                messenger.show(e)
            }
        }
    }
}
