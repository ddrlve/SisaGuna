package com.sisaguna.android.data.repository

import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.remote.ApiException
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.remote.apiCall
import com.sisaguna.android.data.remote.apiData
import com.sisaguna.android.data.remote.toVoucher
import com.sisaguna.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Vouchers of the account: claimed ones can be applied at checkout, `used` ones are dropped. */
@Singleton
class ApiVoucherRepository @Inject constructor(
    private val api: ApiService,
    private val auth: AuthRepository,
    private val messenger: AppMessenger,
    @ApplicationScope private val scope: CoroutineScope,
) : VoucherRepository {

    private val _vouchers = MutableStateFlow<List<Voucher>>(emptyList())
    override val vouchers: StateFlow<List<Voucher>> = _vouchers.asStateFlow()

    private val _claimed = MutableStateFlow<Set<String>>(emptySet())
    override val claimed: StateFlow<Set<String>> = _claimed.asStateFlow()

    init {
        scope.launch {
            auth.isAuthenticated.collect { signedIn ->
                if (!signedIn) {
                    _vouchers.value = emptyList()
                    _claimed.value = emptySet()
                    return@collect
                }
                try {
                    refresh()
                } catch (e: AppException) {
                    messenger.show(e)
                }
            }
        }
    }

    override suspend fun refresh() {
        // A guest has nothing to load (and the server would answer 401).
        if (!auth.isAuthenticated.value) return
        val rows = apiData { api.vouchers() }.filter { it.status != "used" }
        _vouchers.value = rows.map { it.toVoucher() }
        _claimed.value = rows.filter { it.status == "claimed" }.map { it.code }.toSet()
    }

    override fun claim(code: String) {
        if (code in _claimed.value) return
        _claimed.value = _claimed.value + code
        scope.launch {
            try {
                apiCall { api.claimVoucher(code) }
            } catch (e: AppException) {
                if (e is ApiException && e.code == "voucher_already_claimed") return@launch // already ours: keep it
                _claimed.value = _claimed.value - code
                messenger.show(e)
            }
        }
    }
}
