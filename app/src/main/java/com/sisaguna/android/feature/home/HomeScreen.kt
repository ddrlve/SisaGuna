package com.sisaguna.android.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.R
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.MerchantStatus
import com.sisaguna.android.ui.components.SgSearchField
import com.sisaguna.android.ui.domain.ListingCard
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SisaGunaTheme
import java.time.Instant
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onListingClick: (Listing) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreenContent(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onRetry = viewModel::retry,
        onListingClick = onListingClick,
    )
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onSearchQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onListingClick: (Listing) -> Unit,
) {
    Scaffold(containerColor = SgColor.Neutral50) { padding ->
        when (uiState) {
            is HomeUiState.Loading -> LoadingState(modifier = Modifier.padding(padding))
            is HomeUiState.Error -> ErrorState(
                message = uiState.message,
                onRetry = onRetry,
                modifier = Modifier.padding(padding),
            )
            is HomeUiState.Success -> HomeFeedList(
                state = uiState,
                onSearchQueryChange = onSearchQueryChange,
                onListingClick = onListingClick,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = SgColor.Brand500)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = SgColor.Neutral500,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Coba lagi") }
        }
    }
}

@Composable
private fun HomeFeedList(
    state: HomeUiState.Success,
    onSearchQueryChange: (String) -> Unit,
    onListingClick: (Listing) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nearbyTitle = stringResource(R.string.home_rail_nearby)
    val nearbySubtitle = stringResource(R.string.home_rail_nearby_subtitle)
    val dealsTitle = stringResource(R.string.home_rail_deals)
    val dealsSubtitle = stringResource(R.string.home_rail_deals_subtitle)
    val animalFeedTitle = stringResource(R.string.home_rail_animal_feed)
    val animalFeedSubtitle = stringResource(R.string.home_rail_animal_feed_subtitle)
    val compostTitle = stringResource(R.string.home_rail_compost)
    val compostSubtitle = stringResource(R.string.home_rail_compost_subtitle)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // Location selector, wishlist and notifications belong to Alamat & Preferensi /
        // Profile, neither built yet — the bar is here for visual fidelity, the two icon
        // buttons are inert.
        item { HomeTopBar(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) }
        item {
            SgSearchField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = stringResource(R.string.home_search_placeholder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item { PromoBanner(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        item {
            Text(
                text = stringResource(R.string.home_category_title),
                style = MaterialTheme.typography.titleMedium,
                color = SgColor.Neutral800,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
            )
        }
        item { TierShortcutRow(modifier = Modifier.padding(horizontal = 16.dp)) }
        item { Spacer(modifier = Modifier.height(8.dp)) }

        if (state.isEmpty) {
            item { EmptySearchState(modifier = Modifier.padding(24.dp)) }
        } else {
            listingRail(
                title = nearbyTitle,
                subtitle = nearbySubtitle,
                trailing = { RadiusTag() },
                listings = state.nearby,
                now = state.now,
                onListingClick = onListingClick,
            )
            listingRail(
                title = dealsTitle,
                subtitle = dealsSubtitle,
                trailing = { SeeAllLink() },
                listings = state.deals,
                now = state.now,
                onListingClick = onListingClick,
            )
            listingRail(
                title = animalFeedTitle,
                subtitle = animalFeedSubtitle,
                trailing = { SeeAllLink() },
                listings = state.animalFeed,
                now = state.now,
                onListingClick = onListingClick,
            )
            listingRail(
                title = compostTitle,
                subtitle = compostSubtitle,
                trailing = { SeeAllLink() },
                listings = state.compost,
                now = state.now,
                onListingClick = onListingClick,
            )
        }
    }
}

@Composable
private fun HomeTopBar(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(SgColor.Brand200, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                // Opens the "Pilih Lokasi" sheet (figma/HomeCollection.jpeg) once Alamat &
                // Preferensi is built — inert for now.
                .clickable(onClick = {}),
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                tint = SgColor.Brand700,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.home_default_address_label),
                style = MaterialTheme.typography.labelLarge,
                color = SgColor.Brand800,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = {}) {
            Icon(Icons.Filled.FavoriteBorder, contentDescription = stringResource(R.string.home_wishlist_cd), tint = SgColor.Rose500)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.home_notifications_cd), tint = SgColor.Yellow500)
        }
    }
}

private fun LazyListScope.listingRail(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    listings: List<HomeListingUi>,
    now: Instant,
    onListingClick: (Listing) -> Unit,
) {
    if (listings.isEmpty()) return

    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = SgColor.Neutral800)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = SgColor.Neutral500)
            }
            trailing()
        }
    }
    item {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(listings, key = { it.listing.id }) { item ->
                ListingCard(
                    listing = item.listing,
                    merchant = item.merchant,
                    now = now,
                    onClick = { onListingClick(item.listing) },
                )
            }
        }
    }
}

@Composable
private fun RadiusTag() {
    Text(
        text = stringResource(R.string.home_radius_tag),
        style = MaterialTheme.typography.labelMedium,
        color = SgColor.Brand700,
    )
}

