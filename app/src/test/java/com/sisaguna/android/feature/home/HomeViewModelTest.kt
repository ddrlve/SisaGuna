package com.sisaguna.android.feature.home

import com.sisaguna.android.data.repository.FakeNotificationRepository
import com.sisaguna.android.data.repository.FakeVoucherRepository
import com.sisaguna.android.testutil.MainDispatcherRule
import com.sisaguna.android.testutil.TestListingRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private fun viewModel() = HomeViewModel(TestListingRepository(), FakeNotificationRepository(), FakeVoucherRepository(), com.sisaguna.android.data.repository.InMemoryCartRepository(), com.sisaguna.android.data.settings.AppSettingsRepository(null))

    private val HomeViewModel.success get() = uiState.value as HomeUiState.Success

    @Test
    fun `default tab is Siap Santap`() {
        assertEquals(HomeTab.SIAP_SANTAP, viewModel().success.selectedTab)
    }

    @Test
    fun `selected tab survives retry`() {
        val vm = viewModel()
        vm.onTabSelected(HomeTab.TERNAK_KOMPOS)
        vm.retry()
        assertEquals(HomeTab.TERNAK_KOMPOS, vm.success.selectedTab)
    }

    @Test
    fun `search query survives tab switch`() {
        val vm = viewModel()
        vm.onSearchQueryChange("ampas")
        vm.onTabSelected(HomeTab.TERNAK_KOMPOS)
        assertEquals("ampas", vm.success.searchQuery)
        assertEquals(1, vm.success.animalFeed.size)
    }

    @Test
    fun `other tab hint shows while searching with empty active tab`() {
        val vm = viewModel()
        vm.onTabSelected(HomeTab.TERNAK_KOMPOS)
        vm.onSearchQueryChange("nasi") // only a human listing matches
        assertTrue(vm.success.activeTabIsEmpty)
        assertEquals(1, vm.success.otherTabMatchCount)
    }

    @Test
    fun `other tab hint is zero without a query`() {
        val vm = viewModel()
        vm.onSearchQueryChange("nasi")
        vm.onTabSelected(HomeTab.TERNAK_KOMPOS)
        vm.onSearchQueryChange("")
        assertEquals(0, vm.success.otherTabMatchCount)
    }

    @Test
    fun `other tab hint is zero when active tab has matches`() {
        val vm = viewModel()
        vm.onSearchQueryChange("a") // matches in both tabs
        assertEquals(0, vm.success.otherTabMatchCount)
    }

    @Test
    fun `free only filter with nearest sort`() {
        val vm = viewModel()
        vm.onTabSelected(HomeTab.TERNAK_KOMPOS)
        vm.onFilterChange(HomeFilter(freeOnly = true, sort = HomeSort.NEAREST))
        assertTrue(vm.success.animalFeed.all { it.listing.isFree })
        assertEquals(listOf("l2", "l5"), vm.success.animalFeed.map { it.listing.id })
        assertEquals(2, vm.success.filter.activeCount)
    }

    @Test
    fun `distance filter can empty a tab and point to the other`() {
        val vm = viewModel()
        vm.onTabSelected(HomeTab.TERNAK_KOMPOS)
        vm.onFilterChange(HomeFilter(maxDistanceKm = 1.0))
        assertEquals(listOf("l2"), vm.success.animalFeed.map { it.listing.id })
        vm.onFilterChange(HomeFilter(maxDistanceKm = 0.5))
        assertTrue(vm.success.activeTabIsEmpty)
        assertEquals(0, vm.success.otherTabMatchCount) // nothing within 0.5 km anywhere
    }

    @Test
    fun `farm count sums animal feed and compost`() {
        assertEquals(3, viewModel().success.farmCount)
    }
}
