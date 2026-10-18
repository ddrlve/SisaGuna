package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.Fulfillment
import com.sisaguna.android.data.model.QuantityUnit
import java.time.Instant
import kotlinx.coroutines.flow.StateFlow

enum class IncomingStatus { WAITING, HANDED_OVER, NO_SHOW }

/** An order placed at the signed-in user's own store (merchant side). */
data class IncomingOrder(
    val id: String,
    val buyerName: String,
    /** Empty for real orders: the backend never gives the code to the merchant, the buyer tells it in person. */
    val pickupCode: String = "",
    val itemTitle: String,
    val quantity: Int,
    val unit: QuantityUnit,
    val total: Int,
    val fulfillment: Fulfillment,
    val courierLabel: String?,
    val createdAt: Instant,
    val pickupBy: Instant,
    val status: IncomingStatus = IncomingStatus.WAITING,
)

/** Today's numbers for the partner home (WIB day). All amounts are Rupiah. */
data class MerchantDashboard(
    val salesToday: Int = 0,
    val commissionToday: Int = 0,
    val netToday: Int = 0,
    val completedToday: Int = 0,
    val pendingOrders: Int = 0,
    val activeListings: Int = 0,
)

sealed interface ConfirmResult {
    data object Confirmed : ConfirmResult
    data object WrongCode : ConfirmResult

    /** Typed wrong too many times (the order is locked for a while) or some other refusal; [message] says why. */
    data class Rejected(val message: String) : ConfirmResult
}

interface IncomingOrderRepository {
    val orders: StateFlow<List<IncomingOrder>>
    val dashboard: StateFlow<MerchantDashboard>

    /** The merchant types the code the buyer shows; a match marks the order handed over. */
    suspend fun confirmPickup(orderId: String, code: String): ConfirmResult

    /** Reloads the list from the source (the buyer may have placed a new order). */
    suspend fun refresh() {}
}
