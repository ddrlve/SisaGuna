package com.sisaguna.android.feature.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.R
import com.sisaguna.android.feature.address.LocationPickerSheet
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.MerchantStatus
import com.sisaguna.android.ui.components.SgSearchField
import com.sisaguna.android.ui.domain.ListingCard
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.theme.SisaGunaTheme
import java.time.Instant
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onListingClick: (Listing) -> Unit = {},
    onCategoryClick: (ListingTier) -> Unit = {},
    onUploadClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var addressLabel by remember { mutableStateOf("Rumah") }
    var showLocationSheet by remember { mutableStateOf(false) }

    HomeScreenContent(
        uiState = uiState,
        addressLabel = addressLabel,
        onAddressClick = { showLocationSheet = true },
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onRetry = viewModel::retry,
        onListingClick = onListingClick,
        onCategoryClick = onCategoryClick,
        onUploadClick = onUploadClick,
    )

    if (showLocationSheet) {
        LocationPickerSheet(
            onDismiss = { showLocationSheet = false },
            onLocationResolved = { label ->
                addressLabel = label
                showLocationSheet = false
            },
        )
    }
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    addressLabel: String,
    onAddressClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onListingClick: (Listing) -> Unit,
    onCategoryClick: (ListingTier) -> Unit = {},
    onUploadClick: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize().background(SgColor.Neutral100)) {
        when (uiState) {
            is HomeUiState.Loading -> LoadingState()
            is HomeUiState.Error -> ErrorState(message = uiState.message, onRetry = onRetry)
            is HomeUiState.Success -> HomeFeedList(
                state = uiState,
                addressLabel = addressLabel,
                onAddressClick = onAddressClick,
                onSearchQueryChange = onSearchQueryChange,
                onListingClick = onListingClick,
                onCategoryClick = onCategoryClick,
                onUploadClick = onUploadClick,
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
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = SgColor.Neutral500)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Coba lagi") }
        }
    }
}

@Composable
private fun HomeFeedList(
    state: HomeUiState.Success,
    addressLabel: String,
    onAddressClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onListingClick: (Listing) -> Unit,
    onCategoryClick: (ListingTier) -> Unit,
    onUploadClick: () -> Unit,
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
        item {
            HomeTopBar(
                addressLabel = addressLabel,
                onAddressClick = onAddressClick,
                onUploadClick = onUploadClick,
                modifier = Modifier.padding(horizontal = 23.dp, vertical = 16.dp),
            )
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 23.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                SgSearchField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = stringResource(R.string.home_search_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                )
                PromoBanner()
            }
        }
        item { SiapSantapHeaderAndTile(onTierClick = onCategoryClick) }

        if (state.isEmpty) {
            item {
                PakanTernakSection(
                    animalFeedTitle = animalFeedTitle,
                    animalFeedSubtitle = animalFeedSubtitle,
                    animalFeed = emptyList(),
                    compostTitle = compostTitle,
                    compostSubtitle = compostSubtitle,
                    compost = emptyList(),
                    now = state.now,
                    onListingClick = onListingClick,
                    onTierClick = onCategoryClick,
                )
            }
            item { EmptySearchState(modifier = Modifier.padding(24.dp)) }
        } else {
            listingRail(nearbyTitle, nearbySubtitle, { RadiusTag() }, state.nearby, state.now, onListingClick)
            listingRail(dealsTitle, dealsSubtitle, { SeeAllLink(onClick = { onCategoryClick(ListingTier.HUMAN) }) }, state.deals, state.now, onListingClick)
            item {
                PakanTernakSection(
                    animalFeedTitle = animalFeedTitle,
                    animalFeedSubtitle = animalFeedSubtitle,
                    animalFeed = state.animalFeed,
                    compostTitle = compostTitle,
                    compostSubtitle = compostSubtitle,
                    compost = state.compost,
                    now = state.now,
                    onListingClick = onListingClick,
                    onTierClick = onCategoryClick,
                )
            }
        }
    }
}

/** Figma node 40:6216 top row: "Rumah" location chip (opens LocationPickerSheet) + "Upload" CTA
 * (85:3039) + notification bell (104:6511). */
