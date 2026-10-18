package com.sisaguna.android.testutil

/** Test repositories that can lower a listing's stock, like the server does when an order is placed. */
interface StockTracking {
    fun reduceStock(listingId: String, by: Int)
}
