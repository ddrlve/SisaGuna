package com.sisaguna.android.feature.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.data.model.Address
import com.sisaguna.android.data.repository.AddressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
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

    fun useCurrentLocation(label: String) = repository.selectCurrentLocation(label)

    /** Saves a new or edited address and selects it. Returns the saved address. */
    fun save(existingId: String?, label: String, place: Place, note: String): Address {
        val address = Address(
            id = existingId ?: "a-" + UUID.randomUUID().toString().take(8),
            label = label.trim().ifBlank { "Alamat" },
            fullAddress = place.fullAddress,
            latitude = place.latitude,
            longitude = place.longitude,
            note = note.trim(),
        )
        repository.upsert(address)
        repository.select(address.id)
        return address
    }

    fun delete(id: String) = repository.delete(id)
}
