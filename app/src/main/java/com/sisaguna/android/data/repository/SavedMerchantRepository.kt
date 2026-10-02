package com.sisaguna.android.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Merchants the user follows. Insertion-ordered so the Saved list keeps a stable order. */
interface SavedMerchantRepository {
    val savedIds: StateFlow<Set<String>>
    fun unsave(id: String)

    /** Puts [id] back where it was before [unsave] (Snackbar "Batalkan"); appends if unknown. */
    fun restore(id: String)
}

/** Seeded with 4 merchants from [FakeListingRepository] — there's no save action yet (spec §3). */
@Singleton
class FakeSavedMerchantRepository @Inject constructor() : SavedMerchantRepository {

    private val _savedIds = MutableStateFlow<Set<String>>(linkedSetOf("m1", "m2", "m4", "m5"))
    override val savedIds: StateFlow<Set<String>> = _savedIds.asStateFlow()

    private val removedAt = mutableMapOf<String, Int>()

    override fun unsave(id: String) {
        val list = _savedIds.value.toList()
        val index = list.indexOf(id)
        if (index < 0) return
        removedAt[id] = index
        _savedIds.value = LinkedHashSet(list - id)
    }

    override fun restore(id: String) {
        if (id in _savedIds.value) return
        val list = _savedIds.value.toMutableList()
        val index = (removedAt.remove(id) ?: list.size).coerceIn(0, list.size)
        list.add(index, id)
        _savedIds.value = LinkedHashSet(list)
    }
}
