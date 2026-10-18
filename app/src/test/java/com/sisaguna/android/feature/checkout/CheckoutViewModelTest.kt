package com.sisaguna.android.feature.checkout

import com.sisaguna.android.data.model.Order
import com.sisaguna.android.data.model.PaymentKind
import com.sisaguna.android.data.remote.ApiException
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.remote.NetworkException
import com.sisaguna.android.data.repository.FakeAddressRepository
import com.sisaguna.android.data.repository.OrderRepository
import com.sisaguna.android.data.repository.PlaceOrderRequest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.sisaguna.android.data.repository.FakeNotificationRepository
import com.sisaguna.android.data.repository.FakeOrderRepository
import com.sisaguna.android.data.repository.FakePaymentMethodRepository
import com.sisaguna.android.data.repository.FakeVoucherRepository
import com.sisaguna.android.data.repository.InMemoryCartRepository
import com.sisaguna.android.testutil.MainDispatcherRule
import com.sisaguna.android.testutil.TestListingRepository
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CheckoutViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val listings = TestListingRepository()
    private val cart = InMemoryCartRepository()
    private val orders = FakeOrderRepository(listings, FakeNotificationRepository(), { Instant.now() }, paymentDelayMs = 0)
    private val vouchers = FakeVoucherRepository()
    private fun vm() = CheckoutViewModel(cart, listings, orders, FakePaymentMethodRepository(), vouchers, FakeAddressRepository())
    private fun listing(id: String) = listings.listings.value.first { it.id == id }

    @Test
    fun `paying with qris places the order at once and clears the cart`() {
        cart.add(listing("l1"), 2)
        val vm = vm()
        assertEquals(17000, vm.uiState.value.total) // 16.000 + Rp 1.000 service fee
        assertEquals(34000, vm.uiState.value.savings)
        vm.pay()
        val step = vm.uiState.value.step
        assertTrue(step is PaymentStep.Done)
        assertTrue(cart.cart.value.isEmpty)
        assertEquals((step as PaymentStep.Done).orderId, orders.orders.value.first().id)
    }

    @Test
    fun `free order skips online payment`() {
        cart.add(listing("l2"), 1) // free
        val vm = vm()
        vm.select(PaymentKind.GOPAY)
        assertEquals(PaymentKind.CASH, vm.uiState.value.effectivePayment)
        vm.pay()
        assertTrue(vm.uiState.value.step is PaymentStep.Done)
        assertEquals(PaymentKind.CASH, orders.orders.value.first().payment)
    }

    @Test
    fun `quantity is clamped to stock and zero removes the line`() {
        cart.add(listing("l1"), 1)
        val vm = vm()
        vm.setQuantity("l1", 99)
        assertEquals(5, vm.uiState.value.lines.single().quantity)
        vm.setQuantity("l1", 0)
        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun `note is capped at 140 characters`() {
        val vm = vm()
        vm.onNoteChange("a".repeat(200))
        assertEquals(140, vm.uiState.value.note.length)
    }

    @Test
    fun `voucher applies and is recorded on the order`() {
        cart.add(listing("l1"), 2) // 16.000
        val vm = vm()
        vm.selectVoucher("HEMAT5K")
        assertEquals(5000, vm.uiState.value.voucherDiscount)
        assertEquals(12000, vm.uiState.value.total) // incl. Rp 1.000 service fee
        vm.select(PaymentKind.CASH)
        vm.pay()
        val order = orders.orders.value.first()
        assertEquals(12000, order.total)
        assertEquals(1000, order.serviceFee)
        assertEquals("HEMAT5K", order.voucherCode)
    }

    @Test
    fun `add more offers the same store's other food and adds one portion`() {
        cart.add(listing("l1"), 1)
        val vm = vm()
        val more = vm.uiState.value.moreFromStore
        assertTrue(more.isNotEmpty())
        assertTrue(more.all { it.merchantId == listing("l1").merchantId && it.id != "l1" })
        vm.addMore(more.first())
        assertEquals(2, vm.uiState.value.lines.size)
        assertTrue(vm.uiState.value.moreFromStore.none { it.id == more.first().id })
    }

    @Test
    fun `voucher below minimum gives zero`() {
        cart.add(listing("l4"), 1) // 5.000 < 15.000 minimum
        val vm = vm()
        vm.selectVoucher("HEMAT5K")
        assertEquals(0, vm.uiState.value.voucherDiscount)
        assertEquals(6000, vm.uiState.value.total)
    }
}

