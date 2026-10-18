package com.sisaguna.android.data.repository

import com.sisaguna.android.core.ui.AppMessenger
import com.sisaguna.android.data.model.GeoPoint
import com.sisaguna.android.data.model.HalalStatus
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.remote.ApiException
import com.sisaguna.android.data.remote.ApiService
import com.sisaguna.android.data.remote.AppException
import com.sisaguna.android.data.remote.RealtimeClient
import com.sisaguna.android.data.remote.RealtimeEvent
import com.sisaguna.android.data.remote.apiCall
import com.sisaguna.android.data.remote.apiData
import com.sisaguna.android.data.remote.dto.CreateListingRequest
import com.sisaguna.android.data.remote.dto.CreateMerchantBody
import com.sisaguna.android.data.remote.dto.MerchantDto
import com.sisaguna.android.data.remote.toListing
import com.sisaguna.android.data.remote.toMerchant
import com.sisaguna.android.di.ApplicationScope
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Catalog from sisaguna-be. Everything fetched is also remembered in two caches ([listings] and
 * [merchant]) because several screens look a store or listing up synchronously by id; a
 * lightweight card never overwrites the richer fields a detail call already brought in.
 */
@Singleton
class ApiListingRepository @Inject constructor(
    private val api: ApiService,
    private val addresses: AddressRepository,
    private val auth: AuthRepository,
    private val media: MediaRepository,
    private val messenger: AppMessenger,
    realtime: RealtimeClient,
    @ApplicationScope private val scope: CoroutineScope,
) : ListingRepository {

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    override val listings: StateFlow<List<Listing>> = _listings.asStateFlow()

    private val _myMerchant = MutableStateFlow<Merchant?>(null)
    override val myMerchant: StateFlow<Merchant?> = _myMerchant.asStateFlow()

    private val merchants = MutableStateFlow<Map<String, Merchant>>(emptyMap())

    init {
        scope.launch {
            auth.isAuthenticated.collect { signedIn -> if (signedIn) loadMine() else forgetMine() }
        }
    }

    init {
        scope.launch {
            realtime.events.collect { event ->
                when (event) {
                    is RealtimeEvent.StockChanged -> setStock(event.listingId, event.stock)
                    // An admin approved/suspended the store (or we were offline meanwhile): look again.
                    RealtimeEvent.MerchantStatusChanged, RealtimeEvent.Connected -> if (auth.isAuthenticated.value) loadMine()
                    else -> Unit
                }
            }
        }
    }

    private fun setStock(listingId: String, stock: Int) {
        _listings.update { list -> list.map { if (it.id == listingId) it.copy(stock = stock) else it } }
    }

    private val point: GeoPoint? get() = addresses.searchLocation.value

    // ------------------------------------------------------------ feeds

    override suspend fun getHomeFeed(focusTier: ListingTier, maxKm: Double?): HomeFeed {
        val home = apiData { api.home(point?.latitude, point?.longitude, maxKm) }
        val cards = home.nearby + home.deals + home.popular + home.animalFeed + home.compost
        cards.forEach { cacheMerchant(it.merchant, full = false) }
        home.offerMerchants.forEach { cacheMerchant(it, full = false) }
        cacheListings(cards.map { it.toListing() })

        // todays_offer belongs to the current offer list only; drop a stale one remembered earlier.
        val offers = home.offerMerchants.associate { it.id to it.todaysOffer }
        val seen = (cards.map { it.merchant.id } + home.offerMerchants.map { it.id }).toSet()
        val byId = seen.mapNotNull { id -> merchants.value[id]?.copy(todaysOffer = offers[id]) }.associateBy { it.id }
        return HomeFeed(
            nearby = home.nearby.map { it.toListing() },
            deals = home.deals.map { it.toListing() },
            animalFeed = home.animalFeed.map { it.toListing() },
            compost = home.compost.map { it.toListing() },
            merchantsById = byId,
            popular = home.popular.map { it.toListing() },
        )
    }

    // shortcut: first page only (the API caps a page at 50), add paging when a tier outgrows it.
    override suspend fun getListingsByTier(tier: ListingTier): CategoryFeed {
        val cards = apiData { api.listings(tier = tier.name.lowercase(), lat = point?.latitude, lng = point?.longitude) }
        cards.forEach { cacheMerchant(it.merchant, full = false) }
        val listings = cards.map { it.toListing() }
        cacheListings(listings)
        return CategoryFeed(listings, cards.mapNotNull { merchants.value[it.merchant.id] }.associateBy { it.id })
    }

    override suspend fun getMerchants(): List<Merchant> {
        val available = apiData { api.merchants(point?.latitude, point?.longitude) }
        val saved = if (auth.isAuthenticated.value) apiData { api.savedMerchants(point?.latitude, point?.longitude) } else emptyList()
        return (available + saved).distinctBy { it.id }.map { cacheMerchant(it, full = false) }
    }

    override fun merchant(id: String): Merchant? = merchants.value[id]

    override suspend fun getMerchantFeed(merchantId: String): MerchantFeed? = try {
        val dto = apiData { api.merchant(merchantId, point?.latitude, point?.longitude) }
        val merchant = cacheMerchant(dto, full = true)
        val listings = dto.listings.map { it.toListing() }
        cacheListings(listings)
        MerchantFeed(merchant, listings)
    } catch (e: ApiException) {
        if (e.status == 404) null else throw e
    }

    override suspend fun getListingDetail(listingId: String): ListingDetail? = try {
        val dto = apiData { api.listing(listingId, point?.latitude, point?.longitude) }
        // The detail merchant has about/address/hours but not a store-page's photos: keep those.
        val merchant = cacheMerchant(dto.merchant, full = false, detail = true)
        val listing = dto.toListing()
        val more = dto.moreFromMerchant.map { it.toListing() }
        cacheListings(listOf(listing) + more)
        ListingDetail(listing, merchant, more)
    } catch (e: ApiException) {
        if (e.status == 404) null else throw e
    }

    // ------------------------------------------------------------ my store

    override suspend fun getMyListings(): List<Listing> {
        val mine = myMerchant.value?.id ?: return emptyList()
        val rows = apiData { api.myListings() }.map { it.toListing(mine) }
        // Replace the user's rows wholesale: deleted/expired ones must disappear from the cache too.
        _listings.update { current -> current.filterNot { it.merchantId == mine } }
        cacheListings(rows)
        return rows
    }

    override suspend fun createStore(draft: StoreDraft): Merchant {
        val body = CreateMerchantBody(
            name = draft.name,
            about = draft.about,
            address = draft.address,
            lat = draft.latitude,
            lng = draft.longitude,
            locationLabel = draft.locationLabel,
            openHours = draft.openHours,
            halal = draft.halal.name.lowercase(),
            allergens = draft.allergens.map { it.name.lowercase() },
            prepMinutes = draft.prepMinutes,
        )
        val store = cacheMerchant(apiData { api.createMerchant(body) }, full = true)
        _myMerchant.value = store
        return store
    }

    override suspend fun createListing(draft: ListingDraft, idempotencyKey: String): Listing {
        val mine = myMerchant.value?.id ?: throw ApiException(403, "merchant_required", "Daftarkan toko dulu untuk mulai berjualan.")
        // Uploaded one by one on purpose: a failure then leaves at most a few orphan files,
        // which the server deletes after 24 hours.
        val photoIds = draft.photoUris.map { media.upload(it, MediaPurpose.LISTING).id }
        val videoId = draft.videoUri?.let { media.upload(it, MediaPurpose.LISTING).id }
        val request = CreateListingRequest(
            title = draft.title,
            description = draft.description,
            tier = draft.tier.name.lowercase(),
            isFree = draft.isFree,
            price = draft.price.takeUnless { draft.isFree },
            priceOriginal = draft.priceOriginal,
            stock = draft.stock,
            halal = draft.halal.name.lowercase(),
            allergens = draft.allergens.map { it.name.lowercase() },
            madeAt = draft.madeAt?.toString(),
            storage = draft.storage.name.lowercase(),
            safetyChecks = draft.safetyChecks.map { it.name.lowercase() },
            pickupHours = draft.pickupHours,
            photoMediaIds = photoIds,
            videoMediaId = videoId,
        )
        val created = apiData { api.createListing(idempotencyKey, request) }.toListing().copy(merchantId = mine)
        cacheListings(listOf(created))
        return created
    }

    override suspend fun deleteListing(listingId: String) {
        apiCall { api.deleteListing(listingId) }
        _listings.update { list -> list.filterNot { it.id == listingId } }
    }

    private suspend fun loadMine() {
        try {
            _myMerchant.value = cacheMerchant(apiData { api.myMerchant() }, full = true)
            getMyListings()
        } catch (e: ApiException) {
            // 404: a plain buyer account without a store.
            if (e.status != 404) messenger.show(e)
        } catch (e: AppException) {
            messenger.show(e)
        }
    }

    private fun forgetMine() {
        val mine = _myMerchant.value?.id
        _myMerchant.value = null
        if (mine != null) _listings.update { list -> list.filterNot { it.merchantId == mine } }
    }

    // ------------------------------------------------------------ caches

    private fun cacheListings(incoming: List<Listing>) {
        _listings.update { current ->
            val byId = LinkedHashMap<String, Listing>(current.size + incoming.size)
            current.forEach { byId[it.id] = it }
            incoming.forEach { new -> byId[new.id] = byId[new.id]?.let { old -> keepRicher(old, new) } ?: new }
            byId.values.toList()
        }
    }

    /** A feed card carries no description/gallery/safety data; don't wipe what a detail call loaded. */
    private fun keepRicher(old: Listing, new: Listing): Listing = new.copy(
        description = new.description.ifBlank { old.description },
        gallery = new.gallery.ifEmpty { old.gallery },
        madeAt = new.madeAt ?: old.madeAt,
        videoUrl = new.videoUrl ?: old.videoUrl,
        safetyChecks = new.safetyChecks.ifEmpty { old.safetyChecks },
        allergens = new.allergens.ifEmpty { old.allergens },
        halal = if (new.halal == HalalStatus.UNVERIFIED) old.halal else new.halal,
        distanceKm = new.distanceKm ?: old.distanceKm,
    )

    /**
     * [full] = a whole store page or `/me/merchant`, which replaces the entry. Otherwise the DTO is a
     * partial view (a card on a listing, a store-list row, a listing's merchant block) and only
     * the fields it really carries are merged in.
     */
    private fun cacheMerchant(dto: MerchantDto, full: Boolean, detail: Boolean = false): Merchant {
        val incoming = dto.toMerchant()
        val old = merchants.value[dto.id]
        val merged = when {
            full || old == null -> incoming
            else -> old.copy(
                name = incoming.name,
                isVerified = incoming.isVerified,
                location = incoming.location.ifBlank { old.location },
                rating = incoming.rating ?: old.rating,
                ratingCount = incoming.ratingCount,
                bannerUrl = incoming.bannerUrl.ifBlank { old.bannerUrl },
                todaysOffer = incoming.todaysOffer ?: old.todaysOffer,
                distanceKm = incoming.distanceKm ?: old.distanceKm,
                availableCount = if (dto.nextPickupEnd != null || dto.availableCount > 0) incoming.availableCount else old.availableCount,
                nextPickupEnd = incoming.nextPickupEnd ?: old.nextPickupEnd,
                ratingBreakdown = incoming.ratingBreakdown.ifEmpty { old.ratingBreakdown },
            ).let { base ->
                if (!detail) base else base.copy(
                    about = incoming.about, address = incoming.address, openHours = incoming.openHours, halal = incoming.halal,
                    allergens = incoming.allergens, deliveryAvailable = incoming.deliveryAvailable, prepMinutes = incoming.prepMinutes,
                )
            }
        }
        merchants.update { it + (merged.id to merged) }
        return merged
    }
}
