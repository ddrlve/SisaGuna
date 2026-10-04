package com.sisaguna.android.feature.notifications

import androidx.compose.material3.HorizontalDivider

import androidx.compose.foundation.border

import androidx.compose.foundation.layout.offset

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import com.sisaguna.android.ui.i18n.SgSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.R
import com.sisaguna.android.data.model.AppNotification
import com.sisaguna.android.data.model.NotificationType
import com.sisaguna.android.ui.components.SgChip
import com.sisaguna.android.ui.components.SgEmptyState
import com.sisaguna.android.ui.components.SgTopBar
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import java.time.ZoneId
import kotlinx.coroutines.launch

private data class TypeStyle(val icon: ImageVector, val tint: Color, val ink: Color)

private fun styleFor(type: NotificationType) = when (type) {
    NotificationType.PICKUP -> TypeStyle(Icons.Rounded.Schedule, SgColor.Farm, SgColor.FarmInk)
    NotificationType.ORDER -> TypeStyle(Icons.Rounded.ShoppingBag, SgColor.Mint, SgColor.Brand700)
    NotificationType.MERCHANT -> TypeStyle(Icons.Rounded.Storefront, SgColor.Compost, SgColor.CompostInk)
    NotificationType.PROMO -> TypeStyle(Icons.Rounded.LocalOffer, SgColor.PromoTint, SgColor.PromoInk)
    NotificationType.IMPACT -> TypeStyle(Icons.Rounded.Eco, SgColor.Mint, SgColor.Brand600)
}

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.notif_deleted)
    val allReadMessage = stringResource(R.string.notif_all_read)
    val undoLabel = stringResource(R.string.common_undo)
    val zone = remember { ZoneId.systemDefault() }

    Scaffold(
        containerColor = SgColor.Page,
        snackbarHost = { SgSnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
        topBar = {
            SgTopBar(title = stringResource(R.string.notif_title), onBack = onBack) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(visible = state.unreadCount > 0, enter = fadeIn(), exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.9f)) {
                        TextButton(onClick = {
                            viewModel.markAllRead()
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(allReadMessage)
                            }
                        }) {
                            Text(stringResource(R.string.notif_mark_all), style = SgTextStyle.Label, color = SgColor.Brand600)
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.notif_settings_cd), tint = SgColor.Ink)
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = SgSpacing.Xl),
        ) {
            item(key = "filters") {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Sm),
                    horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
                ) {
                    listOf(
                        NotificationFilter.ALL to R.string.notif_filter_all,
                        NotificationFilter.ORDERS to R.string.notif_filter_orders,
                        NotificationFilter.MERCHANTS to R.string.notif_filter_merchants,
                        NotificationFilter.PROMOS to R.string.notif_filter_promos,
                    ).forEach { (filter, label) ->
                        SgChip(label = stringResource(label), selected = state.filter == filter, onClick = { viewModel.onFilter(filter) })
                    }
                }
            }
            if (state.isEmpty) {
                item(key = "empty") {
                    SgEmptyState(
                        icon = Icons.Rounded.NotificationsNone,
                        title = stringResource(R.string.notif_empty_title),
                        body = stringResource(R.string.notif_empty_body),
                    )
                }
            }
            state.groups.forEach { group ->
                item(key = "header-${group.group}") {
                    Text(
                        text = stringResource(
                            when (group.group) {
                                DayGroup.TODAY -> R.string.notif_group_today
                                DayGroup.YESTERDAY -> R.string.notif_group_yesterday
                                DayGroup.THIS_WEEK -> R.string.notif_group_week
                                DayGroup.EARLIER -> R.string.notif_group_earlier
                            },
                        ),
                        style = SgTextStyle.Caption,
                        modifier = Modifier
                            .animateItem()
                            .padding(start = SgSpacing.Gutter, end = SgSpacing.Gutter, top = SgSpacing.Lg, bottom = SgSpacing.Xs),
                    )
                }
                items(group.items, key = { it.id }) { notification ->
                    SwipeableNotificationRow(
                        notification = notification,
                        time = relativeTime(notification.createdAt, state.now, zone),
                        onOpen = { viewModel.onOpen(notification.id) },
                        onDelete = {
                            val deleted = viewModel.delete(notification.id) ?: return@SwipeableNotificationRow
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                val result = snackbar.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Short)
                                if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(deleted)
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableNotificationRow(
    notification: AppNotification,
    time: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
            value == SwipeToDismissBoxValue.EndToStart
        },
        positionalThreshold = { distance -> distance * 0.4f },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SgColor.RedStatus)
                    .padding(end = SgSpacing.Xl),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = SgColor.OnBrand)
            }
        },
    ) {
        NotificationRow(notification, time, onOpen)
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, time: String, onOpen: () -> Unit) {
    val style = styleFor(notification.type)
    val background by animateColorAsState(
        targetValue = if (notification.isRead) SgColor.BaseWhite else SgColor.Mint,
        animationSpec = tween(200),
        label = "notifBg",
    )
    val unreadCd = stringResource(R.string.notif_unread_cd)
    // The unread dot rides on the icon instead of taking a trailing column, so reading a
    // notification no longer leaves an empty 20dp gap on the right: both edges stay at the
    // gutter whether it's read or not.
    Column(Modifier.background(background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressable(onOpen, pressedScale = 0.99f)
                .semantics { if (!notification.isRead) stateDescription = unreadCd }
                .padding(horizontal = SgSpacing.Gutter, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(Modifier.size(40.dp)) {
                Box(
                    modifier = Modifier.fillMaxSize().background(style.tint, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(style.icon, contentDescription = null, tint = style.ink, modifier = Modifier.size(20.dp))
                }
                val dot by animateFloatAsState(if (notification.isRead) 0f else 1f, tween(150), label = "unreadDot")
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(12.dp)
                        .graphicsLayer { alpha = dot; scaleX = 0.6f + 0.4f * dot; scaleY = 0.6f + 0.4f * dot }
                        .border(2.dp, background, CircleShape)
                        .padding(2.dp)
                        .background(SgColor.Brand500, CircleShape),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = SgSpacing.Md), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        notification.title,
                        style = SgTextStyle.Label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = SgSpacing.Sm),
                    )
                    Text(time, style = SgTextStyle.Caption, color = if (notification.isRead) SgColor.InkMuted else SgColor.Brand600)
                }
                Text(notification.body, style = SgTextStyle.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        HorizontalDivider(color = SgColor.Hairline, modifier = Modifier.padding(start = SgSpacing.Gutter + 40.dp + SgSpacing.Md))
    }
}
