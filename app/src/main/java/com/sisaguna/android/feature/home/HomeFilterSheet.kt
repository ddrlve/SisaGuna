package com.sisaguna.android.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.components.SgChip
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle

private val distanceOptions = listOf<Pair<Double?, String>>(1.0 to "< 1 km", 2.0 to "< 2 km", 5.0 to "< 5 km", null to "Semua")

/**
 * Draft-then-apply: changes stay local until "Terapkan", so browsing options never reshuffles
 * the feed behind the sheet. "Reset" returns the draft to defaults without closing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeFilterSheet(
    current: HomeFilter,
    onApply: (HomeFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(current) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SgColor.BaseWhite,
        scrimColor = SgColor.Ink.copy(alpha = 0.32f),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = SgSpacing.Gutter).padding(bottom = SgSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            Text("Filter & urutkan", style = SgTextStyle.Title)

            Text("Urutkan", style = SgTextStyle.Label, modifier = Modifier.padding(top = SgSpacing.Sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm), verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                HomeSort.entries.forEach { sort ->
                    SgChip(sort.label, draft.sort == sort, { draft = draft.copy(sort = sort) })
                }
            }

            Text("Jarak", style = SgTextStyle.Label, modifier = Modifier.padding(top = SgSpacing.Sm))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                distanceOptions.forEach { (km, label) ->
                    SgChip(label, draft.maxDistanceKm == km, { draft = draft.copy(maxDistanceKm = km) })
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SgSpacing.Sm)
                    .toggleable(draft.freeOnly, role = Role.Switch) { draft = draft.copy(freeOnly = it) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Gratis saja", style = SgTextStyle.Label)
                    Text("Tampilkan makanan tanpa biaya", style = SgTextStyle.Caption)
                }
                Switch(
                    checked = draft.freeOnly,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = SgColor.Brand500,
                        uncheckedTrackColor = SgColor.Hairline,
                        uncheckedBorderColor = SgColor.Hairline,
                        uncheckedThumbColor = SgColor.BaseWhite,
                    ),
                )
            }

            Row(
                modifier = Modifier.padding(top = SgSpacing.Lg),
                horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
            ) {
                SgButton("Reset", onClick = { draft = HomeFilter() }, style = SgButtonStyle.Secondary, modifier = Modifier.weight(1f))
                SgButton("Terapkan", onClick = { onApply(draft) }, modifier = Modifier.weight(2f))
            }
        }
    }
}
