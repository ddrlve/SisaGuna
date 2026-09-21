package com.sisaguna.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sisaguna.android.ui.theme.SgColor

/**
 * Honest placeholder for a bottom-nav destination with no Figma design yet, so tapping the
 * tab does something real instead of nothing — see ANDROID_CLAUDE.md status on Saved/Profile.
 */
@Composable
fun ComingSoonScreen(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$title belum tersedia di sesi ini",
            style = MaterialTheme.typography.bodyMedium,
            color = SgColor.Neutral500,
            textAlign = TextAlign.Center,
        )
    }
}
