package com.sisaguna.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgTextStyle

/**
 * Search pill. A clear (×) button appears once there's text. The filter button only shows when
 * [onFilterClick] is set, so screens without a filter sheet don't show a dead icon, and it
 * carries a count badge when filters are active.
 */
@Composable
fun SgSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    containerColor: Color = SgColor.BaseWhite,
    onFilterClick: (() -> Unit)? = null,
    activeFilterCount: Int = 0,
) {
    val focus = LocalFocusManager.current
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(SgRadius.Pill))
            .background(containerColor)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Pill))
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = SgColor.InkMuted, modifier = Modifier.size(20.dp))
        Box(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            if (value.isEmpty()) {
                Text(text = placeholder, style = SgTextStyle.TextSmRegular, color = SgColor.InkMuted, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Ink),
                cursorBrush = SolidColor(SgColor.Brand500),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AnimatedVisibility(
            visible = value.isNotEmpty(),
            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.8f),
            exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.8f),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .pressable({ onValueChange("") }, pressedScale = 0.9f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Hapus pencarian", tint = SgColor.InkMuted, modifier = Modifier.size(18.dp))
            }
        }
        if (onFilterClick != null) {
            Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(24.dp).background(SgColor.Hairline))
            Box(modifier = Modifier.size(40.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (activeFilterCount > 0) SgColor.Mint else Color.Transparent)
                        .pressable(onFilterClick, pressedScale = 0.9f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = if (activeFilterCount > 0) "Filter, $activeFilterCount aktif" else "Filter",
                        tint = if (activeFilterCount > 0) SgColor.Brand700 else SgColor.Ink,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (activeFilterCount > 0) {
                    Text(
                        activeFilterCount.toString(),
                        color = SgColor.BaseWhite,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                            .background(SgColor.Brand500, CircleShape)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}
