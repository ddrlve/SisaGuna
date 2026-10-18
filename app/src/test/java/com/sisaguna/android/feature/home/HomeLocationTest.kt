package com.sisaguna.android.feature.home

import com.sisaguna.android.data.repository.FakeAddressRepository
import com.sisaguna.android.data.repository.FakeNotificationRepository
import com.sisaguna.android.data.repository.FakeVoucherRepository
import com.sisaguna.android.data.repository.InMemoryCartRepository
import com.sisaguna.android.data.settings.AppSettingsRepository
import com.sisaguna.android.feature.address.Place
import com.sisaguna.android.testutil.MainDispatcherRule
import com.sisaguna.android.testutil.TestListingRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeLocationTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val listings = TestListingRepository()
    private val addresses = FakeAddressRepository()
    private val settings = AppSettingsRepository(null)
    // Lazy: the dispatcher rule only starts once the test method runs, after field initializers.
    private val vm by lazy { HomeViewModel(listings, FakeNotificationRepository(), FakeVoucherRepository(), InMemoryCartRepository(), settings, addresses) }

    @Test
    fun `the location card shows until a location is known`() {
        assertTrue(vm.showLocationPrompt.value)
        vm.useLocation(Place("Pinang, Tangerang", "Jl. Contoh", -6.2275, 106.6544))
        assertFalse(vm.showLocationPrompt.value)
        assertEquals("Pinang, Tangerang", addresses.currentLocationLabel.value)
        assertEquals(-6.2275, addresses.searchLocation.value?.latitude ?: 0.0, 0.0001)
    }

    @Test
    fun `saying later hides the card and is remembered`() {
        vm.dismissLocationPrompt()
        assertFalse(vm.showLocationPrompt.value)
        assertTrue(settings.settings.value.locationPromptDismissed)
    }

    @Test
    fun `picking a radius asks the server for it and clearing it removes the limit`() {
        assertNull(listings.lastHomeMaxKm)
        vm.onFilterChange(HomeFilter(maxDistanceKm = 5.0))
        assertEquals(5.0, listings.lastHomeMaxKm!!, 0.0)
        vm.onFilterChange(HomeFilter(maxDistanceKm = null))
        assertNull(listings.lastHomeMaxKm)
    }

    @Test
    fun `a new location reloads the feed`() {
        val before = listings.homeLoads
        vm.useLocation(Place("A", "A", -6.2, 106.8))
        assertTrue(listings.homeLoads > before)
    }
}
