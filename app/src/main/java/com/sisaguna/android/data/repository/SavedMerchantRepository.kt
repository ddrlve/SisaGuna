package com.sisaguna.android.data.repository

import kotlinx.coroutines.flow.StateFlow

/** Merchants the user follows. Insertion-ordered so the Saved list keeps a stable order. */
interface SavedMerchantRepository {
    val savedIds: StateFlow<Set<String>>
    fun unsave(id: String)

    /** Puts [id] back where it was before [unsave] (Snackbar "Batalkan"); appends if unknown. */
    fun restore(id: String)
}
