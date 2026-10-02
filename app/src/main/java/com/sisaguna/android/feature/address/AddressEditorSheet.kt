package com.sisaguna.android.feature.address

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgButtonStyle
import com.sisaguna.android.ui.components.SgChip
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle

private val presetLabels = listOf("Rumah", "Kantor", "Kos", "Apartemen")

/** Names a picked map point and adds a note for the merchant/courier ("pagar hitam"). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddressEditorSheet(
    place: Place,
    initialLabel: String,
    initialNote: String,
    onChangeLocation: () -> Unit,
    onSave: (label: String, note: String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var label by remember { mutableStateOf(initialLabel) }
    var note by remember { mutableStateOf(initialNote) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SgColor.BaseWhite,
        scrimColor = SgColor.Ink.copy(alpha = 0.25f),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = SgSpacing.Gutter).padding(bottom = SgSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            Text(if (onDelete != null) "Ubah alamat" else "Simpan alamat", style = SgTextStyle.Title)
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(SgRadius.Tile)).background(SgColor.Page).padding(SgSpacing.Md),
                horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(36.dp).background(SgColor.Mint, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = SgColor.Brand600)
                }
                Column(Modifier.weight(1f)) {
                    Text(place.shortLabel, style = SgTextStyle.Label)
                    Text(place.fullAddress, style = SgTextStyle.Caption, maxLines = 2)
                }
                SgButton("Ubah", onClick = onChangeLocation, style = SgButtonStyle.Ghost, height = 36.dp)
            }
            Text("Nama alamat", style = SgTextStyle.Label)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SgSpacing.Sm)) {
                presetLabels.forEach { preset -> SgChip(preset, label == preset, { label = preset }) }
            }
            Field(value = label, onChange = { label = it.take(30) }, placeholder = "Atau tulis sendiri, misal: Rumah Ibu")
            Text("Catatan (opsional)", style = SgTextStyle.Label)
            Field(value = note, onChange = { note = it.take(80) }, placeholder = "Contoh: pagar hitam, sebelah warung")
            SgButton(
                "Simpan alamat",
                onClick = { onSave(label, note) },
                enabled = label.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = SgSpacing.Sm),
            )
            if (onDelete != null) {
                SgButton("Hapus alamat", onClick = onDelete, style = SgButtonStyle.Destructive, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(placeholder, style = SgTextStyle.Body) },
        textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Ink),
        shape = RoundedCornerShape(SgRadius.Thumb),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SgColor.Brand500, unfocusedBorderColor = SgColor.Hairline),
        modifier = Modifier.fillMaxWidth(),
    )
}
