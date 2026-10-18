package com.sisaguna.android.feature.chat

import com.sisaguna.android.ui.components.CountBadge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.ui.components.SgEmptyState
import com.sisaguna.android.ui.components.SgTopBar
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.i18n.Text
import com.sisaguna.android.ui.i18n.l
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import androidx.compose.runtime.getValue

/** Every conversation with stores, newest first, unread ones in bold with a count. */
@Composable
fun ChatInboxScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: ChatInboxViewModel = hiltViewModel(),
) {
    val threads by viewModel.threads.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(SgColor.Page)) {
        Box(Modifier.background(SgColor.BaseWhite)) {
            SgTopBar(title = l("Chat toko", "Store chats"), onBack = onBack)
        }
        if (threads.isEmpty()) {
            SgEmptyState(
                icon = Icons.Rounded.ChatBubbleOutline,
                title = l("Belum ada chat", "No chats yet"),
                body = l("Buka halaman makanan atau toko, lalu ketuk Chat untuk bertanya ke penjual.", "Open a food or store page and tap Chat to ask the seller."),
            )
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(threads, key = { it.id }) { t ->
                val unread = t.unread > 0
                Row(
                    Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .background(SgColor.BaseWhite)
                        .pressable({ onOpenChat(t.merchantId) }, pressedScale = 0.99f)
                        .padding(horizontal = SgSpacing.Gutter, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StoreAvatar(t.name, t.merchant?.photos?.firstOrNull() ?: t.merchant?.bannerUrl, size = 48.dp, radius = 14.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(t.name, style = SgTextStyle.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            (if (t.lastMine) l("Kamu: ", "You: ") else "") + t.lastText,
                            style = if (unread) SgTextStyle.Body.copy(fontWeight = FontWeight.SemiBold) else SgTextStyle.Body,
                            color = if (unread) SgColor.Ink else SgColor.InkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(t.lastAt?.let { chatTime(it) }.orEmpty(), style = SgTextStyle.Caption, color = if (unread) SgColor.Brand600 else SgColor.InkMuted)
                        if (unread) CountBadge(t.unread, size = 20.dp)
                    }
                }
                HorizontalDivider(color = SgColor.Hairline, modifier = Modifier.padding(start = 76.dp))
            }
        }
    }
}
