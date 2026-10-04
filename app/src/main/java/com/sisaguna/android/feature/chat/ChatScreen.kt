package com.sisaguna.android.feature.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sisaguna.android.data.model.ChatMessage
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.ui.components.SgEaseOut
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.domain.ListingImage
import com.sisaguna.android.ui.domain.formatClock
import com.sisaguna.android.ui.domain.formatPrice
import com.sisaguna.android.ui.domain.relativeDay
import com.sisaguna.android.ui.i18n.Text
import com.sisaguna.android.ui.i18n.l
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import java.time.Instant
import java.time.ZoneId

/**
 * Buyer-to-store chat. Built from interview feedback: before paying, buyers want to ask
 * whether the food is still there, when it was cooked and whether it's halal. Those questions
 * sit as one-tap chips above the keyboard; the item being asked about is pinned at the top.
 */
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onOpenStore: (String) -> Unit,
    onOpenListing: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val merchant = state.merchant
    val listState = rememberLazyListState()
    val openedAt = remember { Instant.now() }

    // Keep the newest message (or the typing bubble) in view.
    val rows = state.messages.size + if (state.storeTyping) 1 else 0
    LaunchedEffect(rows) {
        if (rows > 0) listState.animateScrollToItem(rows - 1)
    }

    Column(Modifier.fillMaxSize().background(SgColor.Page).imePadding()) {
        ChatTopBar(merchant, typing = state.storeTyping, onBack = onBack, onOpenStore = { merchant?.let { onOpenStore(it.id) } })
        state.listing?.let { ListingContext(it, onClick = { onOpenListing(it.id) }) }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (state.messages.isEmpty() && !state.storeTyping) {
                EmptyChat(merchant?.name.orEmpty(), Modifier.align(Alignment.Center))
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(state.messages, key = { _, m -> m.id }) { index, message ->
                    val prev = state.messages.getOrNull(index - 1)
                    if (prev == null || !sameDay(prev.sentAt, message.sentAt)) DayLabel(message.sentAt)
                    Bubble(message, isNew = message.sentAt.isAfter(openedAt), groupedWithPrevious = prev?.fromMe == message.fromMe)
                }
                if (state.storeTyping) item(key = "typing") { TypingBubble() }
            }
        }

        Composer(
            draft = state.draft,
            canSend = state.canSend,
            onDraftChange = viewModel::onDraftChange,
            onSend = { viewModel.send() },
            onQuick = { viewModel.send(it) },
        )
    }
}

@Composable
private fun ChatTopBar(merchant: Merchant?, typing: Boolean, onBack: () -> Unit, onOpenStore: () -> Unit) {
    Column(Modifier.background(SgColor.BaseWhite)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = l("Kembali", "Back"), tint = SgColor.Ink)
            }
            AsyncImage(
                merchant?.photos?.firstOrNull() ?: merchant?.bannerUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SgColor.Mint),
            )
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(merchant?.name.orEmpty(), style = SgTextStyle.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Status line swaps to "typing" so the buyer knows an answer is coming.
                Text(
                    if (typing) l("sedang mengetik…", "typing…") else l("Biasanya membalas dalam 5 menit", "Usually replies within 5 min"),
                    style = SgTextStyle.Caption,
                    color = if (typing) SgColor.Brand600 else SgColor.InkMuted,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onOpenStore) {
                Icon(Icons.Rounded.Storefront, contentDescription = l("Lihat toko", "Visit store"), tint = SgColor.Brand600)
            }
        }
        HorizontalDivider(color = SgColor.Hairline)
    }
}

@Composable
private fun ListingContext(listing: Listing, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(SgColor.BaseWhite)
            .pressable(onClick, pressedScale = 0.99f)
            .padding(horizontal = SgSpacing.Gutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ListingImage(listing.imageUrl, listing.tier, null, Modifier.size(44.dp).clip(RoundedCornerShape(SgRadius.Thumb)))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(l("Kamu bertanya tentang", "You're asking about"), style = SgTextStyle.Caption)
            Text(listing.title, style = SgTextStyle.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatPrice(listing.unitPrice), style = SgTextStyle.Label, color = SgColor.Brand700)
    }
    HorizontalDivider(color = SgColor.Hairline)
}

