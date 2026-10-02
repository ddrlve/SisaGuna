package com.sisaguna.android.feature.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.R
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.ui.components.SgChip
import com.sisaguna.android.ui.components.SgEmptyState
import com.sisaguna.android.ui.components.SgSearchField
import com.sisaguna.android.ui.components.SgTopBar
import com.sisaguna.android.ui.domain.CartBar
import com.sisaguna.android.ui.domain.CategoryListingCard
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import kotlinx.coroutines.launch

/** Figma 273:12143 — search + tier chips + merchant band + 2-column grid. */
@Composable
fun MerchantDetailScreen(
    onBack: () -> Unit,
    onListingClick: (String) -> Unit,
    onCartClick: () -> Unit,
    /** Fixed title ("Disimpan" when opened from Saved); null shows the merchant's name. */
    title: String? = null,
    viewModel: MerchantDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val removedMessage = stringResource(R.string.saved_removed)
    val restoredMessage = stringResource(R.string.saved_restored)
    val undoLabel = stringResource(R.string.common_undo)

    Scaffold(
        containerColor = SgColor.Page,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            SgTopBar(
                title = title ?: (state as? MerchantDetailUiState.Success)?.merchant?.name.orEmpty(),
                onBack = onBack,
            )
        },
        bottomBar = {
            val s = state as? MerchantDetailUiState.Success
            CartBar(
                visible = s != null && s.cartCount > 0,
                itemCount = s?.cartCount ?: 0,
                total = s?.cartTotal ?: 0,
                merchantName = null,
                onClick = onCartClick,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        when (val s = state) {
            MerchantDetailUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SgColor.Brand500)
            }
            MerchantDetailUiState.NotFound -> SgEmptyState(
                icon = Icons.Rounded.Storefront,
                title = stringResource(R.string.merchant_not_found),
                body = stringResource(R.string.merchant_not_found_body),
                modifier = Modifier.padding(padding),
                action = {
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = SgColor.Brand500)) {
                        Text(stringResource(R.string.common_back_cd), style = SgTextStyle.Label, color = SgColor.BaseWhite)
                    }
                },
            )
            is MerchantDetailUiState.Success -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = SgSpacing.Gutter, end = SgSpacing.Gutter, bottom = SgSpacing.Xl),
                horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                        SgSearchField(
                            value = s.query,
                            onValueChange = viewModel::onQueryChange,
                            placeholder = stringResource(R.string.merchant_search_placeholder),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TierChips(selected = s.tierFilter, onToggle = viewModel::onTierToggle)
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    // Full-bleed band: undo the grid's gutter padding so the white reaches the
                    // screen edges while its content stays on the gutter (spec §1.2).
                    MerchantHeaderBand(
                        merchant = s.merchant,
                        distanceKm = s.distanceKm,
                        listingCount = s.totalListings,
                        saved = s.isSaved,
                        onToggleSave = {
                            val nowSaved = viewModel.toggleSave()
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                if (nowSaved) {
                                    snackbar.showSnackbar(restoredMessage, duration = SnackbarDuration.Short)
                                } else {
                                    val result = snackbar.showSnackbar(removedMessage, undoLabel, duration = SnackbarDuration.Short)
                                    if (result == SnackbarResult.ActionPerformed) viewModel.toggleSave()
                                }
                            }
                        },
                        modifier = Modifier.layoutFullBleed(SgSpacing.Gutter),
                    )
                }
                if (s.listings.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SgEmptyState(
                            icon = Icons.Rounded.SearchOff,
                            title = stringResource(R.string.merchant_no_listings),
                            body = stringResource(R.string.merchant_no_listings_body),
                        )
                    }
                } else {
                    items(s.listings, key = { it.id }) { listing ->
                        CategoryListingCard(
                            listing = listing,
                            merchant = s.merchant,
                            now = s.now,
                            onClick = { onListingClick(listing.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TierChips(selected: ListingTier?, onToggle: (ListingTier) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
    ) {
        listOf(
            ListingTier.HUMAN to R.string.tier_human,
            ListingTier.ANIMAL_FEED to R.string.tier_animal_feed_chip,
            ListingTier.COMPOST to R.string.tier_compost_chip,
        ).forEach { (tier, label) ->
            SgChip(label = stringResource(label), selected = selected == tier, onClick = { onToggle(tier) })
        }
    }
}

/** Widens a child by [gutter] on both sides of its parent's padding. */
private fun Modifier.layoutFullBleed(gutter: Dp): Modifier =
    this.layout { measurable, constraints ->
        val extra = gutter.roundToPx() * 2
        val placeable = measurable.measure(
            constraints.copy(
                minWidth = constraints.maxWidth + extra,
                maxWidth = constraints.maxWidth + extra,
            ),
        )
        layout(constraints.maxWidth, placeable.height) {
            placeable.place(-gutter.roundToPx(), 0)
        }
    }
