package com.sisaguna.android.ui.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sisaguna.android.ui.theme.SgColor

/** Generic tier/category filter chip, shape TBD once Figma spacing tokens are confirmed. */
@Composable
fun SgChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SgColor.Brand500,
            selectedLabelColor = SgColor.BaseWhite,
        ),
    )
}
