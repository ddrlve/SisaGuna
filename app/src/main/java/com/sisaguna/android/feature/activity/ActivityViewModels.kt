package com.sisaguna.android.feature.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sisaguna.android.data.model.Order
import com.sisaguna.android.data.model.OrderRating
import com.sisaguna.android.data.model.OrderStatus
import com.sisaguna.android.data.repository.OrderRepository
import com.sisaguna.android.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class ActivityTab { ONGOING, HISTORY }

enum class HistoryFilter { ALL, COMPLETED, CANCELLED }

/** Totals across completed orders — the "your impact" strip on top of Activity. */
data class ActivityImpact(val portions: Int, val savedRupiah: Int, val completedOrders: Int)

data class ActivityUiState(
    val tab: ActivityTab = ActivityTab.ONGOING,
    val filter: HistoryFilter = HistoryFilter.ALL,
    val ongoing: List<Order> = emptyList(),
    val history: List<Order> = emptyList(),
    val impact: ActivityImpact = ActivityImpact(0, 0, 0),
    val toRateCount: Int = 0,
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    orderRepository: OrderRepository,
) : ViewModel() {

    private val tab = MutableStateFlow(ActivityTab.ONGOING)
    private val filter = MutableStateFlow(HistoryFilter.ALL)

    val uiState: StateFlow<ActivityUiState> = combine(orderRepository.orders, tab, filter) { orders, t, f ->
        val completed = orders.filter { it.status == OrderStatus.COMPLETED }
        ActivityUiState(
            tab = t,
            filter = f,
            ongoing = orders.filter { it.status == OrderStatus.READY }.sortedBy { it.pickupEnd },
            history = orders.filter { o ->
                o.status != OrderStatus.READY && when (f) {
                    HistoryFilter.ALL -> true
                    HistoryFilter.COMPLETED -> o.status == OrderStatus.COMPLETED
                    HistoryFilter.CANCELLED -> o.status == OrderStatus.CANCELLED
                }
            },
            impact = ActivityImpact(
                portions = completed.sumOf { it.itemCount },
                savedRupiah = completed.sumOf { it.savings },
                completedOrders = completed.size,
            ),
            toRateCount = orders.count { it.canRate },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ActivityUiState())

    fun onTab(value: ActivityTab) {
        tab.value = value
    }

    fun onFilter(value: HistoryFilter) {
        filter.value = value
    }
}

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>(Screen.OrderDetail.ARG_ID).orEmpty()

    /** True right after checkout — the screen shows a success banner once. */
    val justPlaced: Boolean = savedStateHandle.get<Boolean>(Screen.OrderDetail.ARG_JUST_PLACED) ?: false

    val order: StateFlow<Order?> = orderRepository.orders
        .map { list -> list.firstOrNull { it.id == orderId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, orderRepository.orders.value.firstOrNull { it.id == orderId })

    fun markPickedUp() = orderRepository.markPickedUp(orderId)

    fun cancel() = orderRepository.cancel(orderId)

    fun rate(stars: Int, tags: List<String>, comment: String) =
        orderRepository.rate(orderId, OrderRating(stars, tags, comment.trim()))

    fun complain(reason: com.sisaguna.android.data.model.ComplaintReason, detail: String, photos: List<String>) =
        orderRepository.complain(
            orderId,
            com.sisaguna.android.data.model.Complaint(reason, detail.trim(), photos, java.time.Instant.now()),
        )
}

/** Tag suggestions in the rating sheet: praise for 4–5 stars, issues for 1–3. */
fun ratingTags(stars: Int): List<String> = if (stars >= 4) {
    listOf("Makanan enak", "Porsi banyak", "Ramah", "Tepat waktu", "Kemasan rapi")
} else {
    listOf("Tidak sesuai deskripsi", "Porsi sedikit", "Kurang segar", "Harus menunggu lama", "Kurang ramah")
}

fun ratingLabel(stars: Int): String = when (stars) {
    1 -> "Sangat kurang"
    2 -> "Kurang"
    3 -> "Cukup"
    4 -> "Bagus"
    5 -> "Luar biasa!"
    else -> "Ketuk bintang untuk menilai"
}