/** Order repository that plays back a scripted outcome and records what the ViewModel sent. */
private class ScriptedOrders(private val delegate: FakeOrderRepository) : OrderRepository by delegate {
    val keys = mutableListOf<String>()
    val expectedTotals = mutableListOf<Int?>()
    var failure: AppException? = null

    override suspend fun place(request: PlaceOrderRequest, idempotencyKey: String): Order {
        keys += idempotencyKey
        expectedTotals += request.expectedTotal
        failure?.let { throw it }
        return delegate.place(request, idempotencyKey)
    }
}

class CheckoutServerContractTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val listings = TestListingRepository()
    private val cart = InMemoryCartRepository()
    private val orders = ScriptedOrders(FakeOrderRepository(listings, FakeNotificationRepository(), { Instant.now() }, paymentDelayMs = 0))
    private fun vm() = CheckoutViewModel(cart, listings, orders, FakePaymentMethodRepository(), FakeVoucherRepository(), FakeAddressRepository())

    private fun readyToPay(): CheckoutViewModel {
        cart.add(listings.listings.value.first { it.id == "l1" }, 2)
        return vm().also { it.select(PaymentKind.CASH) }
    }

    @Test
    fun `the total on screen is sent so the server can detect a price change`() {
        val vm = readyToPay()
        vm.pay()
        assertEquals(listOf<Int?>(17000), orders.expectedTotals)
    }

    @Test
    fun `price_changed adopts the server numbers and asks again`() {
        val vm = readyToPay()
        orders.failure = ApiException(
            409, "price_changed", "harga berubah",
            JsonObject(
                mapOf(
                    "voucher_discount" to JsonPrimitive(0), "service_fee" to JsonPrimitive(2000),
                    "delivery_fee" to JsonPrimitive(0), "total" to JsonPrimitive(18000),
                ),
            ),
        )
        vm.pay()

        val state = vm.uiState.value
        assertTrue((state.step as PaymentStep.Failed).message.contains("Harga berubah"))
        assertEquals(18000, state.total)
        assertEquals(2000, state.serviceFee)
        assertTrue(cart.cart.value.quantities.isNotEmpty()) // nothing was bought

        vm.setQuantity("l1", 1) // editing the basket brings the live preview back
        assertEquals(null, vm.uiState.value.serverPrice)
    }

    @Test
    fun `a network failure retries with the same key but a refusal starts a new purchase`() {
        val vm = readyToPay()
        orders.failure = NetworkException(java.io.IOException("timeout"))
        vm.pay()
        vm.pay()
        assertEquals(2, orders.keys.size)
        assertEquals(orders.keys[0], orders.keys[1])

        orders.failure = ApiException(409, "out_of_stock", "stok habis")
        vm.pay()
        vm.pay()
        assertEquals(4, orders.keys.size)
        assertEquals(orders.keys[0], orders.keys[2]) // still the same purchase until the server answers
        assertTrue(orders.keys[3] != orders.keys[2]) // after a refusal a new tap is a new purchase
    }

    @Test
    fun `a refusal is shown with the server's reason`() {
        val vm = readyToPay()
        orders.failure = ApiException(409, "out_of_stock", "stok tidak cukup")
        vm.pay()
        assertEquals("Stok tidak cukup", (vm.uiState.value.step as PaymentStep.Failed).message)
    }
}
