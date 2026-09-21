package com.sisaguna.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sisaguna.android.R
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle

/**
 * Search field at the top of Home (Figma node 31:5187). White pill, no border, search icon
 * left, filter icon right — not a Material outlined field, that look didn't match the design.
 */
@Composable
fun SgSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onFilterClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .background(SgColor.BaseWhite, RoundedCornerShape(30.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = SgColor.Neutral500,
            modifier = Modifier.padding(end = 8.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(text = placeholder, style = SgTextStyle.TextSmRegular, color = SgColor.Neutral500)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Neutral800),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_filter_inner),
            contentDescription = "Filter",
            tint = Color.Unspecified,
            modifier = Modifier.clickable(onClick = onFilterClick),
        )
    }
}
