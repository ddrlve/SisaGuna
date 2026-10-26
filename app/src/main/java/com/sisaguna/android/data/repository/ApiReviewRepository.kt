package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.Review
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.apiData
import com.sisaguna.android.data.remote.toReview
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Store reviews, fetched per store on demand. Reviews are created only from a finished order
 * (`POST /orders/:id/review`), never here, and the server shortens author names ("Dinda P.").
 */
@Singleton
class ApiReviewRepository @Inject constructor(
    private val api: ApiService,
) : ReviewRepository {

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    override val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    // shortcut: the newest 50 reviews of a store; add cursor paging (`meta.next_cursor`) for "show more".
    override suspend fun load(merchantId: String) {
        val fresh = apiData { api.merchantReviews(merchantId) }.map { it.toReview(merchantId) }
        _reviews.update { all -> (all.filterNot { it.merchantId == merchantId } + fresh).sortedByDescending { it.createdAt } }
    }
}
