package com.sisaguna.android.feature.activity

import androidx.lifecycle.SavedStateHandle
import com.sisaguna.android.data.model.OrderStatus
import com.sisaguna.android.data.repository.FakeNotificationRepository
import com.sisaguna.android.data.repository.FakeOrderRepository
import com.sisaguna.android.navigation.Screen
import com.sisaguna.android.testutil.MainDispatcherRule
import com.sisaguna.android.testutil.TestListingRepository
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActivityViewModelsTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val orders = FakeOrderRepository(TestListingRepository(), FakeNotificationRepository(), { Instant.now() }, 0)

    @Test
    fun `seed splits into ongoing and history with impact from completed`() {
        val s = ActivityViewModel(orders).uiState.value
        assertEquals(listOf("o4"), s.ongoing.map { it.id })
        assertEquals(listOf("o3", "o2", "o1"), s.history.map { it.id })
        assertEquals(4, s.impact.portions) // 1 donat + 3 from o2
        assertEquals(1, s.toRateCount)
    }

    @Test
    fun `history filter`() {
        val vm = ActivityViewModel(orders)
        vm.onFilter(HistoryFilter.CANCELLED)
        assertEquals(listOf("o1"), vm.uiState.value.history.map { it.id })
    }

    @Test
    fun `picking up moves order to history and makes it rateable`() {
        val activity = ActivityViewModel(orders)
        val detail = OrderDetailViewModel(orders, SavedStateHandle(mapOf(Screen.OrderDetail.ARG_ID to "o4")))
        detail.markPickedUp()
        assertTrue(activity.uiState.value.ongoing.isEmpty())
        assertEquals(OrderStatus.COMPLETED, detail.order.value?.status)
        assertTrue(detail.order.value!!.canRate)
        detail.rate(4, listOf("Ramah"), "  oke ")
        assertEquals("oke", detail.order.value?.rating?.comment)
        assertEquals(1, activity.uiState.value.toRateCount) // only seed o3 left to rate
    }

    @Test
    fun `rating tags switch at four stars`() {
        assertTrue("Ramah" in ratingTags(4))
        assertTrue("Kurang segar" in ratingTags(3))
    }
}
