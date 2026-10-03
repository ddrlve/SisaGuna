package com.sisaguna.android.feature.upload

import com.sisaguna.android.data.model.SafetyCheck
import com.sisaguna.android.data.model.StorageMethod
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.testutil.MainDispatcherRule
import com.sisaguna.android.testutil.TestListingRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class UploadViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val now = Instant.parse("2026-10-03T05:00:00Z")
    private val repo = TestListingRepository()
    private fun vm() = UploadViewModel(repo, { now })

    @Test
    fun `invalid publish reveals errors and adds nothing`() {
        val vm = vm()
        assertNull(vm.publish())
        assertEquals("Nama makanan wajib diisi", vm.form.value.visibleError(UploadField.TITLE))
        assertEquals(5, repo.listings.value.size)
    }

    @Test
    fun `price must be below original`() {
        val vm = vm()
        vm.update { copy(originalPrice = "10.000", price = "12000") }
        assertEquals("Harga jual harus lebih murah dari harga normal", vm.form.value.error(UploadField.PRICE))
    }

    @Test
    fun `free listing skips price checks`() {
        val vm = vm()
        vm.update { copy(isFree = true) }
        assertNull(vm.form.value.error(UploadField.PRICE))
        assertNull(vm.form.value.error(UploadField.ORIGINAL_PRICE))
    }

    @Test
    fun `valid publish lands in my store with pickup window`() {
        val vm = vm()
        vm.update {
            copy(
                title = "Bolu Pandan", tier = ListingTier.HUMAN,
                description = "Bolu pandan sisa acara, masih lembut.",
                originalPrice = "40000", price = "15000", stock = 3, pickupHours = 4,
                madeHoursAgo = 0, storage = StorageMethod.SHELF, checks = SafetyCheck.entries.toSet(),
            )
        }
        val id = vm.publish()
        assertNotNull(id)
        val listing = repo.listings.value.first()
        assertEquals(id, listing.id)
        assertEquals(ListingRepository.MY_MERCHANT_ID, listing.merchantId)
        assertEquals(15000, listing.unitPrice)
        assertEquals(now.plus(4, ChronoUnit.HOURS), listing.pickupEnd)
    }

    @Test
    fun `suggested price is 40 percent rounded down to 500`() {
        assertEquals(15500, UploadForm(originalPrice = "39000").suggestedPrice)
    }

    @Test
    fun `food past its safe window cannot be published as human food`() {
        val vm = vm()
        vm.update {
            copy(
                title = "Nasi Rames", tier = ListingTier.HUMAN, description = "Nasi rames sisa makan siang kantor.",
                isFree = true, madeHoursAgo = 6, storage = StorageMethod.ROOM_TEMP, checks = SafetyCheck.entries.toSet(),
            )
        }
        assertNull(vm.publish())
        assertNotNull(vm.form.value.error(UploadField.SAFETY, now))
        vm.switchToFeed()
        vm.update { copy(weight = "3") }
        assertNotNull(vm.publish())
    }

    @Test
    fun `feed is weighed and grams round up to whole kilos`() {
        val form = UploadForm(tier = ListingTier.ANIMAL_FEED, weight = "1500", weightUnit = WeightUnit.GRAM)
        assertEquals(1.5, form.weightKg!!, 0.001)
        assertEquals(2, form.stockUnits)
    }

    @Test
    fun `pickup window never outlives the safe-to-eat time`() {
        val vm = vm()
        vm.update {
            copy(
                title = "Sop Ayam", tier = ListingTier.HUMAN, description = "Sop ayam kampung sisa katering.",
                isFree = true, madeHoursAgo = 3, storage = StorageMethod.ROOM_TEMP, pickupHours = 8, checks = SafetyCheck.entries.toSet(),
            )
        }
        vm.publish()
        assertEquals(now.plus(1, ChronoUnit.HOURS), repo.listings.value.first().pickupEnd)
    }
}
