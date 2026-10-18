package com.sisaguna.android.feature.admin

import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.model.ComplaintStatus
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.MerchantStatus
import com.sisaguna.android.data.remote.ApiException
import com.sisaguna.android.data.repository.FakeAdminRepository
import com.sisaguna.android.testutil.MainDispatcherRule
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdminViewModelsTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val repo = FakeAdminRepository()
    private val messenger = AppMessenger()

    // ---- stores

    @Test
    fun `the store queue starts on pending and a decision leaves it at once`() {
        val vm = AdminMerchantsViewModel(repo, messenger)
        assertEquals(listOf("m1"), vm.state.value.items.map { it.id })
        vm.decide(vm.state.value.items.single(), approve = true)
        assertTrue(vm.state.value.items.isEmpty())
        assertTrue("merchant:m1:true" in repo.calls)
    }

    @Test
    fun `a refused decision brings the store back`() {
        val vm = AdminMerchantsViewModel(repo, messenger)
        repo.failWith = ApiException(403, "forbidden", "tidak boleh")
        vm.decide(vm.state.value.items.single(), approve = false)
        assertEquals(listOf("m1"), vm.state.value.items.map { it.id })
    }

    @Test
    fun `changing the status filter reloads with it`() {
        val vm = AdminMerchantsViewModel(repo, messenger)
        vm.setStatus(MerchantStatus.APPROVED)
        assertEquals(listOf("m2"), vm.state.value.items.map { it.id })
        assertEquals(MerchantStatus.APPROVED, repo.requestedMerchantStatuses.last())
        vm.setStatus(null)
        assertEquals(2, vm.state.value.items.size)
    }

    // ---- complaints

    @Test
    fun `closing a complaint needs a note and never reaches the server without one`() {
        val vm = AdminComplaintsViewModel(repo, messenger)
        val complaint = vm.state.value.items.single()
        var done = false
        vm.update(complaint, ComplaintStatus.RESOLVED, "  ") { done = true }
        assertFalse(done)
        assertTrue(repo.calls.none { it.startsWith("complaint:") })
    }

    @Test
    fun `a note lets the complaint be resolved and the list reloads`() {
        val vm = AdminComplaintsViewModel(repo, messenger)
        var done = false
        vm.update(vm.state.value.items.single(), ComplaintStatus.RESOLVED, "Dana dikembalikan") { done = true }
        assertTrue(done)
        assertTrue("complaint:c1:RESOLVED:Dana dikembalikan" in repo.calls)
        assertTrue(vm.state.value.items.isEmpty()) // the default filter shows only open (submitted) ones
    }

    @Test
    fun `reviewing needs no note`() {
        val vm = AdminComplaintsViewModel(repo, messenger)
        var done = false
        vm.update(vm.state.value.items.single(), ComplaintStatus.REVIEWING, "") { done = true }
        assertTrue(done)
    }

    // ---- vouchers

    private fun vouchersVm() = AdminVouchersViewModel(repo, messenger).also { it.clock = { Instant.parse("2026-10-18T00:00:00Z") } }

    @Test
    fun `voucher rules are checked before anything is sent`() {
        assertEquals("Kode 4-20 karakter: huruf besar dan angka saja.", VoucherForm(code = "ab").error())
        assertEquals("Judul wajib diisi.", VoucherForm(code = "HEMAT20").error())
        assertEquals("Isi besar diskon.", VoucherForm(code = "HEMAT20", title = "Hemat").error())
        assertEquals("Persen diskon antara 1 dan 100.", VoucherForm(code = "HEMAT20", title = "Hemat", value = "150").error())
        assertNull(VoucherForm(code = "HEMAT20", title = "Hemat", value = "20").error())
        assertEquals("Kuota minimal 1.", VoucherForm(code = "HEMAT20", title = "Hemat", value = "20", quota = "0").error())
    }

    @Test
    fun `creating sends one kind of discount and an expiry counted from now`() {
        val vm = vouchersVm()
        vm.newVoucher()
        vm.update {
            copy(
                code = "HEMAT20", title = "Hemat 20", kind = DiscountKind.PERCENT, value = "20", maxDiscount = "8000",
                minSpend = "15000", tier = ListingTier.ANIMAL_FEED, validDays = 14, quota = "50", terms = "Satu per akun\n\n Tunai tidak berlaku ",
            )
        }
        vm.save()

        val sent = repo.lastNew!!
        assertEquals(20, sent.percentOff)
        assertNull(sent.flatOff)
        assertEquals(8000, sent.maxDiscount)
        assertEquals(ListingTier.ANIMAL_FEED, sent.appliesToTier)
        assertEquals(Instant.parse("2026-10-18T00:00:00Z").plus(14, ChronoUnit.DAYS), sent.expiresAt)
        assertEquals(50, sent.quota)
        assertEquals(listOf("Satu per akun", "Tunai tidak berlaku"), sent.terms)
        assertNull(vm.form.value) // the sheet closes
        assertTrue(vm.state.value.items.any { it.code == "HEMAT20" })
    }

    @Test
    fun `a flat discount carries no percent and no cap`() {
        val vm = vouchersVm()
        vm.newVoucher()
        vm.update { copy(code = "POTONG5K", title = "Potong", kind = DiscountKind.FLAT, value = "5000", maxDiscount = "9999") }
        vm.save()
        val sent = repo.lastNew!!
        assertEquals(5000, sent.flatOff)
        assertNull(sent.percentOff)
        assertNull(sent.maxDiscount)
    }

    @Test
    fun `an invalid form stays open and sends nothing`() {
        val vm = vouchersVm()
        vm.newVoucher()
        vm.save()
        assertNotNull(vm.form.value)
        assertTrue(vm.form.value!!.submitted)
        assertTrue(repo.calls.none { it.startsWith("create") })
    }

    @Test
    fun `editing keeps the expiry unless it is extended`() {
        val vm = vouchersVm()
        vm.edit(repo.vouchers.single())
        vm.update { copy(title = "Hemat banget") }
        vm.save()
        assertEquals("Hemat banget", repo.lastPatch!!.title)
        assertNull(repo.lastPatch!!.expiresAt)

        vm.edit(repo.vouchers.single())
        vm.update { copy(validDays = 30, extend = true) }
        vm.save()
        assertEquals(Instant.parse("2026-10-18T00:00:00Z").plus(30, ChronoUnit.DAYS), repo.lastPatch!!.expiresAt)
    }

    @Test
    fun `the switch flips at once and reverts when refused`() {
        val vm = vouchersVm()
        val v = vm.state.value.items.single()
        vm.setActive(v, false)
        assertFalse(vm.state.value.items.single().isActive)

        repo.failWith = ApiException(409, "x", "gagal")
        vm.setActive(vm.state.value.items.single(), true)
        assertFalse(vm.state.value.items.single().isActive) // reverted to what the server still has
    }

    @Test
    fun `load more appends the next page`() {
        repo.totalPages = 2
        val vm = vouchersVm()
        assertTrue(vm.state.value.hasMore)
        vm.loadMore()
        assertEquals(2, vm.state.value.items.size)
        assertFalse(vm.state.value.hasMore)
    }
}
