package com.sisaguna.android.data.repository

import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.model.Complaint
import com.sisaguna.android.data.model.Fulfillment
import com.sisaguna.android.data.model.Order
import com.sisaguna.android.data.model.OrderRating
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.remote.RealtimeClient
import com.sisaguna.android.data.remote.RealtimeEvent
import com.sisaguna.android.data.remote.apiCall
import com.sisaguna.android.data.remote.apiData
import com.sisaguna.android.data.remote.dto.CancelBody
import com.sisaguna.android.data.remote.dto.ComplaintBody
import com.sisaguna.android.data.remote.dto.OrderDto
import com.sisaguna.android.data.remote.dto.OrderLineRequest
import com.sisaguna.android.data.remote.dto.PlaceOrderBody
import com.sisaguna.android.data.remote.dto.ReviewBody
import com.sisaguna.android.data.remote.toOrder
import com.sisaguna.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The buyer's orders, owned by the server. Prices, fees, stock and the allowed actions
 * (`can_cancel`, `can_rate`, `can_complain`) come from the backend; this class only keeps a
 * newest-first copy so screens can observe it.
 */
@Singleton
class ApiOrderRepository @Inject constructor(
    private val api: ApiService,
    private val auth: AuthRepository,
    private val media: MediaRepository,
    private val listings: ListingRepository,
    private val messenger: AppMessenger,
    realtime: RealtimeClient,
    @ApplicationScope private val scope: CoroutineScope,
) : OrderRepository {

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    override val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    /** Where the next older page of history starts; null when it is all loaded. */
    private var historyCursor: String? = null
    private val _hasMoreHistory = MutableStateFlow(false)
    override val hasMoreHistory: StateFlow<Boolean> = _hasMoreHistory.asStateFlow()

    /** Orders brought in by "load more": a refresh renews the first page and keeps these. */
    private val olderIds = mutableSetOf<String>()
    private val pagingLock = Mutex()

    init {
        scope.launch {
            auth.isAuthenticated.collect { signedIn ->
                if (!signedIn) {
                    olderIds.clear()
                    setHistoryCursor(null)
                    _orders.value = emptyList()
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
        // The merchant handing an order over (or the no-show job) changes it without the buyer doing anything.
        scope.launch {
            realtime.events.collect { event ->
                if (auth.isAuthenticated.value && (event is RealtimeEvent.OrderChanged || event == RealtimeEvent.Connected)) {
                    try {
                        refresh()
                    } catch (e: AppException) {
                        // Background refresh: the next event or screen open retries.
                    }
                }
            }
        }
    }

    override suspend fun refresh() {
        // A guest has nothing to load (and the server would answer 401).
        if (!auth.isAuthenticated.value) return
        pagingLock.withLock {
            val ongoing = apiData { api.orders(tab = "ongoing") }
            val history = apiCall { api.orders(tab = "history", filter = "all") }
            val fresh = (ongoing + history.data).distinctBy { it.id }.map(::map)
            val older = _orders.value.filter { it.id in olderIds && fresh.none { f -> f.id == it.id } }
            _orders.value = (fresh + older).sortedByDescending { it.createdAt }
            if (older.isEmpty()) setHistoryCursor(history.meta?.nextCursor)
        }
    }

    override suspend fun loadMoreHistory() {
        if (!auth.isAuthenticated.value) return
        pagingLock.withLock {
            val cursor = historyCursor ?: return
            val page = apiCall { api.orders(tab = "history", filter = "all", cursor = cursor) }
            val more = page.data.map(::map)
            olderIds += more.map { it.id }
            _orders.update { list -> (list + more).distinctBy { it.id }.sortedByDescending { it.createdAt } }
            setHistoryCursor(page.meta?.nextCursor)
        }
    }

    private fun setHistoryCursor(cursor: String?) {
        historyCursor = cursor
        _hasMoreHistory.value = cursor != null
    }

    override suspend fun place(request: PlaceOrderRequest, idempotencyKey: String): Order {
        val delivery = request.delivery?.takeIf { request.fulfillment == Fulfillment.DELIVERY }
        val body = PlaceOrderBody(
            merchantId = request.merchantId,
            lines = request.lines.map { (listing, qty) -> OrderLineRequest(listing.id, qty) },
            paymentKind = request.payment.name.lowercase(),
            fulfillment = request.fulfillment.name.lowercase(),
            voucherCode = request.voucherCode,
            courier = delivery?.courier?.name?.lowercase(),
            speed = delivery?.speed?.name?.lowercase(),
            addressId = request.deliveryAddress?.id?.takeIf { delivery != null },
            note = request.note,
            expectedTotal = request.expectedTotal,
        )
        return upsert(apiData { api.placeOrder(idempotencyKey, body) })
    }

    override suspend fun cancel(orderId: String) {
        upsert(apiData { api.cancelOrder(orderId, CancelBody()) })
    }

    override suspend fun rate(orderId: String, rating: OrderRating) {
        apiCall { api.reviewOrder(orderId, ReviewBody(rating.stars, rating.tags, rating.comment)) }
        reload(orderId)
    }

    override suspend fun complain(orderId: String, complaint: Complaint) {
        // Photos are private to the buyer, the store and the admin; the server checks the purpose.
        val mediaIds = complaint.photos.map { media.upload(it, MediaPurpose.COMPLAINT).id }
        apiCall { api.complainOrder(orderId, ComplaintBody(complaint.reason.name.lowercase(), complaint.detail, mediaIds)) }
        reload(orderId)
    }

    /** The review/complaint answers don't carry the order, so fetch it again for fresh flags and status. */
    private suspend fun reload(orderId: String) {
        upsert(apiData { api.order(orderId) })
    }

    private fun upsert(dto: OrderDto): Order {
        val order = map(dto)
        _orders.update { list -> (listOf(order) + list.filterNot { it.id == order.id }).sortedByDescending { it.createdAt } }
        return order
    }

    private fun map(dto: OrderDto): Order = dto.toOrder(listings::merchant)
}
