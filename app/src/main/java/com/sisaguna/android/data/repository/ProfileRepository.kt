package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.ImpactStats
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.UserProfile
import kotlinx.coroutines.flow.StateFlow
import com.sisaguna.android.data.remote.AppException

interface ProfileRepository {
    val profile: StateFlow<UserProfile>

    /** Throws [AppException] when the server refuses (e.g. `email_taken`). */
    suspend fun update(name: String, email: String, phone: String)

    /** [uri] is a local photo to upload, or null to remove the current one. */
    suspend fun setPhoto(uri: String?)

    /** Picks a fruit avatar and drops the photo, so the avatar is what shows. */
    suspend fun setAvatar(key: String)
    suspend fun getImpact(): ImpactStats
    suspend fun getMyCatalog(): List<Listing>

    /** False when [current] doesn't match. */
    suspend fun changePassword(current: String, new: String): Boolean
}
