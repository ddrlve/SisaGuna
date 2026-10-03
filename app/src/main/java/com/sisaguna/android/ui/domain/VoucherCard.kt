package com.sisaguna.android.ui.domain

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sisaguna.android.data.repository.Voucher
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.i18n.Text
import com.sisaguna.android.ui.i18n.l
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Home voucher card: a photo of what the voucher is *for* on the left (so "Diskon 20%" reads
 * as "20% off bread & dessert" at a glance), ticket notches, and the claim state. Tapping opens
 * [VoucherDetailSheet] with the full conditions instead of claiming blindly.
 */
@Composable
fun VoucherCard(
    voucher: Voucher,
    claimed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = TicketShape(notchX = 96.dp, notchRadius = 9.dp, corner = 18.dp)
    val accent = Color(voucher.accent)
    val border by animateColorAsState(if (claimed) SgColor.Brand500 else SgColor.Hairline, tween(150), label = "voucherBorder")
    Row(
        modifier = modifier
            .width(300.dp)
            .height(104.dp)
            .clip(shape)
            .background(SgColor.BaseWhite)
            .border(1.dp, border, shape)
            .pressable(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(96.dp).fillMaxHeight()) {
            AsyncImage(
                model = voucher.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.85f)))))
            androidx.compose.material3.Text(
                voucher.headlineValue(),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                fontFamily = SgTextStyle.Label.fontFamily,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 8.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f).padding(start = 14.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(voucher.title, style = SgTextStyle.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(voucher.appliesTo, style = SgTextStyle.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (claimed) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SgColor.Brand500, modifier = Modifier.size(14.dp))
                    Text(l(" Diklaim", " Claimed"), style = SgTextStyle.TextXsMedium, color = SgColor.Brand600)
                } else {
                    Text(
                        l("Klaim", "Claim"),
                        style = SgTextStyle.TextXsMedium.copy(fontWeight = FontWeight.Bold),
                        color = SgColor.OnBrand,
                        modifier = Modifier.background(SgColor.Brand500, RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(l("S&K", "T&C") + " ›", style = SgTextStyle.TextXsMedium, color = SgColor.InkMuted)
            }
        }
    }
}

private fun Voucher.headlineValue(): String = when {
    percentOff != null -> "$percentOff%"
    flatOff != null -> "${flatOff / 1000}rb"
    else -> "Promo"
}

private val expiryFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH.mm", Locale("id", "ID"))

/** Full voucher conditions: what it's for, minimum spend, cap, validity, and the T&C list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherDetailSheet(
    voucher: Voucher,
    claimed: Boolean,
    onClaim: () -> Unit,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SgColor.BaseWhite,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Sm),
        ) {
            Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp))) {
                AsyncImage(voucher.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(voucher.accent).copy(alpha = 0.92f), Color.Transparent))))
                Column(Modifier.align(Alignment.CenterStart).padding(20.dp)) {
                    androidx.compose.material3.Text(voucher.headlineValue(), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, fontFamily = SgTextStyle.Label.fontFamily)
                    Text(voucher.title, style = SgTextStyle.Label, color = Color.White)
                }
            }
            Row(Modifier.padding(top = SgSpacing.Lg), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(l("Kode voucher", "Voucher code"), style = SgTextStyle.Caption)
                    androidx.compose.material3.Text(voucher.code, style = SgTextStyle.Title.copy(letterSpacing = 1.5.sp))
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(SgRadius.Pill))
                        .background(SgColor.Mint)
                        .pressable({ clipboard.setText(AnnotatedString(voucher.code)) })
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = SgColor.Brand700, modifier = Modifier.size(16.dp))
                    Text(l(" Salin", " Copy"), style = SgTextStyle.Label, color = SgColor.Brand700)
                }
            }
            InfoRow(Icons.Rounded.Storefront, l("Berlaku untuk", "Valid for"), voucher.merchantName ?: voucher.appliesTo)
            InfoRow(Icons.Rounded.Schedule, l("Berlaku sampai", "Valid until"), expiryFormat.format(voucher.expiresAt.atZone(ZoneId.systemDefault())))
            Text(l("Syarat & ketentuan", "Terms & conditions"), style = SgTextStyle.Title, modifier = Modifier.padding(top = SgSpacing.Lg, bottom = SgSpacing.Sm))
            voucher.terms.forEachIndexed { i, term ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Box(
                        Modifier.size(20.dp).background(SgColor.Mint, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { androidx.compose.material3.Text("${i + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SgColor.Brand700) }
                    Text(term, style = SgTextStyle.Body.copy(color = SgColor.Ink), modifier = Modifier.padding(start = 10.dp))
                }
            }
            Spacer(Modifier.height(SgSpacing.Lg))
            SgButton(
                text = if (claimed) l("Sudah diklaim · pakai saat checkout", "Claimed · use at checkout") else l("Klaim voucher", "Claim voucher"),
                onClick = { if (!claimed) onClaim(); onDismiss() },
                enabled = !claimed,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(SgSpacing.Md))
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.padding(top = SgSpacing.Md), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(SgColor.Page, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = SgColor.InkMuted, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(label, style = SgTextStyle.Caption)
            Text(value, style = SgTextStyle.Label)
        }
    }
}
