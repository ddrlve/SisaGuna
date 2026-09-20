package com.sisaguna.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Small pill label used for tier tags, "gratis", verified badges, etc. */
@Composable
fun SgBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = contentColor,
        fontSize = 11.sp,
        fontWeight = MaterialTheme.typography.labelSmall.fontWeight,
        modifier = modifier
            .background(containerColor, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