@Composable
private fun EmptyChat(storeName: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).background(SgColor.Mint, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = SgColor.Brand600, modifier = Modifier.size(26.dp))
        }
        Text(
            l("Tanya langsung ke $storeName", "Ask $storeName directly"),
            style = SgTextStyle.Title,
            modifier = Modifier.padding(top = SgSpacing.Md),
        )
        Text(
            l("Cek stok, jam masak, atau halal sebelum pesan. Pilih pertanyaan cepat di bawah.", "Check stock, cooking time or halal status before ordering. Tap a quick question below."),
            style = SgTextStyle.Body,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun DayLabel(at: Instant) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(
            relativeDay(at),
            style = SgTextStyle.Caption.copy(fontSize = 11.sp),
            modifier = Modifier.background(SgColor.Hairline, RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

/**
 * Mine on the right in brand green, the store's on the left in white. Consecutive messages
 * from the same side tuck together. A bubble that arrives while the chat is open rises 10dp
 * and fades in; history loads still.
 */
@Composable
private fun Bubble(message: ChatMessage, isNew: Boolean, groupedWithPrevious: Boolean) {
    val enter = remember { Animatable(if (isNew) 0f else 1f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(220, easing = SgEaseOut)) }
    val mine = message.fromMe
    val shape = if (mine) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = if (groupedWithPrevious) 0.dp else 6.dp)
            .graphicsLayer {
                alpha = enter.value
                translationY = (1f - enter.value) * 10.dp.toPx()
            },
        contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            Modifier
                .widthIn(max = 290.dp)
                .clip(shape)
                .background(if (mine) SgColor.Brand500 else SgColor.BaseWhite)
                .then(if (mine) Modifier else Modifier.border(1.dp, SgColor.Hairline, shape))
                .padding(start = 14.dp, end = 14.dp, top = 9.dp, bottom = 7.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                message.text,
                style = SgTextStyle.Body.copy(fontSize = 15.sp, lineHeight = 21.sp),
                color = if (mine) SgColor.OnBrand else SgColor.Ink,
            )
            Text(
                formatClock(message.sentAt),
                style = SgTextStyle.Caption.copy(fontSize = 10.sp),
                color = if (mine) SgColor.OnBrand.copy(alpha = 0.75f) else SgColor.InkMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Three dots pulsing in sequence: the only looping motion here, shown only while it's true. */
@Composable
private fun TypingBubble() {
    val pulse = rememberInfiniteTransition(label = "typing")
    Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.CenterStart) {
        Row(
            Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(SgColor.BaseWhite)
                .border(1.dp, SgColor.Hairline, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(3) { i ->
                val a by pulse.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(450, delayMillis = i * 150, easing = SgEaseOut), RepeatMode.Reverse),
                    label = "dot$i",
                )
                Box(Modifier.size(7.dp).graphicsLayer { alpha = a }.background(SgColor.InkMuted, CircleShape))
            }
        }
    }
}

@Composable
private fun Composer(
    draft: String,
    canSend: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuick: (String) -> Unit,
) {
    Column(Modifier.background(SgColor.BaseWhite).navigationBarsPadding()) {
        HorizontalDivider(color = SgColor.Hairline)
        LazyRow(
            contentPadding = PaddingValues(horizontal = SgSpacing.Gutter, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(QuickQuestion.entries) { q ->
                Text(
                    q.text,
                    style = SgTextStyle.Label.copy(fontSize = 13.sp),
                    color = SgColor.Brand700,
                    modifier = Modifier
                        .clip(RoundedCornerShape(SgRadius.Pill))
                        .background(SgColor.Mint)
                        .pressable({ onQuick(q.text) }, pressedScale = 0.95f)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = SgSpacing.Gutter, end = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(SgColor.Page)
                    .border(1.dp, SgColor.Hairline, RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (draft.isEmpty()) Text(l("Tulis pesan…", "Write a message…"), style = SgTextStyle.Body, color = SgColor.InkMuted)
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    textStyle = SgTextStyle.Body.copy(fontSize = 15.sp, color = SgColor.Ink),
                    cursorBrush = SolidColor(SgColor.Brand500),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // Send stays visible but muted until there's text, so the target never jumps.
            val bg by animateColorAsState(if (canSend) SgColor.Brand500 else SgColor.Hairline, tween(150), label = "sendBg")
            val scale by animateFloatAsState(if (canSend) 1f else 0.9f, tween(150, easing = SgEaseOut), label = "sendScale")
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .size(44.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(CircleShape)
                    .background(bg)
                    .pressable(onSend, enabled = canSend, pressedScale = 0.9f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Send,
                    contentDescription = l("Kirim", "Send"),
                    tint = if (canSend) SgColor.OnBrand else SgColor.InkMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun sameDay(a: Instant, b: Instant, zone: ZoneId = ZoneId.systemDefault()): Boolean =
    a.atZone(zone).toLocalDate() == b.atZone(zone).toLocalDate()

/** Short time for inbox rows: clock today, otherwise the relative day. */
internal fun chatTime(at: Instant, now: Instant = Instant.now()): String =
    if (sameDay(at, now)) formatClock(at) else relativeDay(at, now)
