package com.sisaguna.android.data.repository

import com.sisaguna.android.data.model.ImpactStats
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.UserProfile
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ProfileRepository {
    val profile: StateFlow<UserProfile>
    fun update(name: String, email: String, phone: String)
    fun setPhoto(uri: String?)
    suspend fun getImpact(): ImpactStats
    suspend fun getMyCatalog(): List<Listing>

    /** False when [current] doesn't match. Fake: the starting password is "sisaguna123". */
    suspend fun changePassword(current: String, new: String): Boolean
}

/** Values from Figma Profile (273:12725). The catalog is the user's own store
 * ([ListingRepository.MY_MERCHANT_ID]), where uploads land. */
@Singleton
class FakeProfileRepository @Inject constructor(
    private val listingRepository: ListingRepository,
) : ProfileRepository {

    private val _profile = MutableStateFlow(
        UserProfile(
            name = "Budi Santoso",
            email = "budi.santoso@gmail.com",
            phone = "81234567890",
            location = "Tangerang, Banten",
            memberSince = YearMonth.of(2026, 1),
        ),
    )
    override val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private var password = "sisaguna123"

    override fun update(name: String, email: String, phone: String) {
        _profile.value = _profile.value.copy(name = name.trim(), email = email.trim(), phone = phone)
    }

    override fun setPhoto(uri: String?) {
        _profile.value = _profile.value.copy(photoUri = uri)
    }

    override suspend fun getImpact(): ImpactStats {
        delay(300)
        return ImpactStats(portions = 34, compostKg = 3, carbonKg = 32, savedRupiah = 245_000)
    }

    override suspend fun getMyCatalog(): List<Listing> =
        listingRepository.listings.value.filter { it.merchantId == ListingRepository.MY_MERCHANT_ID }

    override suspend fun changePassword(current: String, new: String): Boolean {
        delay(600)
        if (current != password) return false
        password = new
        return true
    }
}
