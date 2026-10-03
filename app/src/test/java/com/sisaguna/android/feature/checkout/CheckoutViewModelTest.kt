package com.sisaguna.android.feature.checkout

import com.sisaguna.android.data.model.PaymentKind
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
    private fun vm() = CheckoutViewModel(cart, listings, orders, FakePaymentMethodRepository(), vouchers, com.sisaguna.android.data.repository.FakeAddressRepository())
    private fun listing(id: String) = listings.listings.value.first { it.id == id }

    @Test
    fun `qris waits for confirmation then places order and clears cart`() {
        cart.add(listing("l1"), 2)
        val vm = vm()
        assertEquals(16000, vm.uiState.value.total)
        assertEquals(34000, vm.uiState.value.savings)
        vm.pay()
        assertEquals(PaymentStep.AwaitingQris, vm.uiState.value.step)
        vm.confirmQrisPaid()
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
    fun `voucher applies, is recorded on the order, and is consumed`() {
        cart.add(listing("l1"), 2) // 16.000
        val vm = vm()
        vm.selectVoucher("HEMAT5K")
        assertEquals(5000, vm.uiState.value.voucherDiscount)
        assertEquals(11000, vm.uiState.value.total)
        vm.select(PaymentKind.CASH)
        vm.pay()
        val order = orders.orders.value.first()
        assertEquals(11000, order.total)
        assertEquals("HEMAT5K", order.voucherCode)
        assertTrue(vouchers.vouchers.value.none { it.code == "HEMAT5K" })
    }

    @Test
    fun `voucher below minimum gives zero`() {
        cart.add(listing("l4"), 1) // 5.000 < 15.000 minimum
        val vm = vm()
        vm.selectVoucher("HEMAT5K")
        assertEquals(0, vm.uiState.value.voucherDiscount)
        assertEquals(5000, vm.uiState.value.total)
    }
}
