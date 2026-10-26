package com.sisaguna.android.data.repository

import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.model.ImpactStats
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.UserProfile
import com.sisaguna.android.data.remote.ApiException
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.remote.TokenStore
import com.sisaguna.android.data.remote.Tokens
import com.sisaguna.android.data.remote.apiData
import com.sisaguna.android.data.remote.dto.ChangePasswordRequest
import com.sisaguna.android.data.remote.dto.UserDto
import com.sisaguna.android.data.remote.toProfile
import com.sisaguna.android.di.ApplicationScope
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Singleton
class ApiProfileRepository @Inject constructor(
    private val api: ApiService,
    private val auth: AuthRepository,
    private val tokens: TokenStore,
    private val media: MediaRepository,
    private val listingRepository: ListingRepository,
    private val messenger: AppMessenger,
    @ApplicationScope scope: CoroutineScope,
) : ProfileRepository {

    private val _profile = MutableStateFlow(SIGNED_OUT)
    override val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    init {
        scope.launch {
            auth.isAuthenticated.collect { signedIn ->
                if (signedIn) refresh() else _profile.value = SIGNED_OUT
            }
        }
    }

    private suspend fun refresh() {
        try {
            _profile.value = apiData { api.me() }.toProfile()
        } catch (e: AppException) {
            messenger.show(e)
        }
    }

    override suspend fun update(name: String, email: String, phone: String) = patch(
        buildJsonObject {
            put("name", name.trim())
            put("email", email.trim())
            put("phone", phone)
        },
    )

    override suspend fun setPhoto(uri: String?) {
        val mediaId = uri?.let { media.upload(it, MediaPurpose.AVATAR).id }
        // photo_media_id: null removes the photo, so the key must be sent explicitly.
        patch(JsonObject(mapOf("photo_media_id" to (mediaId?.let(::JsonPrimitive) ?: JsonNull))))
    }

    override suspend fun setAvatar(key: String) = patch(
        buildJsonObject {
            put("avatar_key", key)
            put("photo_media_id", JsonNull)
        },
    )

    private suspend fun patch(body: JsonObject) {
        val user: UserDto = apiData { api.updateMe(body) }
        _profile.value = user.toProfile()
    }

    override suspend fun getImpact(): ImpactStats {
        val dto = apiData { api.impact() }
        return ImpactStats(dto.portions, dto.feedCompostKg.roundToInt(), dto.carbonKg.roundToInt(), dto.savedRupiah)
    }

    override suspend fun getMyCatalog(): List<Listing> = listingRepository.getMyListings()

    override suspend fun changePassword(current: String, new: String): Boolean = try {
        // The server revokes every refresh token and answers with a fresh session for this device.
        val session = apiData { api.changePassword(ChangePasswordRequest(current, new)) }
        tokens.save(Tokens(session.accessToken, session.refreshToken))
        true
    } catch (e: ApiException) {
        if (e.code == "wrong_password") false else throw e
    }

    private companion object {
        val SIGNED_OUT = UserProfile(name = "", email = "", phone = "", location = "", memberSince = YearMonth.now())
    }
}
