package com.sisaguna.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.i18n.Text

/** Store photo, or its first letter on a mint tile while the photo isn't known. */
@Composable
fun StoreAvatar(name: String, imageUrl: String?, size: Dp, radius: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(radius)
    if (imageUrl.isNullOrBlank()) {
        Box(modifier.size(size).clip(shape).background(SgColor.Mint), contentAlignment = Alignment.Center) {
            Text(name.trim().firstOrNull()?.uppercase().orEmpty(), style = SgTextStyle.Title, color = SgColor.Brand700)
        }
    } else {
        AsyncImage(
            imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(shape).background(SgColor.Mint),
        )
    }
}
