package com.sisaguna.android.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import com.sisaguna.android.data.repository.Voucher
import com.sisaguna.android.ui.domain.VoucherTicket
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.sisaguna.android.ui.components.SgEaseOut
import com.sisaguna.android.ui.components.SgEmptyState
import com.sisaguna.android.ui.components.SgSearchField
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.domain.ListingCard
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
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
    onNotificationsClick: () -> Unit = {},
    showNotificationBadge: Boolean = false,
    addressLabel: String = "Rumah",
    onAddressClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unread by viewModel.unreadNotifications.collectAsStateWithLifecycle()
    val vouchers by viewModel.vouchers.collectAsStateWithLifecycle()
    var showFilter by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun toast(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    Box(Modifier.fillMaxSize()) {
        HomeScreenContent(
            uiState = uiState,
            addressLabel = addressLabel,
            unreadNotifications = if (showNotificationBadge) unread else 0,
            vouchers = vouchers,
            onAddressClick = onAddressClick,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onFilterClick = { showFilter = true },
            onTabSelected = viewModel::onTabSelected,
            onRetry = viewModel::retry,
            onListingClick = onListingClick,
            onCategoryClick = onCategoryClick,
            onUploadClick = onUploadClick,
            onNotificationsClick = onNotificationsClick,
            onClaimVoucher = { code ->
                viewModel.claimVoucher(code)
                toast("Voucher diklaim. Pakai saat checkout ya!")
            },
            onBanner = { action ->
                when (action) {
                    BannerAction.BROWSE_FREE -> {
                        viewModel.onTabSelected(HomeTab.SIAP_SANTAP)
                        val current = (uiState as? HomeUiState.Success)?.filter ?: HomeFilter()
                        viewModel.onFilterChange(current.copy(freeOnly = true))
                        toast("Menampilkan makanan gratis")
                    }
                    BannerAction.CLAIM_FIRST_ORDER -> {
                        viewModel.claimVoucher("PERTAMA")
                        toast("Voucher PERTAMA diklaim: diskon 50% pesanan pertama")
                    }
                    BannerAction.OPEN_FARM_TAB -> viewModel.onTabSelected(HomeTab.TERNAK_KOMPOS)
                    BannerAction.UPLOAD -> onUploadClick()
                }
            },
        )
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showFilter) {
        HomeFilterSheet(
            current = (uiState as? HomeUiState.Success)?.filter ?: HomeFilter(),
            onApply = {
                viewModel.onFilterChange(it)
                showFilter = false
            },
            onDismiss = { showFilter = false },
        )
    }
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    addressLabel: String,
    unreadNotifications: Int,
    vouchers: List<Pair<Voucher, Boolean>>,
    onAddressClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onTabSelected: (HomeTab) -> Unit,
    onRetry: () -> Unit,
    onListingClick: (Listing) -> Unit,
    onCategoryClick: (ListingTier) -> Unit = {},
    onUploadClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onClaimVoucher: (String) -> Unit = {},
    onBanner: (BannerAction) -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize().background(SgColor.Page)) {
        when (uiState) {
            is HomeUiState.Loading -> LoadingState()
            is HomeUiState.Error -> ErrorState(message = uiState.message, onRetry = onRetry)
            is HomeUiState.Success -> HomeFeedList(
                state = uiState,
                addressLabel = addressLabel,
                unreadNotifications = unreadNotifications,
                vouchers = vouchers,
                onAddressClick = onAddressClick,
                onSearchQueryChange = onSearchQueryChange,
                onFilterClick = onFilterClick,
                onTabSelected = onTabSelected,
                onListingClick = onListingClick,
                onCategoryClick = onCategoryClick,
                onUploadClick = onUploadClick,
                onNotificationsClick = onNotificationsClick,
                onClaimVoucher = onClaimVoucher,
                onBanner = onBanner,
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
            Text(text = message, style = SgTextStyle.Body)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.common_retry)) }
        }
    }
}

