package com.sisaguna.android.feature.saved

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.R
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.components.SgEmptyState
import com.sisaguna.android.ui.domain.formatClock
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.domain.InitialAvatar
import com.sisaguna.android.ui.domain.MerchantSummary
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import kotlinx.coroutines.launch

/** Figma 273:12037. */
@Composable
fun SavedScreen(
    onMerchantClick: (String) -> Unit,
    onExploreClick: () -> Unit,
    viewModel: SavedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val removedMessage = stringResource(R.string.saved_removed)
    val undoLabel = stringResource(R.string.common_undo)

    Scaffold(
        containerColor = SgColor.Page,
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        when (val s = state) {
            SavedUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SgColor.Brand500)
            }
            is SavedUiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = SgSpacing.Xl),
                verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.saved_title),
                        style = SgTextStyle.Display,
                        modifier = Modifier.padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Lg),
                    )
                }
                if (s.merchants.isEmpty()) {
                    item {
                        SgEmptyState(
                            icon = Icons.Rounded.BookmarkBorder,
                            title = stringResource(R.string.saved_empty_title),
                            body = stringResource(R.string.saved_empty_body),
                            action = {
                                Button(
                                    onClick = onExploreClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = SgColor.Brand500),
                                ) { Text(stringResource(R.string.saved_empty_action), style = SgTextStyle.Label, color = SgColor.BaseWhite) }
                            },
                        )
                    }
                } else {
                    item {
                        SavedSummary(merchantCount = s.merchants.size, available = s.availableTotal)
                    }
                    item {
                        Text(
                            text = stringResource(R.string.saved_section_merchants),
                            style = SgTextStyle.Title,
                            modifier = Modifier.padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Sm),
                        )
                    }
                    items(s.merchants, key = { it.merchant.id }) { entry ->
                        SavedMerchantCard(
                            entry = entry,
                            onClick = { onMerchantClick(entry.merchant.id) },
                            onUnsave = {
                                val id = entry.merchant.id
                                viewModel.unsave(id)
                                scope.launch {
                                    snackbar.currentSnackbarData?.dismiss()
                                    val result = snackbar.showSnackbar(removedMessage, undoLabel, duration = SnackbarDuration.Short)
                                    if (result == SnackbarResult.ActionPerformed) viewModel.undo(id)
                                }
                            },
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = SgSpacing.Gutter),
                        )
                    }
                }
                if (s.suggestions.isNotEmpty()) {
                    item(key = "suggest-title") {
                        Column(Modifier.animateItem().padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Lg)) {
                            Text("Rekomendasi penyedia", style = SgTextStyle.Title)
                            Text("Sedang punya makanan di sekitarmu. Simpan biar dapat kabar menu baru.", style = SgTextStyle.Caption)
                        }
                    }
                    items(s.suggestions, key = { "s-" + it.merchant.id }) { entry ->
                        SuggestionRow(
                            entry = entry,
                            onClick = { onMerchantClick(entry.merchant.id) },
                            onSave = { viewModel.save(entry.merchant.id) },
                            modifier = Modifier.animateItem().padding(horizontal = SgSpacing.Gutter),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedSummary(merchantCount: Int, available: Int) {
    Row(
        modifier = Modifier
            .padding(horizontal = SgSpacing.Gutter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(Brush.linearGradient(listOf(SgColor.Brand500, SgColor.Brand700)))
            .padding(SgSpacing.Lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("$available makanan tersedia", style = SgTextStyle.Title, color = SgColor.BaseWhite)
            Text("dari $merchantCount penyedia favoritmu hari ini", style = SgTextStyle.Caption, color = SgColor.BaseWhite.copy(alpha = 0.85f))
        }
        Box(Modifier.size(44.dp).background(SgColor.BaseWhite.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Favorite, contentDescription = null, tint = SgColor.BaseWhite)
        }
    }
}

@Composable
private fun SavedMerchantCard(
    entry: SavedMerchantUi,
    onClick: () -> Unit,
    onUnsave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val merchant = entry.merchant
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Card))
            .pressable(onClick),
    ) {
        Row(
            modifier = Modifier.padding(start = SgSpacing.Lg, top = SgSpacing.Md, end = SgSpacing.Xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            InitialAvatar(initial = merchant.name.first().uppercaseChar(), size = 48.dp, verified = merchant.isVerified)
            MerchantSummary(
                merchant = merchant,
                distanceKm = entry.distanceKm,
                verifiedLabel = stringResource(R.string.saved_verified),
                modifier = Modifier.weight(1f),
            )
            HeartButton(saved = true, merchantName = merchant.name, onClick = onUnsave)
        }
        Row(
            modifier = Modifier.padding(start = SgSpacing.Lg, end = SgSpacing.Lg, top = SgSpacing.Sm, bottom = SgSpacing.Md),
            horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
        ) {
            if (entry.availableCount > 0) {
                InfoChip("${entry.availableCount} makanan tersedia", SgColor.Mint, SgColor.Brand700)
                entry.nextPickupEnd?.let { InfoChip("Ambil s/d ${formatClock(it)}", SgColor.Farm, SgColor.FarmInk) }
            } else {
                InfoChip("Belum ada makanan hari ini", SgColor.Page, SgColor.InkMuted)
            }
        }
    }
}

@Composable
private fun InfoChip(text: String, bg: Color, fg: Color) {
    Text(
        text,
        style = SgTextStyle.TextXsMedium,
        color = fg,
        modifier = Modifier.background(bg, RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun SuggestionRow(entry: SavedMerchantUi, onClick: () -> Unit, onSave: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Tile))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Tile))
            .pressable(onClick)
            .padding(SgSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
    ) {
        InitialAvatar(initial = entry.merchant.name.first().uppercaseChar(), size = 40.dp, verified = entry.merchant.isVerified)
        Column(Modifier.weight(1f)) {
            Text(entry.merchant.name, style = SgTextStyle.Label)
            Text("${entry.availableCount} tersedia · ${entry.merchant.location}", style = SgTextStyle.Caption)
        }
        SgButton("Simpan", onClick = onSave, style = SgButtonStyle.Secondary, height = 36.dp)
    }
}

