package com.sisaguna.android.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.ui.graphics.Brush
import coil.compose.AsyncImage
import com.sisaguna.android.data.repository.seedImage
import com.sisaguna.android.data.settings.UserMode
import com.sisaguna.android.ui.domain.CartBar
import com.sisaguna.android.ui.domain.VoucherCard
import com.sisaguna.android.ui.domain.VoucherDetailSheet
import com.sisaguna.android.ui.i18n.l
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import com.sisaguna.android.ui.i18n.SgSnackbarHost
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
import com.sisaguna.android.ui.i18n.Text
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
    onSellerChatsClick: () -> Unit = {},
    sellerUnreadChats: Int = 0,
    addressLabel: String = "Rumah",
    onAddressClick: () -> Unit = {},
    onCartClick: () -> Unit = {},
    onMerchantClick: (String) -> Unit = {},
    onUploadTierClick: (ListingTier) -> Unit = { onUploadClick() },
    onMyCatalogClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unread by viewModel.unreadNotifications.collectAsStateWithLifecycle()
    val vouchers by viewModel.vouchers.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val cart by viewModel.cartSummary.collectAsStateWithLifecycle()
    var showFilter by remember { mutableStateOf(false) }
    var openVoucher by remember { mutableStateOf<Voucher?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun toast(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Buyer <-> merchant homes crossfade (short: it's a mode change the user asked for, not
        // navigation). Size snaps so the list never animates its height.
        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn(tween(220, easing = SgEaseOut)) togetherWith fadeOut(tween(120)) using SizeTransform(clip = false) { _, _ -> snap() } },
            label = "homeMode",
        ) { m ->
        if (m == UserMode.MERCHANT) {
            MerchantHome(
                onModeChange = viewModel::setMode,
                onUploadTierClick = onUploadTierClick,
                onNotificationsClick = onNotificationsClick,
                unreadNotifications = if (showNotificationBadge) unread else 0,
                onMyCatalogClick = onMyCatalogClick,
                onListingClick = onListingClick,
                onChatsClick = onSellerChatsClick,
                unreadChats = sellerUnreadChats,
            )
        } else HomeScreenContent(
            mode = m,
            onModeChange = viewModel::setMode,
            onMerchantClick = onMerchantClick,
            onVoucherClick = { openVoucher = it },
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
        }
        // Cart stays reachable from Home (user testing: it vanished after leaving a store).
        CartBar(
            visible = cart != null && mode == UserMode.BUYER,
            itemCount = cart?.itemCount ?: 0,
            total = cart?.total ?: 0,
            merchantName = cart?.merchantName,
            onClick = onCartClick,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        SgSnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (cart != null) 72.dp else 0.dp))
    }

    openVoucher?.let { v ->
        VoucherDetailSheet(
            voucher = v,
            claimed = vouchers.firstOrNull { it.first.code == v.code }?.second == true,
            onClaim = {
                viewModel.claimVoucher(v.code)
                toast(l("Voucher diklaim. Pakai saat checkout ya!", "Voucher claimed. Use it at checkout!"))
            },
            onDismiss = { openVoucher = null },
        )
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
    mode: UserMode = UserMode.BUYER,
    onModeChange: (UserMode) -> Unit = {},
    onMerchantClick: (String) -> Unit = {},
    onVoucherClick: (Voucher) -> Unit = {},
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
                mode = mode,
                onModeChange = onModeChange,
                onMerchantClick = onMerchantClick,
                onVoucherClick = onVoucherClick,
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
    mode: UserMode,
    onModeChange: (UserMode) -> Unit,
    onMerchantClick: (String) -> Unit,
    onVoucherClick: (Voucher) -> Unit,
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
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            HomeTopBar(
                mode = mode,
                onModeChange = onModeChange,
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
            // Was janky: the default SizeTransform animated the item's height inside the
            // LazyColumn while both tabs overlapped, so the page "breathed". Now the height
            // snaps, the old tab leaves fast (90ms) and the new one slides in from the side the
            // thumb moved toward, with the strong ease-out — reads as one sideways move.
            AnimatedContent(
                targetState = state.selectedTab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (fadeIn(tween(220, delayMillis = 40, easing = SgEaseOut)) + slideInHorizontally(tween(280, easing = SgEaseOut)) { if (forward) 72 else -72 }) togetherWith
                        (fadeOut(tween(90)) + slideOutHorizontally(tween(160, easing = SgEaseOut)) { if (forward) -48 else 48 }) using
                        SizeTransform(clip = false) { _, _ -> snap() }
                },
                label = "homeTab",
            ) { tab ->
                Column {
                    when (tab) {
                        HomeTab.SIAP_SANTAP -> SiapSantapTab(state, vouchers, onListingClick, onCategoryClick, onVoucherClick, onMerchantClick)
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
    onVoucherClick: (Voucher) -> Unit,
    onMerchantClick: (String) -> Unit,
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
                VoucherCard(voucher = v, claimed = claimed, onClick = { onVoucherClick(v) })
            }
        }
    }
    if (state.offerMerchants.isNotEmpty() && state.searchQuery.isBlank()) {
        Text(
            l("Penawaran hari ini", "Today's offers"),
            style = SgTextStyle.Title,
            modifier = Modifier.padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Lg),
        )
        Text(
            l("Promo langsung dari toko mitra", "Deals straight from partner stores"),
            style = SgTextStyle.Caption,
            modifier = Modifier.padding(horizontal = SgSpacing.Gutter),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Md),
            horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            items(state.offerMerchants, key = { it.id }) { m -> OfferStoreCard(m, onClick = { onMerchantClick(m.id) }) }
        }
    }
    Rail(
        title = l("Paling laris", "Best sellers"),
        subtitle = l("Yang paling sering diselamatkan hari ini", "Most rescued today"),
        trailing = { SeeAllLink(onClick = { onCategoryClick(ListingTier.HUMAN) }) },
        listings = state.popular,
        now = state.now,
        onListingClick = onListingClick,
    )
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
            photo = seedImage("sayur_pakan"),
            label = stringResource(R.string.tier_animal_feed),
            count = state.animalFeed.size,
            tint = SgColor.Farm,
            modifier = Modifier.weight(1f),
            onClick = { onCategoryClick(ListingTier.ANIMAL_FEED) },
        )
        WideTile(
            photo = seedImage("kompos_sayur"),
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
    mode: UserMode,
    onModeChange: (UserMode) -> Unit,
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
            ModeSwitch(mode = mode, onChange = onModeChange)
            NotificationBell(unread = unreadNotifications, onClick = onNotificationsClick)
        }
    }
}