/**
 * Spec §2 layout plus the round-2 polish: banner carousel above a sticky segmented switch.
 * Once the switch reaches the top it stays pinned on a frosted (translucent white + hairline)
 * strip so the user can change tabs without scrolling back up.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeFeedList(
    state: HomeUiState.Success,
    addressLabel: String,
    unreadNotifications: Int,
    vouchers: List<Pair<Voucher, Boolean>>,
    onAddressClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onTabSelected: (HomeTab) -> Unit,
    onListingClick: (Listing) -> Unit,
    onCategoryClick: (ListingTier) -> Unit,
    onUploadClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onClaimVoucher: (String) -> Unit,
    onBanner: (BannerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val pinned by remember { derivedStateOf { listState.firstVisibleItemIndex >= 3 } }
    val stripAlpha by animateFloatAsState(if (pinned) 1f else 0f, tween(180), label = "stickyGlass")

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = SgSpacing.Xl),
    ) {
        item {
            HomeTopBar(
                addressLabel = addressLabel,
                unreadNotifications = unreadNotifications,
                onAddressClick = onAddressClick,
                onUploadClick = onUploadClick,
                onNotificationsClick = onNotificationsClick,
                modifier = Modifier.padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Md),
            )
        }
        item {
            SgSearchField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = stringResource(R.string.home_search_placeholder),
                onFilterClick = onFilterClick,
                activeFilterCount = state.filter.activeCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Lg),
            )
        }
        item {
            BannerCarousel(onAction = onBanner, modifier = Modifier.padding(top = SgSpacing.Lg))
        }
        stickyHeader(key = "switch") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SgColor.Page.copy(alpha = 0.6f + 0.32f * stripAlpha))
                    .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Md, bottom = SgSpacing.Sm),
            ) {
                SegmentedSwitch(
                    options = listOf(stringResource(R.string.tier_human), stringResource(R.string.home_tab_farm)),
                    selectedIndex = state.selectedTab.ordinal,
                    onSelect = { onTabSelected(HomeTab.entries[it]) },
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).graphicsLayer { alpha = stripAlpha }.background(SgColor.Hairline))
        }
        if (state.filter.activeCount > 0) {
            item(key = "active-filter") {
                ActiveFilterBar(state.filter, onClick = onFilterClick)
            }
        }
        item(key = "tab-content") {
            AnimatedContent(
                targetState = state.selectedTab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (fadeIn(tween(200, easing = SgEaseOut)) + slideInHorizontally(tween(260, easing = SgEaseOut)) { if (forward) it / 8 else -it / 8 }) togetherWith
                        fadeOut(tween(120))
                },
                label = "homeTab",
            ) { tab ->
                Column {
                    when (tab) {
                        HomeTab.SIAP_SANTAP -> SiapSantapTab(state, vouchers, onListingClick, onCategoryClick, onClaimVoucher)
                        HomeTab.TERNAK_KOMPOS -> TernakKomposTab(state, onListingClick, onCategoryClick)
                    }
                    if (state.activeTabIsEmpty) {
                        if (state.otherTabMatchCount > 0) {
                            val otherTab = if (tab == HomeTab.SIAP_SANTAP) HomeTab.TERNAK_KOMPOS else HomeTab.SIAP_SANTAP
                            OtherTabHint(
                                count = state.otherTabMatchCount,
                                tabName = stringResource(if (otherTab == HomeTab.SIAP_SANTAP) R.string.tier_human else R.string.home_tab_farm),
                                onClick = { onTabSelected(otherTab) },
                            )
                        } else {
                            SgEmptyState(
                                icon = Icons.Rounded.SearchOff,
                                title = stringResource(R.string.home_empty_title),
                                body = stringResource(R.string.home_empty_subtitle),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFilterBar(filter: HomeFilter, onClick: () -> Unit) {
    val parts = buildList {
        if (filter.sort != HomeSort.RELEVANT) add(filter.sort.label)
        filter.maxDistanceKm?.let { add("< ${it.toInt()} km") }
        if (filter.freeOnly) add("Gratis")
    }
    Row(
        modifier = Modifier
            .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Sm)
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Pill))
            .background(SgColor.Mint)
            .pressable(onClick)
            .padding(horizontal = SgSpacing.Lg, vertical = SgSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Filter: " + parts.joinToString(" · "), style = SgTextStyle.TextXsMedium, color = SgColor.Brand700, modifier = Modifier.weight(1f))
        Text("Ubah", style = SgTextStyle.TextXsMedium, color = SgColor.Brand600)
    }
}

@Composable
private fun SiapSantapTab(
    state: HomeUiState.Success,
    vouchers: List<Pair<Voucher, Boolean>>,
    onListingClick: (Listing) -> Unit,
    onCategoryClick: (ListingTier) -> Unit,
    onClaimVoucher: (String) -> Unit,
) {
    Rail(
        title = stringResource(R.string.home_rail_nearby),
        subtitle = stringResource(R.string.home_rail_nearby_subtitle),
        trailing = { RadiusTag() },
        listings = state.nearby,
        now = state.now,
        onListingClick = onListingClick,
    )
    if (vouchers.isNotEmpty() && state.searchQuery.isBlank()) {
        Text(
            "Voucher untuk kamu",
            style = SgTextStyle.Title,
            modifier = Modifier.padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Xl),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Md),
            horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            items(vouchers, key = { it.first.code }) { (v, claimed) ->
                VoucherTicket(
                    title = v.title,
                    subtitle = v.description,
                    trailing = if (claimed) "Sudah diklaim ✓" else "Ketuk untuk klaim",
                    trailingColor = if (claimed) SgColor.InkMuted else SgColor.Brand700,
                    selected = claimed,
                    onClick = { if (!claimed) onClaimVoucher(v.code) },
                    modifier = Modifier.width(272.dp),
                )
            }
        }
    }
    Rail(
        title = stringResource(R.string.home_rail_deals),
        subtitle = stringResource(R.string.home_rail_deals_subtitle),
        trailing = { SeeAllLink(onClick = { onCategoryClick(ListingTier.HUMAN) }) },
        listings = state.deals,
        now = state.now,
        onListingClick = onListingClick,
    )
}

@Composable
private fun TernakKomposTab(
    state: HomeUiState.Success,
    onListingClick: (Listing) -> Unit,
    onCategoryClick: (ListingTier) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Lg),
        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
    ) {
        WideTile(
            image = R.drawable.category_animal,
            label = stringResource(R.string.tier_animal_feed),
            count = state.animalFeed.size,
            tint = SgColor.Farm,
            modifier = Modifier.weight(1f),
            onClick = { onCategoryClick(ListingTier.ANIMAL_FEED) },
        )
        WideTile(
            image = R.drawable.category_compost,
            label = stringResource(R.string.tier_compost),
            count = state.compost.size,
            tint = SgColor.Compost,
            modifier = Modifier.weight(1f),
            onClick = { onCategoryClick(ListingTier.COMPOST) },
        )
    }
    Rail(
        title = stringResource(R.string.home_rail_animal_feed),
        subtitle = stringResource(R.string.home_rail_animal_feed_subtitle),
        trailing = { SeeAllLink(onClick = { onCategoryClick(ListingTier.ANIMAL_FEED) }) },
        listings = state.animalFeed,
        now = state.now,
        onListingClick = onListingClick,
    )
    Rail(
        title = stringResource(R.string.home_rail_compost),
        subtitle = stringResource(R.string.home_rail_compost_subtitle),
        trailing = { SeeAllLink(onClick = { onCategoryClick(ListingTier.COMPOST) }) },
        listings = state.compost,
        now = state.now,
        onListingClick = onListingClick,
    )
}

/** Figma node 40:6216 top row: "Rumah" location chip (opens LocationPickerSheet) + "Upload" CTA
 * (85:3039) + notification bell (104:6511) with an unread badge. */
