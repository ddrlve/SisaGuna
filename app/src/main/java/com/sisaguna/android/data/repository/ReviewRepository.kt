package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.Review
import kotlinx.coroutines.flow.StateFlow

interface ReviewRepository {
    /** Newest first, across all stores. */
    val reviews: StateFlow<List<Review>>
    fun forMerchant(merchantId: String): List<Review> = reviews.value.filter { it.merchantId == merchantId }

    /** Fetches the newest reviews of [merchantId] into [reviews]. */
    suspend fun load(merchantId: String) {}
}