@Composable
internal fun NotificationBell(unread: Int, onClick: () -> Unit) {
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
            com.sisaguna.android.ui.components.CountBadge(unread, color = SgColor.RedStatus, ring = SgColor.Page)
        }
    }
}

/**
 * Siap Santap / Ternak & Kompos switch. User testing called the old one "kurang smooth dan
 * jelek": a grey track with a white thumb moved by `offset` (a relayout every frame) and labels
 * that only changed colour at the end.
 *
 * Now: a green thumb rides under the selected option on `graphicsLayer.translationX` (no
 * relayout), driven by a critically damped spring (Apple: bounce only after a momentum gesture) so a quick double tap retargets mid-way
 * instead of restarting; label + icon colours cross-fade over the same 200ms so the text
 * "hands over" as the thumb passes. Each option carries an icon so the two worlds are told
 * apart at a glance.
 */
@Composable
private fun SegmentedSwitch(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val icons = listOf(Icons.Rounded.RestaurantMenu, Icons.Rounded.Agriculture)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(SgRadius.Pill))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Pill))
            .padding(4.dp),
    ) {
        val segmentWidth = maxWidth / options.size
        val density = androidx.compose.ui.platform.LocalDensity.current
        val thumbX by animateFloatAsState(
            targetValue = with(density) { (segmentWidth * selectedIndex).toPx() },
            animationSpec = spring(dampingRatio = 1f, stiffness = 420f), // critically damped: no overshoot on a tap
            label = "segmentThumb",
        )
        Box(
            modifier = Modifier
                .width(segmentWidth)
                .fillMaxHeight()
                .graphicsLayer { translationX = thumbX }
                .shadow(6.dp, RoundedCornerShape(SgRadius.Pill), ambientColor = SgColor.Brand500, spotColor = SgColor.Brand500)
                .background(SgColor.Brand500, RoundedCornerShape(SgRadius.Pill)),
        )
        Row(modifier = Modifier.fillMaxSize().selectableGroup()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val fg by animateColorAsState(if (selected) SgColor.OnBrand else SgColor.InkMuted, tween(200, easing = SgEaseOut), label = "segFg")
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(SgRadius.Pill))
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icons[index % icons.size], contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                    Text(
                        text = label,
                        style = SgTextStyle.Label.copy(fontWeight = FontWeight.Bold),
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

/** Pembeli | Mitra pill in the Home top bar — the two homes user testing asked us to separate. */
@Composable
fun ModeSwitch(mode: UserMode, onChange: (UserMode) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(SgRadius.Pill))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Pill))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(UserMode.BUYER to l("Pembeli", "Buyer"), UserMode.MERCHANT to l("Mitra", "Seller")).forEach { (m, label) ->
            val selected = m == mode
            val bg by animateColorAsState(if (selected) SgColor.Ink else Color.Transparent, tween(180, easing = SgEaseOut), label = "modeBg")
            val fg by animateColorAsState(if (selected) SgColor.BaseWhite else SgColor.InkMuted, tween(180, easing = SgEaseOut), label = "modeFg")
            Text(
                label,
                style = SgTextStyle.TextXsMedium.copy(fontWeight = FontWeight.Bold),
                color = fg,
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(SgRadius.Pill))
                    .background(bg)
                    .pressable({ onChange(m) }, pressedScale = 0.95f)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(horizontal = 12.dp),
            )
        }
    }
}

/** Store card for "Penawaran hari ini": banner photo, store name, rating, and the offer. */
@Composable
private fun OfferStoreCard(merchant: Merchant, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SgColor.BaseWhite)
            .pressable(onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(96.dp)) {
            AsyncImage(merchant.bannerUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))))
            Text(
                merchant.name,
                style = SgTextStyle.Label,
                color = SgColor.OnBrand,
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 8.dp),
            )
            merchant.rating?.let {
                Text(
                    "★ %.1f".format(it),
                    style = SgTextStyle.TextXsMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1F2A1C),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.White, RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocalOffer, contentDescription = null, tint = SgColor.PromoInk, modifier = Modifier.size(16.dp))
            Text(
                merchant.todaysOffer.orEmpty(),
                style = SgTextStyle.Caption.copy(color = SgColor.Ink),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** Ternak & Kompos entry tile (spec §2): tinted icon square left, label + count right. */
@Composable
private fun WideTile(
    photo: String,
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
                .size(48.dp)
                .clip(RoundedCornerShape(SgRadius.Thumb))
                .background(tint),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(model = photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = SgTextStyle.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = stringResource(R.string.home_available_count, count) + " · " + l("per kg", "by kg"), style = SgTextStyle.Caption, maxLines = 1)
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