@Composable
private fun HomeTopBar(
    addressLabel: String,
    unreadNotifications: Int,
    onAddressClick: () -> Unit,
    onUploadClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(SgRadius.Pill))
                .background(SgColor.BaseWhite)
                .pressable(onAddressClick)
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
                style = SgTextStyle.Label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp).widthIn(max = 132.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(SgRadius.Pill))
                    .background(SgColor.Brand500)
                    .pressable(onUploadClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_upload),
                    contentDescription = null,
                    tint = SgColor.BaseWhite,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.home_upload_cta),
                    style = SgTextStyle.Label,
                    color = SgColor.BaseWhite,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            NotificationBell(unread = unreadNotifications, onClick = onNotificationsClick)
        }
    }
}

@Composable
private fun NotificationBell(unread: Int, onClick: () -> Unit) {
    val cd = if (unread > 0) {
        stringResource(R.string.home_notifications_unread_cd, unread)
    } else {
        stringResource(R.string.home_notifications_cd)
    }
    Box(modifier = Modifier.size(40.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(SgColor.BaseWhite)
                .pressable(onClick, pressedScale = 0.92f)
                .semantics { contentDescription = cd },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_notification_bell),
                contentDescription = null,
                tint = SgColor.Ink,
                modifier = Modifier.size(22.dp),
            )
        }
        AnimatedVisibility(
            visible = unread > 0,
            enter = scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = 0.6f)) + fadeIn(),
            exit = scaleOut(targetScale = 0.6f, animationSpec = tween(120)) + fadeOut(tween(120)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-2).dp),
        ) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                    .border(2.dp, SgColor.Page, CircleShape)
                    .padding(2.dp)
                    .background(SgColor.RedStatus, CircleShape)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (unread > 9) "9+" else unread.toString(),
                    color = SgColor.BaseWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