/**
 * 48dp heart toggle. On change it dips to 0.8 and springs back with a little overshoot — the
 * pop confirms the tap even before the Snackbar appears.
 */
@Composable
fun HeartButton(saved: Boolean, merchantName: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var taps by remember { mutableIntStateOf(0) }
    var squeezed by remember { androidx.compose.runtime.mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (squeezed) 0.8f else 1f,
        animationSpec = if (squeezed) tween(90) else spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        finishedListener = { squeezed = false },
        label = "heart",
    )
    LaunchedEffect(taps) { if (taps > 0) squeezed = true }

    val cd = if (saved) {
        stringResource(R.string.saved_unsave_cd, merchantName)
    } else {
        stringResource(R.string.saved_save_cd, merchantName)
    }
    IconButton(
        onClick = {
            taps++
            onClick()
        },
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            imageVector = if (saved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = cd,
            tint = if (saved) SgColor.RedStatus else SgColor.InkMuted,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}

@Composable
internal fun MerchantHeaderBand(
    merchant: Merchant,
    distanceKm: Double?,
    listingCount: Int,
    saved: Boolean,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SgColor.BaseWhite)
            .padding(start = SgSpacing.Gutter, end = SgSpacing.Xs, top = SgSpacing.Lg, bottom = SgSpacing.Lg)
            .animateContentSize(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
            InitialAvatar(initial = merchant.name.first().uppercaseChar(), size = 56.dp, verified = merchant.isVerified)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                MerchantSummary(merchant = merchant, distanceKm = distanceKm, verifiedLabel = stringResource(R.string.saved_verified))
                Text(text = stringResource(R.string.merchant_listing_count, listingCount), style = SgTextStyle.Caption, color = SgColor.Brand600)
            }
            HeartButton(saved = saved, merchantName = merchant.name, onClick = onToggleSave)
        }
    }
}
