package com.sisaguna.android.feature.saved

import androidx.lifecycle.SavedStateHandle
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.repository.FakeSavedMerchantRepository
import com.sisaguna.android.data.repository.InMemoryCartRepository
import com.sisaguna.android.navigation.Screen
import com.sisaguna.android.testutil.MainDispatcherRule
import com.sisaguna.android.testutil.TestListingRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SavedViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val SavedViewModel.ids get() = (uiState.value as SavedUiState.Success).merchants.map { it.merchant.id }

    @Test
    fun `list reflects unsave and undo in original order`() {
        val vm = SavedViewModel(TestListingRepository(), FakeSavedMerchantRepository())
        assertEquals(listOf("m1", "m2", "m4", "m5"), vm.ids)
        vm.unsave("m2")
        assertEquals(listOf("m1", "m4", "m5"), vm.ids)
        vm.undo("m2")
        assertEquals(listOf("m1", "m2", "m4", "m5"), vm.ids)
    }

    @Test
    fun `empty when all removed`() {
        val vm = SavedViewModel(TestListingRepository(), FakeSavedMerchantRepository())
        listOf("m1", "m2", "m4", "m5").forEach(vm::unsave)
        assertTrue((vm.uiState.value as SavedUiState.Success).merchants.isEmpty())
    }

    @Test
    fun `suggestions are unsaved merchants with food and saving moves them`() {
        val vm = SavedViewModel(TestListingRepository(), FakeSavedMerchantRepository())
        vm.unsave("m4")
        val s = vm.uiState.value as SavedUiState.Success
        assertEquals(listOf("m4"), s.suggestions.map { it.merchant.id })
        vm.save("m4")
        assertTrue((vm.uiState.value as SavedUiState.Success).suggestions.isEmpty())
        assertEquals(3, (vm.uiState.value as SavedUiState.Success).merchants.first { it.merchant.id == "m1" }.availableCount)
    }

    @Test
    fun `distance is nearest listing of the merchant`() {
        val vm = SavedViewModel(TestListingRepository(), FakeSavedMerchantRepository())
        val m4 = (vm.uiState.value as SavedUiState.Success).merchants.first { it.merchant.id == "m4" }
        assertEquals(5.4, m4.distanceKm!!, 0.001)
    }
}

class MerchantDetailViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private fun vm(id: String, saved: FakeSavedMerchantRepository = FakeSavedMerchantRepository()) =
        MerchantDetailViewModel(TestListingRepository(), saved, InMemoryCartRepository(), SavedStateHandle(mapOf(Screen.MerchantDetail.ARG_ID to id)), com.sisaguna.android.data.repository.FakeReviewRepository())

    private val MerchantDetailViewModel.success get() = uiState.value as MerchantDetailUiState.Success

    @Test
    fun `unknown id gives not found`() {
        assertEquals(MerchantDetailUiState.NotFound, vm("nope").uiState.value)
    }

    @Test
    fun `tier filter and search compose`() {
        val vm = vm("m1")
        assertEquals(3, vm.success.listings.size)
        vm.onTierToggle(ListingTier.ANIMAL_FEED)
        assertEquals(listOf("l2"), vm.success.listings.map { it.id })
        vm.onQueryChange("nasi")
        assertTrue(vm.success.listings.isEmpty())
        vm.onTierToggle(ListingTier.ANIMAL_FEED) // clears tier
        assertEquals(listOf("l1"), vm.success.listings.map { it.id })
    }

    @Test
    fun `toggleSave unsaves then re-saves`() {
        val saved = FakeSavedMerchantRepository()
        val vm = vm("m2", saved)
        assertTrue(vm.success.isSaved)
        assertFalse(vm.toggleSave())
        assertFalse(vm.success.isSaved)
        assertTrue(vm.toggleSave())
        assertEquals(listOf("m1", "m2", "m4", "m5"), saved.savedIds.value.toList())
    }
}
