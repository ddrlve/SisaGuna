package com.sisaguna.android.feature.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.R
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.MerchantStatus
import com.sisaguna.android.ui.components.SgSearchField
import com.sisaguna.android.ui.domain.CategoryListingCard
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.theme.SisaGunaTheme
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Matches Figma node 45:9805 "Home" (Category List) — a header (back, address, upload,
 * search, tier/sort filter chips) pinned above a 2-column grid of listings for one tier. */
@Composable
fun CategoryListScreen(
    viewModel: CategoryListViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onListingClick: (Listing) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CategoryListScreenContent(
        uiState = uiState,
        onBack = onBack,
        onTierChange = viewModel::onTierChange,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onToggleNearest = viewModel::onToggleNearest,
        onToggleFree = viewModel::onToggleFree,
        onRetry = { viewModel.onTierChange((uiState as? CategoryListUiState.Success)?.tier ?: ListingTier.HUMAN) },
        onListingClick = onListingClick,
    )
}

@Composable
private fun CategoryListScreenContent(
    uiState: CategoryListUiState,
    onBack: () -> Unit,
    onTierChange: (ListingTier) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleNearest: () -> Unit,
    onToggleFree: () -> Unit,
    onRetry: () -> Unit,
    onListingClick: (Listing) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(SgColor.Neutral100)) {
        val headerTier = (uiState as? CategoryListUiState.Success)?.tier ?: ListingTier.HUMAN
        val searchQuery = (uiState as? CategoryListUiState.Success)?.searchQuery ?: ""
        val nearestFirst = (uiState as? CategoryListUiState.Success)?.nearestFirst ?: false
        val freeOnly = (uiState as? CategoryListUiState.Success)?.freeOnly ?: false

        CategoryHeader(
            tier = headerTier,
            searchQuery = searchQuery,
            nearestFirst = nearestFirst,
            freeOnly = freeOnly,
            onBack = onBack,
            onTierChange = onTierChange,
            onSearchQueryChange = onSearchQueryChange,
            onToggleNearest = onToggleNearest,
            onToggleFree = onToggleFree,
        )

        when (uiState) {
            is CategoryListUiState.Loading -> LoadingState()
            is CategoryListUiState.Error -> ErrorState(message = uiState.message, onRetry = onRetry)
            is CategoryListUiState.Success -> CategoryGrid(state = uiState, onListingClick = onListingClick)
        }
    }
}

@Composable
private fun CategoryHeader(
    tier: ListingTier,
    searchQuery: String,
    nearestFirst: Boolean,
    freeOnly: Boolean,
    onBack: () -> Unit,
    onTierChange: (ListingTier) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleNearest: () -> Unit,
    onToggleFree: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SgColor.BaseWhite)
            .border(BorderStroke(1.dp, SgColor.Neutral100))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(SgColor.BaseWhite, CircleShape)
                        .border(BorderStroke(1.dp, SgColor.Neutral100), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.category_back_cd),
                        tint = SgColor.Neutral800,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(SgColor.BaseWhite, RoundedCornerShape(30.dp))
                        .border(BorderStroke(1.dp, SgColor.Neutral100), RoundedCornerShape(30.dp))
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
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.home_default_address_label),
                        style = SgTextStyle.TextSmMedium,
                        color = SgColor.Neutral800,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(SgColor.Brand500, RoundedCornerShape(30.dp))
                    // Opens the create-listing flow — not built yet this session.
                    .clickable(onClick = {})
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
        }

        SgSearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = stringResource(R.string.home_search_placeholder),
            containerColor = SgColor.Neutral100,
            modifier = Modifier.fillMaxWidth(),
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item { CategoryFilterChip(label = "Siap Santap", selected = tier == ListingTier.HUMAN, onClick = { onTierChange(ListingTier.HUMAN) }) }
            item { CategoryFilterChip(label = "Untuk Ternak", selected = tier == ListingTier.ANIMAL_FEED, onClick = { onTierChange(ListingTier.ANIMAL_FEED) }) }
            item { CategoryFilterChip(label = "Untuk kompos", selected = tier == ListingTier.COMPOST, onClick = { onTierChange(ListingTier.COMPOST) }) }
            item { CategoryFilterChip(label = "Terdekat", selected = nearestFirst, onClick = onToggleNearest) }
            item { CategoryFilterChip(label = "Gratis", selected = freeOnly, onClick = onToggleFree) }
        }
    }
}

/** Figma node 45:10173 "address type" — same visual language as Activity's filter chip. */
@Composable
private fun CategoryFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) SgColor.Brand500 else SgColor.Neutral500,
        modifier = Modifier
            .background(if (selected) SgColor.Green50 else SgColor.BaseWhite, RoundedCornerShape(30.dp))
            .border(BorderStroke(1.dp, if (selected) SgColor.Brand500 else SgColor.Neutral200), RoundedCornerShape(30.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 4.dp),
    )
}

@Composable
private fun CategoryGrid(state: CategoryListUiState.Success, onListingClick: (Listing) -> Unit, modifier: Modifier = Modifier) {
    if (state.isEmpty) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.category_empty_title),
                style = SgTextStyle.TextSmRegular,
                color = SgColor.Neutral500,
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.listings, key = { it.listing.id }) { entry ->
            CategoryListingCard(
                listing = entry.listing,
                merchant = entry.merchant,
                now = state.now,
                onClick = { onListingClick(entry.listing) },
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
            Text(text = message, style = SgTextStyle.TextSmRegular, color = SgColor.Neutral500)
            Button(onClick = onRetry) { Text("Coba lagi") }
        }
    }
}

// ---- Preview: static layout with mock data, no ViewModel/Hilt involved ----

private fun mockCategoryState(): CategoryListUiState.Success {
    val now = Instant.now()
    val merchant = Merchant("m1", "fadlhan", isVerified = true, status = MerchantStatus.APPROVED, location = "Alam Sutera")

    fun listing(id: String, original: Int?, discounted: Int?, free: Boolean, hours: Long) =
        CategoryListingUi(
            Listing(id, merchant.id, "Ayam olie", ListingTier.HUMAN, original, discounted, free, now.plus(hours, ChronoUnit.HOURS), "", 0.4),
            merchant = merchant,
        )

    return CategoryListUiState.Success(
        tier = ListingTier.HUMAN,
        listings = listOf(
            listing("c1", 34000, 11000, false, 2),
            listing("c2", 34000, null, true, 3),
            listing("c3", 34000, 11000, false, 4),
            listing("c4", 34000, null, true, 5),
        ),
        now = now,
    )
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun CategoryListScreenPreview() {
    SisaGunaTheme {
        CategoryListScreenContent(
            uiState = mockCategoryState(),
            onBack = {},
            onTierChange = {},
            onSearchQueryChange = {},
            onToggleNearest = {},
            onToggleFree = {},
            onRetry = {},
            onListingClick = {},
        )
    }
}