@Composable
private fun HomeTopBar(
    addressLabel: String,
    onAddressClick: () -> Unit,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(SgColor.BaseWhite, RoundedCornerShape(30.dp))
                .clickableNoRipple(onAddressClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(SgColor.Green100, CircleShape)
                    .padding(4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_location_chip),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = addressLabel,
                style = SgTextStyle.TextSmMedium,
                color = SgColor.Neutral800,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(SgColor.Brand500, RoundedCornerShape(30.dp))
                    .clickableNoRipple(onUploadClick)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_upload),
                    contentDescription = null,
                    tint = SgColor.BaseWhite,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.home_upload_cta),
                    style = SgTextStyle.TextSmMedium,
                    color = SgColor.BaseWhite,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(SgColor.BaseWhite, CircleShape)
                    // Opens the notification list — not built yet this session.
                    .clickableNoRipple(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_notification_bell),
                    contentDescription = stringResource(R.string.home_notifications_cd),
                    tint = SgColor.Neutral800,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/** Figma node 43:6293 promo banner. Decorative stars/ellipses simplified to a flat brand
 * background — the food illustration and copy are the real Figma asset/text. */
@Composable
private fun PromoBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SgColor.Brand100),
    ) {
        Image(
            painter = painterResource(R.drawable.banner_food),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(190.dp)
                .padding(end = 4.dp),
        )
        Column(modifier = Modifier.padding(start = 16.dp, top = 20.dp)) {
            Text(
                text = stringResource(R.string.home_promo_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = SgColor.Brand700,
            )
            Text(
                text = stringResource(R.string.home_promo_subtitle),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = SgColor.Neutral50,
            )
            Text(
                text = stringResource(R.string.home_promo_free),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                color = SgColor.Neutral50,
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(4.dp).background(SgColor.BaseWhite, CircleShape))
            Box(Modifier.width(12.dp).height(4.dp).background(SgColor.BaseWhite, RoundedCornerShape(50)))
            Box(Modifier.size(4.dp).background(SgColor.BaseWhite, CircleShape))
            Box(Modifier.size(4.dp).background(SgColor.BaseWhite, CircleShape))
        }
    }
}

/** One tier tile (Figma node 259:10226-259:10237 pattern, reused per-section after the
 * section split). Tapping opens Category List filtered by tier. */
@Composable
private fun CategoryTile(
    tier: ListingTier,
    labelRes: Int,
    image: Int,
    bg: Color,
    onClick: (ListingTier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier
            .background(SgColor.BaseWhite, RoundedCornerShape(20.dp))
            .clickableNoRipple(onClick = { onClick(tier) })
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(bg, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(image),
                contentDescription = stringResource(labelRes),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(48.dp),
            )
        }
        Text(
            text = stringResource(labelRes),
            style = SgTextStyle.TextSmRegular,
            color = SgColor.LabelsPrimary,
        )
    }
}

/** Section 1 — human food. No tint (matches the page background), single tile. */
@Composable
private fun SiapSantapHeaderAndTile(onTierClick: (ListingTier) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 23.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.home_section_human_title),
                style = SgTextStyle.TextLgSemibold,
                color = SgColor.Neutral800,
            )
            Text(
                text = stringResource(R.string.home_section_human_subtitle),
                fontSize = 12.sp,
                color = SgColor.Neutral400,
            )
        }
        CategoryTile(
            tier = ListingTier.HUMAN,
            labelRes = R.string.tier_human,
            image = R.drawable.category_human,
            bg = SgColor.Green100,
            onClick = onTierClick,
        )
    }
}

/** Section 2 — animal feed + compost. Tinted Neutral50 container (no Figma source for this
 * grouping — see spec) holding its own two tiles and both its rails, so the tint stays
 * continuous behind all of it as a single LazyColumn item. */
@Composable
private fun PakanTernakSection(
    animalFeedTitle: String,
    animalFeedSubtitle: String,
    animalFeed: List<HomeListingUi>,
    compostTitle: String,
    compostSubtitle: String,
    compost: List<HomeListingUi>,
    now: Instant,
    onListingClick: (Listing) -> Unit,
    onTierClick: (ListingTier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 23.dp)
            .background(SgColor.Neutral50, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.home_section_farm_title),
                style = SgTextStyle.TextLgSemibold,
                color = SgColor.Neutral800,
            )
            Text(
                text = stringResource(R.string.home_section_farm_subtitle),
                fontSize = 12.sp,
                color = SgColor.Neutral400,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryTile(ListingTier.ANIMAL_FEED, R.string.tier_animal_feed, R.drawable.category_animal, SgColor.Orange100, onTierClick)
            CategoryTile(ListingTier.COMPOST, R.string.tier_compost, R.drawable.category_compost, SgColor.Sky100, onTierClick)
        }
        RailBlock(animalFeedTitle, animalFeedSubtitle, { SeeAllLink(onClick = { onTierClick(ListingTier.ANIMAL_FEED) }) }, animalFeed, now, onListingClick)
        RailBlock(compostTitle, compostSubtitle, { SeeAllLink(onClick = { onTierClick(ListingTier.COMPOST) }) }, compost, now, onListingClick)
    }
}

