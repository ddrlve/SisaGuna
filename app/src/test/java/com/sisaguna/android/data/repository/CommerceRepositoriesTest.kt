package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.Address
import com.sisaguna.android.data.model.OrderRating
import com.sisaguna.android.data.model.OrderStatus
import com.sisaguna.android.data.model.PaymentKind
import com.sisaguna.android.testutil.TestListingRepository
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CartRepositoryTest {

    private val listings = TestListingRepository()
    private fun listing(id: String) = listings.listings.value.first { it.id == id }

    @Test
    fun `adding twice sums quantity capped at stock`() {
        val cart = InMemoryCartRepository()
        cart.add(listing("l1"), 3)
        cart.add(listing("l1"), 4) // stock defaults to 5
        assertEquals(5, cart.cart.value.quantities["l1"])
    }

    @Test
    fun `different merchant is refused until replaced`() {
        val cart = InMemoryCartRepository()
        cart.add(listing("l1"), 1)
        val result = cart.add(listing("l4"), 1) // m2
        assertEquals(AddToCartResult.DifferentMerchant("m1"), result)
        cart.replace(listing("l4"), 2)
        assertEquals("m2", cart.cart.value.merchantId)
        assertEquals(mapOf("l4" to 2), cart.cart.value.quantities)
    }

    @Test
    fun `quantity zero removes line and empties merchant`() {
        val cart = InMemoryCartRepository()
        cart.add(listing("l1"), 1)
        cart.setQuantity("l1", 0)
        assertTrue(cart.cart.value.isEmpty)
        assertNull(cart.cart.value.merchantId)
    }
}

class OrderRepositoryTest {

    private val now = Instant.parse("2026-10-03T05:00:00Z")
    private val listings = TestListingRepository()
    private val notifications = FakeNotificationRepository(now)
    private fun repo() = FakeOrderRepository(listings, notifications, { now }, paymentDelayMs = 0)

    @Test
    fun `place creates ready order, reduces stock, and notifies`() = runTest {
        val repo = repo()
        val l1 = listings.listings.value.first { it.id == "l1" }
        val order = repo.place(PlaceOrderRequest("m1", listOf(l1 to 2), PaymentKind.QRIS, "  tanpa sambal "))
        assertEquals(OrderStatus.READY, order.status)
        assertEquals(16000, order.total)
        assertEquals(34000, order.savings)
        assertEquals("tanpa sambal", order.note)
        assertEquals(order, repo.orders.value.first())
        assertEquals(3, listings.listings.value.first { it.id == "l1" }.stock)
        assertEquals("n-${order.id}", notifications.notifications.value.first().id)
    }

    @Test
    fun `rating only allowed once after pickup`() = runTest {
        val repo = repo()
        val l1 = listings.listings.value.first { it.id == "l1" }
        val order = repo.place(PlaceOrderRequest("m1", listOf(l1 to 1), PaymentKind.CASH, ""))
        repo.rate(order.id, OrderRating(5, emptyList(), ""))
        assertNull(repo.orders.value.first { it.id == order.id }.rating) // not picked up yet
        repo.markPickedUp(order.id)
        repo.rate(order.id, OrderRating(9, listOf("Ramah"), "mantap"))
        assertEquals(5, repo.orders.value.first { it.id == order.id }.rating?.stars) // clamped
        repo.rate(order.id, OrderRating(1, emptyList(), ""))
        assertEquals(5, repo.orders.value.first { it.id == order.id }.rating?.stars) // unchanged
    }

    @Test
    fun `cancel only works on ready orders`() {
        val repo = repo()
        repo.cancel("o3") // completed seed
        assertEquals(OrderStatus.COMPLETED, repo.orders.value.first { it.id == "o3" }.status)
        repo.cancel("o4")
        assertEquals(OrderStatus.CANCELLED, repo.orders.value.first { it.id == "o4" }.status)
    }
}

class AddressAndPaymentRepositoryTest {

    @Test
    fun `deleting selected address selects the first remaining`() {
        val repo = FakeAddressRepository()
        repo.select("a2")
        repo.delete("a2")
        assertEquals("a1", repo.selectedId.value)
    }

    @Test
    fun `upsert edits in place and adds new`() {
        val repo = FakeAddressRepository()
        repo.upsert(repo.addresses.value[0].copy(label = "Rumah Ibu"))
        repo.upsert(Address("a3", "Kos", "Jl. Kos", 0.0, 0.0))
        assertEquals(listOf("Rumah Ibu", "Kantor", "Kos"), repo.addresses.value.map { it.label })
    }

    @Test
    fun `current location clears selected address`() {
        val repo = FakeAddressRepository()
        repo.selectCurrentLocation("Pinang, Tangerang")
        assertNull(repo.selectedId.value)
        assertEquals("Pinang, Tangerang", repo.currentLocationLabel.value)
    }

    @Test
    fun `unlinking default e-wallet falls back to QRIS and cash cannot be removed`() {
        val repo = FakePaymentMethodRepository()
        repo.link(PaymentKind.OVO, "081298765432")
        repo.setDefault(PaymentKind.OVO)
        repo.unlink(PaymentKind.OVO)
        assertEquals(PaymentKind.QRIS, repo.defaultKind.value)
        repo.unlink(PaymentKind.CASH)
        assertTrue(repo.methods.value.any { it.kind == PaymentKind.CASH })
        assertEquals(PaymentKind.CASH, repo.methods.value.last().kind)
    }
}

class VoucherTest {
    private val now = Instant.parse("2026-10-03T05:00:00Z")

    @Test
    fun `percent is capped by max and never exceeds subtotal`() {
        val v = Voucher("X", "", "", percentOff = 50, maxDiscount = 10_000, expiresAt = now)
        assertEquals(5_000, v.discountFor(10_000))
        assertEquals(10_000, v.discountFor(100_000))
        val flat = Voucher("Y", "", "", flatOff = 5_000, expiresAt = now)
        assertEquals(3_000, flat.discountFor(3_000))
    }

    @Test
    fun `claim only known vouchers`() {
        val repo = FakeVoucherRepository(now)
        repo.claim("NOPE")
        repo.claim("SELAMAT20")
        assertEquals(setOf("HEMAT5K", "SELAMAT20"), repo.claimed.value)
    }
}
