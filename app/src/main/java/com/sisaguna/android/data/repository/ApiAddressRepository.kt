package com.sisaguna.android.data.repository

import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.model.Address
import com.sisaguna.android.data.model.GeoPoint
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.remote.NotSignedInException
import com.sisaguna.android.data.remote.apiCall
import com.sisaguna.android.data.remote.apiData
import com.sisaguna.android.data.remote.dto.AddressRequest
import com.sisaguna.android.data.remote.toAddress
import com.sisaguna.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Saved addresses live on the server (`/me/addresses`); a GPS fix is only ever local, and is
 * the one thing a guest can still select. The selection is applied immediately and rolled back
 * (with a message) if the server refuses it.
 */
@Singleton
class ApiAddressRepository @Inject constructor(
    private val api: ApiService,
    private val auth: AuthRepository,
    private val messenger: AppMessenger,
    @ApplicationScope private val scope: CoroutineScope,
) : AddressRepository {

    private val _addresses = MutableStateFlow<List<Address>>(emptyList())
    override val addresses: StateFlow<List<Address>> = _addresses.asStateFlow()

    private val _selectedId = MutableStateFlow<String?>(null)
    override val selectedId: StateFlow<String?> = _selectedId.asStateFlow()

    private val _gpsLabel = MutableStateFlow<String?>(null)
    override val currentLocationLabel: StateFlow<String?> = _gpsLabel.asStateFlow()

    private val gpsPoint = MutableStateFlow<GeoPoint?>(null)

    override val searchLocation: StateFlow<GeoPoint?> = combine(_addresses, _selectedId, gpsPoint) { list, id, gps ->
        list.firstOrNull { it.id == id }?.let { GeoPoint(it.latitude, it.longitude) } ?: gps
    }.stateIn(scope, SharingStarted.Eagerly, null)

    init {
        scope.launch {
            auth.isAuthenticated.collect { signedIn -> if (signedIn) refresh() else reset() }
        }
    }

    private suspend fun refresh() {
        try {
            _addresses.value = apiData { api.addresses() }.map { it.toAddress() }
            _selectedId.value = apiData { api.me() }.selectedAddressId?.takeIf { id -> _addresses.value.any { it.id == id } }
            if (_selectedId.value != null) _gpsLabel.value = null
        } catch (e: AppException) {
            messenger.show(e)
        }
    }

    private fun reset() {
        _addresses.value = emptyList()
        _selectedId.value = null
    }

    override suspend fun upsert(address: Address): Address {
        if (!auth.isAuthenticated.value) throw NotSignedInException()
        val body = AddressRequest(address.label, address.fullAddress, address.note, address.latitude, address.longitude)
        val isNew = _addresses.value.none { it.id == address.id }
        val saved = apiData { if (isNew) api.createAddress(body) else api.updateAddress(address.id, body) }.toAddress()
        _addresses.update { list -> if (isNew) list + saved else list.map { if (it.id == saved.id) saved else it } }
        return saved
    }

    override suspend fun delete(id: String) {
        apiCall { api.deleteAddress(id) }
        _addresses.update { list -> list.filterNot { it.id == id } }
        // The server falls back to the GPS location when the selected address is deleted.
        if (_selectedId.value == id) _selectedId.value = null
    }

    override fun select(id: String) {
        if (_addresses.value.none { it.id == id }) return
        val previous = _selectedId.value to _gpsLabel.value
        _selectedId.value = id
        _gpsLabel.value = null
        persistSelection(JsonPrimitive(id)) {
            _selectedId.value = previous.first
            _gpsLabel.value = previous.second
        }
    }

    override fun selectCurrentLocation(label: String, point: GeoPoint) {
        _selectedId.value = null
        _gpsLabel.value = label
        gpsPoint.value = point
        persistSelection(JsonNull) { /* local-only choice: nothing to roll back */ }
    }

    private fun persistSelection(addressId: kotlinx.serialization.json.JsonElement, rollback: () -> Unit) {
        if (!auth.isAuthenticated.value) return
        scope.launch {
            try {
                apiCall { api.selectAddress(JsonObject(mapOf("address_id" to addressId))) }
            } catch (e: AppException) {
                rollback()
                messenger.show(e)
            }
        }
    }
}