/**
 * Pill track in [SgColor.Hairline] with a white thumb that slides under the selected label.
 * The thumb moves on a spring so a quick double-tap retargets mid-flight instead of
 * restarting.
 */
@Composable
private fun SegmentedSwitch(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(SgRadius.Pill))
            .background(SgColor.Hairline)
            .padding(4.dp),
    ) {
        val segmentWidth = maxWidth / options.size
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
            label = "segmentThumb",
        )
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(2.dp, RoundedCornerShape(SgRadius.Pill))
                .background(SgColor.BaseWhite, RoundedCornerShape(SgRadius.Pill)),
        )
        Row(modifier = Modifier.fillMaxSize().selectableGroup()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(SgRadius.Pill))
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = SgTextStyle.Label,
                        color = if (selected) SgColor.Ink else SgColor.InkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
}

/** Ternak & Kompos entry tile (spec §2): tinted icon square left, label + count right. */
@Composable
private fun WideTile(
    image: Int,
    label: String,
    count: Int,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Tile))
            .pressable(onClick)
            .padding(SgSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(tint, RoundedCornerShape(SgRadius.Thumb)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(image),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(32.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = SgTextStyle.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = stringResource(R.string.home_available_count, count), style = SgTextStyle.Body, maxLines = 1)
        }
    }
}

@Composable
private fun OtherTabHint(count: Int, tabName: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Xl)
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(SgColor.Mint)
            .padding(start = SgSpacing.Lg, end = SgSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_other_tab_hint, count, tabName),
            style = SgTextStyle.Label,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClick) {
            Text(text = stringResource(R.string.home_other_tab_action), style = SgTextStyle.Label, color = SgColor.Brand600)
        }
    }
}

/** Heading on the gutter; cards scroll edge to edge but rest on the gutter (spec §1.2). */
@Composable
private fun Rail(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    listings: List<HomeListingUi>,
    now: Instant,
    onListingClick: (Listing) -> Unit,
) {
    if (listings.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = SgTextStyle.Title)
            Text(text = subtitle, style = SgTextStyle.Caption)
        }
        trailing()
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        contentPadding = PaddingValues(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Md),
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

@Composable
private fun RadiusTag() {
    Text(
        text = stringResource(R.string.home_radius_tag),
        style = SgTextStyle.TextXsMedium,
        color = SgColor.Brand700,
        modifier = Modifier
            .background(SgColor.Green100, RoundedCornerShape(SgRadius.Pill))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** "Lihat semua" pill: 36dp tall with an arrow so it reads as a button, not a footnote. */
@Composable
private fun SeeAllLink(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(SgRadius.Pill))
            .background(SgColor.Mint)
            .pressable(onClick)
            .padding(start = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.home_see_all), style = SgTextStyle.Label, color = SgColor.Brand700)
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = SgColor.Brand700,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ---- Preview: static layout with mock data, no ViewModel/Hilt involved ----

private fun mockUiState(tab: HomeTab = HomeTab.SIAP_SANTAP): HomeUiState.Success {
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
        selectedTab = tab,
        now = now,
    )
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun HomeScreenPreview() {
    SisaGunaTheme {
        HomeScreenContent(
            uiState = mockUiState(),
            addressLabel = "Rumah",
            unreadNotifications = 3,
            vouchers = emptyList(),
            onAddressClick = {},
            onFilterClick = {},
            onSearchQueryChange = {},
            onTabSelected = {},
            onRetry = {},
            onListingClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun HomeScreenFarmTabPreview() {
    SisaGunaTheme {
        HomeScreenContent(
            uiState = mockUiState(HomeTab.TERNAK_KOMPOS),
            addressLabel = "Rumah",
            unreadNotifications = 0,
            vouchers = emptyList(),
            onAddressClick = {},
            onFilterClick = {},
            onSearchQueryChange = {},
            onTabSelected = {},
            onRetry = {},
            onListingClick = {},
        )
    }
}
