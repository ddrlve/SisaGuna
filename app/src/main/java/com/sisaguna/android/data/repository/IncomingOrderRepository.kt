package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.Fulfillment
import com.sisaguna.android.data.model.QuantityUnit
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class IncomingStatus { WAITING, HANDED_OVER, NO_SHOW }

/** An order placed at the signed-in user's own store (merchant side). */
data class IncomingOrder(
    val id: String,
    val buyerName: String,
    val pickupCode: String,
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

sealed interface ConfirmResult {
    data object Confirmed : ConfirmResult
    data object WrongCode : ConfirmResult
}

interface IncomingOrderRepository {
    val orders: StateFlow<List<IncomingOrder>>

    /** The merchant types the code the buyer shows; a match marks the order handed over. */
    fun confirmPickup(orderId: String, code: String): ConfirmResult
}

@Singleton
class FakeIncomingOrderRepository @Inject constructor() : IncomingOrderRepository {
    private val now = Instant.now()
    private fun ago(m: Long) = now.minus(Duration.ofMinutes(m))
    private fun inMin(m: Long) = now.plus(Duration.ofMinutes(m))

    private val _orders = MutableStateFlow(
        listOf(
            IncomingOrder("in1", "Clara T.", "SG-5521", "Kue Lapis Sisa Arisan", 2, QuantityUnit.PORTION, 20_000, Fulfillment.PICKUP, null, ago(8), inMin(37)),
            IncomingOrder("in2", "Rizky A.", "SG-6102", "Kue Lapis Sisa Arisan", 1, QuantityUnit.PORTION, 10_000, Fulfillment.DELIVERY, "GoSend · Standar", ago(15), inMin(25)),
            IncomingOrder("in3", "Kebun Warga RW 04", "SG-6630", "Kulit Buah untuk Kompos", 3, QuantityUnit.KILOGRAM, 0, Fulfillment.PICKUP, null, ago(40), inMin(5)),
            IncomingOrder("in4", "Dinda P.", "SG-4410", "Kue Lapis Sisa Arisan", 1, QuantityUnit.PORTION, 10_000, Fulfillment.PICKUP, null, ago(180), ago(135), IncomingStatus.HANDED_OVER),
        ),
    )
    override val orders: StateFlow<List<IncomingOrder>> = _orders.asStateFlow()

    override fun confirmPickup(orderId: String, code: String): ConfirmResult {
        val order = _orders.value.firstOrNull { it.id == orderId } ?: return ConfirmResult.WrongCode
        val typed = code.trim().uppercase().let { if (it.startsWith("SG-")) it else "SG-$it" }
        if (typed != order.pickupCode) return ConfirmResult.WrongCode
        _orders.value = _orders.value.map { if (it.id == orderId) it.copy(status = IncomingStatus.HANDED_OVER) else it }
        return ConfirmResult.Confirmed
    }
}
