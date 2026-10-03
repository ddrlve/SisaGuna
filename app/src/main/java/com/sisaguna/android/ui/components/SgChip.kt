package com.sisaguna.android.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgTextStyle

/**
 * Filter chip with every color set explicitly — never inherits from the color scheme, so it
 * stays readable whatever theme the device is in. Selected = brand fill + check; unselected =
 * white with a hairline border and Ink label.
 */
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
        label = { Text(label, style = SgTextStyle.Label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else {
            null
        },
        shape = RoundedCornerShape(SgRadius.Pill),
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SgColor.BaseWhite,
            labelColor = SgColor.Ink,
            iconColor = SgColor.Ink,
            selectedContainerColor = SgColor.Brand500,
            selectedLabelColor = SgColor.OnBrand,
            selectedLeadingIconColor = SgColor.OnBrand,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = SgColor.Hairline,
            selectedBorderColor = SgColor.Brand500,
        ),
    )
}
