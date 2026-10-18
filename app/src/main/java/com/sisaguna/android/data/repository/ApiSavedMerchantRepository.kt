package com.sisaguna.android.data.repository

import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.remote.apiCall
import com.sisaguna.android.data.remote.apiData
import com.sisaguna.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Followed stores, kept on the server. Changes show instantly and roll back with a message if refused. */
@Singleton
class ApiSavedMerchantRepository @Inject constructor(
    private val api: ApiService,
    private val auth: AuthRepository,
    private val messenger: AppMessenger,
    @ApplicationScope private val scope: CoroutineScope,
) : SavedMerchantRepository {

    private val _savedIds = MutableStateFlow<Set<String>>(emptySet())
    override val savedIds: StateFlow<Set<String>> = _savedIds.asStateFlow()

    /** Where an id sat before [unsave], so "Batalkan" puts it back in the same place. */
    private val removedAt = mutableMapOf<String, Int>()

    init {
        scope.launch {
            auth.isAuthenticated.collect { signedIn ->
                if (!signedIn) {
                    _savedIds.value = emptySet()
                    return@collect
                }
                try {
                    _savedIds.value = LinkedHashSet(apiData { api.savedMerchants(null, null) }.map { it.id })
                } catch (e: AppException) {
                    messenger.show(e)
                }
            }
        }
    }

    override fun unsave(id: String) {
        val list = _savedIds.value.toList()
        val index = list.indexOf(id)
        if (index < 0) return
        removedAt[id] = index
        _savedIds.value = LinkedHashSet(list - id)
        sync(rollback = { insert(id, index) }) { api.unsaveMerchant(id) }
    }

    override fun restore(id: String) {
        if (id in _savedIds.value) return
        insert(id, removedAt.remove(id))
        sync(rollback = { _savedIds.value = LinkedHashSet(_savedIds.value - id) }) { api.saveMerchant(id) }
    }

    private fun insert(id: String, at: Int?) {
        val list = _savedIds.value.toMutableList()
        list.add((at ?: list.size).coerceIn(0, list.size), id)
        _savedIds.value = LinkedHashSet(list)
    }

    private fun sync(rollback: () -> Unit, call: suspend () -> Unit) {
        scope.launch {
            try {
                apiCall { call() }
            } catch (e: AppException) {
                rollback()
                messenger.show(e)
            }
        }
    }
}
