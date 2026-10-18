package com.sisaguna.android.data.repository

import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.remote.ApiException
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.remote.RealtimeClient
import com.sisaguna.android.data.remote.RealtimeEvent
import com.sisaguna.android.data.remote.apiData
import com.sisaguna.android.data.remote.dto.ConfirmBody
import com.sisaguna.android.data.remote.toIncoming
import com.sisaguna.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Orders placed at the user's own store. Only accounts with an approved store get rows. */
@Singleton
class ApiIncomingOrderRepository @Inject constructor(
    private val api: ApiService,
    private val listings: ListingRepository,
    private val messenger: AppMessenger,
    realtime: RealtimeClient,
    @ApplicationScope private val scope: CoroutineScope,
) : IncomingOrderRepository {

    private val _orders = MutableStateFlow<List<IncomingOrder>>(emptyList())
    override val orders: StateFlow<List<IncomingOrder>> = _orders.asStateFlow()

    private val _dashboard = MutableStateFlow(MerchantDashboard())
    override val dashboard: StateFlow<MerchantDashboard> = _dashboard.asStateFlow()

    init {
        scope.launch {
            // Follow the store lookup: a plain buyer account has no merchant endpoints to call.
            listings.myMerchant.collect { store ->
                if (store == null) {
                    _orders.value = emptyList()
                    _dashboard.value = MerchantDashboard()
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

    init {
        scope.launch {
            realtime.events.collect { event ->
                if (event is RealtimeEvent.OrderChanged || event == RealtimeEvent.Connected) {
                    try {
                        refresh()
                    } catch (e: AppException) {
                        // Background refresh: the next event or screen open retries.
                    }
                }
            }
        }
    }

    // shortcut: newest 50 of each group; add cursor paging when a store gets more than that in a day.
    override suspend fun refresh() {
        if (listings.myMerchant.value == null) return
        _dashboard.value = apiData { api.dashboard() }.let {
            MerchantDashboard(it.salesToday, it.commissionToday, it.netToday, it.completedToday, it.pendingOrders, it.activeListings)
        }
        val ready = apiData { api.merchantOrders(status = "ready") }
        val done = apiData { api.merchantOrders(status = "done") }
        _orders.value = (ready + done).distinctBy { it.id }
            .filter { it.status != "cancelled" && it.status != "expired" }
            .map { it.toIncoming() }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun confirmPickup(orderId: String, code: String): ConfirmResult = try {
        val confirmed = apiData { api.confirmOrder(orderId, ConfirmBody(code.trim())) }.toIncoming()
        _orders.update { list -> list.map { if (it.id == orderId) confirmed else it } }
        try {
            refresh() // today's sales just changed
        } catch (e: AppException) {
            messenger.show(e)
        }
        ConfirmResult.Confirmed
    } catch (e: ApiException) {
        when (e.code) {
            "wrong_code" -> ConfirmResult.WrongCode
            "too_many_attempts" -> ConfirmResult.Rejected(e.userMessage)
            else -> throw e
        }
    }
}
