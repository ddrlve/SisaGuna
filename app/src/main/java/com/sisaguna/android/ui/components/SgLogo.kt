package com.sisaguna.android.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import com.sisaguna.android.ui.theme.SgFont
import com.sisaguna.android.R
import com.sisaguna.android.ui.theme.SgColor

/** Brand mark (Figma "Group 6" / "Logo 26/34/38") — the double-chevron glyph + "sisaguna". */
@Composable
fun SgLogo(
    modifier: Modifier = Modifier,
    markSize: Int = 32,
    textSize: Int = 18,
    markTint: Color = Color.Unspecified,
    textColor: Color = SgColor.Neutral800,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_logo_mark),
            contentDescription = null,
            tint = markTint,
            modifier = Modifier.size(markSize.dp),
        )
        // Font padding and the default line box put the wordmark visibly below the mark's
        // centre; trimming both makes Row's CenterVertically centre the actual glyphs.
        Text(
            text = "sisaguna",
            color = textColor,
            style = TextStyle(
                fontFamily = SgFont,
                fontSize = textSize.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                lineHeight = textSize.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