@Composable
private fun SeeAllLink() {
    // Would open Category List filtered accordingly — not built yet this session.
    Text(
        text = stringResource(R.string.home_see_all),
        style = MaterialTheme.typography.labelMedium,
        color = SgColor.Brand700,
        modifier = Modifier.clickable(onClick = {}),
    )
}

@Composable
private fun PromoBanner(modifier: Modifier = Modifier) {
    // [Guessing] Static copy placeholder — swap for the real promo banner content/image once
    // that's confirmed from Figma; this app has no CMS for banners yet.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(SgColor.Brand500, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_promo_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SgColor.BaseWhite,
            )
            Text(
                text = stringResource(R.string.home_promo_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = SgColor.Brand200,
            )
        }
    }
}

// Tapping a tier is meant to open Category List pre-filtered by that tier; that screen isn't
// built yet this session, so onTierClick is exposed but not wired from HomeScreen's caller.
@Composable
private fun TierShortcutRow(
    modifier: Modifier = Modifier,
    onTierClick: (ListingTier) -> Unit = {},
) {
    val shortcuts = listOf(
        Triple(ListingTier.HUMAN, R.string.tier_human, Icons.Filled.Restaurant),
        Triple(ListingTier.ANIMAL_FEED, R.string.tier_animal_feed, Icons.Filled.Pets),
        Triple(ListingTier.COMPOST, R.string.tier_compost, Icons.Filled.Spa),
    )
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        shortcuts.forEach { (tier, labelRes, icon) ->
            TierShortcut(
                label = stringResource(labelRes),
                icon = icon,
                tier = tier,
                onClick = { onTierClick(tier) },
            )
        }
    }
}

@Composable
private fun TierShortcut(
    label: String,
    icon: ImageVector,
    tier: ListingTier,
    onClick: () -> Unit,
) {
    // [Guessing] Background/icon colors approximate the green/peach/blue tiles in
    // figma/HomePage.jpeg; exact hex still needs Figma Inspect (see ui/theme/Color.kt).
    val (background, iconTint) = when (tier) {
        ListingTier.HUMAN -> SgColor.Brand200 to SgColor.Brand700
        ListingTier.ANIMAL_FEED -> SgColor.Orange100 to SgColor.Yellow500
        ListingTier.COMPOST -> SgColor.Sky100 to SgColor.Brand700
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(background, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = SgColor.Neutral800,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun EmptySearchState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = SgColor.Neutral800,
        )
        Text(
            text = stringResource(R.string.home_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = SgColor.Neutral500,
        )
    }
}

// ---- Preview: static layout with mock data, no ViewModel/Hilt involved ----

private fun mockUiState(): HomeUiState.Success {
    val now = Instant.now()
    val merchant = Merchant("m1", "fadlhan", isVerified = true, status = MerchantStatus.APPROVED, location = "Alam Sutera")

    fun listing(id: String, title: String, tier: ListingTier, original: Int?, discounted: Int?, free: Boolean, hours: Long, km: Double) =
        HomeListingUi(
            Listing(id, merchant.id, title, tier, original, discounted, free, now.plus(hours, ChronoUnit.HOURS), "", km),
            merchant = merchant,
        )

    return HomeUiState.Success(
        nearby = listOf(
            listing("p1", "Ayam olie", ListingTier.HUMAN, 34000, 11000, false, 2, 0.4),
            listing("p2", "Nasi Kuning Sisa Katering", ListingTier.HUMAN, 25000, 8000, false, 3, 1.2),
        ),
        deals = listOf(
            listing("p3", "Nasi Box Rapat Berlebih", ListingTier.HUMAN, 20000, null, true, 1, 0.5),
        ),
        animalFeed = listOf(
            listing("p4", "Sisa Sayur untuk Pakan Ternak", ListingTier.ANIMAL_FEED, 10000, 2000, false, 6, 5.4),
        ),
        compost = listOf(
            listing("p5", "Sisa Sayur untuk Kompos", ListingTier.COMPOST, null, null, true, 8, 3.1),
        ),
        now = now,
    )
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun HomeScreenPreview() {
    SisaGunaTheme {
        HomeScreenContent(
            uiState = mockUiState(),
            onSearchQueryChange = {},
            onRetry = {},
            onListingClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
    SisaGunaTheme {
        HomeScreenContent(uiState = HomeUiState.Loading, onSearchQueryChange = {}, onRetry = {}, onListingClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenErrorPreview() {
    SisaGunaTheme {
        HomeScreenContent(
            uiState = HomeUiState.Error("Gagal memuat data. Periksa koneksi internet dan coba lagi."),
            onSearchQueryChange = {},
            onRetry = {},
            onListingClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    SisaGunaTheme {
        HomeScreenContent(
            uiState = HomeUiState.Success(searchQuery = "xyz tidak ada"),
            onSearchQueryChange = {},
            onRetry = {},
            onListingClick = {},
        )
    }
}
