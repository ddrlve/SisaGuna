package com.sisaguna.android.feature.address

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.i18n.Text
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle

/**
 * Floats above Home until a location is known. Distances and "terdekat" only mean something
 * with a position, so this explains why before the system permission dialog appears.
 */
@Composable
fun LocationPromptCard(
    visible: Boolean,
    requester: LocationRequester,
    onPickManually: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        Column(
            Modifier
                .padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Md)
                .shadow(8.dp, RoundedCornerShape(SgRadius.Card))
                .background(SgColor.BaseWhite, RoundedCornerShape(SgRadius.Card))
                .padding(SgSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md)) {
                Box(Modifier.size(40.dp).background(SgColor.Mint, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = SgColor.Brand600)
                }
                Column(Modifier.weight(1f)) {
                    Text("Aktifkan lokasi", style = SgTextStyle.Title)
                    Text(
                        "Supaya kami bisa menampilkan makanan terdekat dan jaraknya dari kamu.",
                        style = SgTextStyle.Body,
                    )
                }
            }
            requester.error?.let { Text(it, style = SgTextStyle.Caption, color = SgColor.RedStatus) }
            Row(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm), modifier = Modifier.fillMaxWidth()) {
                SgButton(
                    "Nanti saja",
                    onClick = onDismiss,
                    style = SgButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
                SgButton(
                    if (requester.busy) "Mencari sinyal…" else "Izinkan lokasi",
                    onClick = requester.request,
                    enabled = !requester.busy,
                    modifier = Modifier.weight(1.4f),
                )
            }
            if (requester.error != null) {
                SgButton("Pilih lewat peta atau alamat", onClick = onPickManually, style = SgButtonStyle.Ghost, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