/** Non-lazy counterpart of [listingRail] — used inside [PakanTernakSection] so both its rails
 * render as part of one LazyColumn item (keeping the tinted background continuous), rather
 * than as separate lazy items the way Section 1's rails still do. Renders nothing when empty,
 * same as [listingRail]. */
@Composable
private fun RailBlock(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    listings: List<HomeListingUi>,
    now: Instant,
    onListingClick: (Listing) -> Unit,
) {
    if (listings.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = SgTextStyle.TextLgSemibold, color = SgColor.Neutral800)
                Text(text = subtitle, fontSize = 12.sp, color = SgColor.Neutral400)
            }
            trailing()
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(listings, key = { it.listing.id }) { entry ->
                ListingCard(
                    listing = entry.listing,
                    merchant = entry.merchant,
                    now = now,
                    onClick = { onListingClick(entry.listing) },
                )
            }
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
                .padding(start = 23.dp, end = 23.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = SgTextStyle.TextLgSemibold, color = SgColor.Neutral800)
                Text(text = subtitle, fontSize = 12.sp, color = SgColor.Neutral400)
            }
            trailing()
        }
    }
    item {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 23.dp, vertical = 8.dp),
        ) {
            items(listings, key = { it.listing.id }) { entry ->
                ListingCard(
                    listing = entry.listing,
                    merchant = entry.merchant,
                    now = now,
                    onClick = { onListingClick(entry.listing) },
                )
            }
        }
    }
}

@Composable
private fun RadiusTag() {
    Text(
        text = stringResource(R.string.home_radius_tag),
        style = SgTextStyle.TextXsMedium,
        color = SgColor.Brand700,
        modifier = Modifier
            .background(SgColor.Green100, RoundedCornerShape(30.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun SeeAllLink(onClick: () -> Unit) {
    Text(
        text = stringResource(R.string.home_see_all),
        style = SgTextStyle.TextXsMedium,
        color = SgColor.Brand600,
        modifier = Modifier.clickableNoRipple(onClick = onClick),
    )
}

@Composable
private fun EmptySearchState(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = stringResource(R.string.home_empty_title), style = SgTextStyle.TextLgSemibold, color = SgColor.Neutral800)
        Text(text = stringResource(R.string.home_empty_subtitle), style = SgTextStyle.TextSmRegular, color = SgColor.Neutral500)
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit = {}): Modifier =
    this.clickable(onClick = onClick)

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
            listing("p2", "Ayam olie", ListingTier.HUMAN, 34000, 11000, false, 3, 0.4),
        ),
        deals = listOf(listing("p3", "Ayam olie", ListingTier.HUMAN, 34000, null, true, 1, 0.4)),
        animalFeed = listOf(listing("p4", "Ayam olie", ListingTier.ANIMAL_FEED, 34000, null, true, 6, 0.4)),
        compost = listOf(listing("p5", "Ayam olie", ListingTier.COMPOST, 34000, null, true, 8, 0.4)),
        now = now,
    )
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun HomeScreenPreview() {
    SisaGunaTheme {
        HomeScreenContent(uiState = mockUiState(), addressLabel = "Rumah", onAddressClick = {}, onSearchQueryChange = {}, onRetry = {}, onListingClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
    SisaGunaTheme {
        HomeScreenContent(uiState = HomeUiState.Loading, addressLabel = "Rumah", onAddressClick = {}, onSearchQueryChange = {}, onRetry = {}, onListingClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenErrorPreview() {
    SisaGunaTheme {
        HomeScreenContent(
            uiState = HomeUiState.Error("Gagal memuat data. Periksa koneksi internet dan coba lagi."),
            addressLabel = "Rumah",
            onAddressClick = {},
            onSearchQueryChange = {},
            onRetry = {},
            onListingClick = {},
        )
    }
}
