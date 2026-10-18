package com.sisaguna.android.data.remote

import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.remote.dto.HomeDto
import com.sisaguna.android.data.remote.dto.ListingCardDto
import com.sisaguna.android.data.remote.dto.ListingDetailDto
import com.sisaguna.android.data.remote.dto.MerchantDto
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The files in `src/test/resources/contract` are real responses from sisaguna.fadh.tech
 * (public GET endpoints, seed data). If the backend changes a field, re-download them and this
 * test shows what the Android DTOs no longer understand.
 */
class ContractFixturesTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResource("/contract/$name")) { "missing fixture $name" }.readText()

    private inline fun <reified T> data(name: String): T = ApiJson.decodeFromString<Envelope<T>>(fixture(name)).data

    @Test
    fun `home feed decodes and maps`() {
        val home = data<HomeDto>("home.json")
        val cards = home.nearby + home.deals + home.popular + home.animalFeed + home.compost
        assertTrue(cards.isNotEmpty())

        val listings = cards.map { it.toListing() }
        assertTrue(listings.all { it.id.isNotBlank() && it.merchantId.isNotBlank() && it.title.isNotBlank() })
        assertTrue(home.animalFeed.all { it.toListing().tier == ListingTier.ANIMAL_FEED })
        assertTrue(home.compost.all { it.toListing().tier == ListingTier.COMPOST })
        assertTrue(listings.any { it.distanceKm != null }) // lat/lng were sent
        assertTrue(home.offerMerchants.all { it.toMerchant().todaysOffer != null })
    }

    @Test
    fun `listing page decodes`() {
        val cards = data<List<ListingCardDto>>("listings.json")
        assertTrue(cards.isNotEmpty())
        cards.map { it.toListing() }.forEach {
            assertTrue(it.stock >= 0)
            assertEquals(ListingTier.HUMAN, it.tier)
            // price is null only for free food
            assertTrue(it.isFree || it.priceDiscounted != null)
        }
    }

    @Test
    fun `listing detail keeps photos, safety inputs and store info`() {
        val detail = data<ListingDetailDto>("listing_detail.json")
        val listing = detail.toListing()
        assertTrue(listing.description.isNotBlank())
        assertTrue(listing.imageUrl.startsWith("https://"))
        assertNotNull(listing.madeAt)
        assertTrue(listing.safetyChecks.isNotEmpty())
        val merchant = detail.merchant.toMerchant()
        assertTrue(merchant.about.isNotBlank() && merchant.address.isNotBlank())
    }

    @Test
    fun `store list rows carry availability for the Saved tab`() {
        val rows = data<List<MerchantDto>>("merchants.json").map { it.toMerchant() }
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.availableCount > 0 && it.nextPickupEnd != null })
    }

    @Test
    fun `store page decodes with its listings and gallery`() {
        val dto = data<MerchantDto>("merchant_detail.json")
        val merchant = dto.toMerchant()
        assertTrue(merchant.photos.isNotEmpty())
        assertTrue(merchant.bannerUrl.startsWith("https://"))
        assertTrue(dto.listings.all { it.merchant.id == dto.id })
        assertTrue(dto.ratingBreakdown.keys.all { it in setOf("1", "2", "3", "4", "5") })
    }
}
