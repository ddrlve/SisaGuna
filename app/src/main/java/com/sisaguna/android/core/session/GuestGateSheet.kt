package com.sisaguna.android.core.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.theme.SisaGunaTheme

/** Shown when a guest taps a feature that needs an account (Upload CTA, Activity/Saved/Profile
 * tabs). No dedicated Figma frame exists for this yet, so it reuses Login's rounded-pill CTA
 * language for visual consistency rather than inventing new UI. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestGateSheet(
    onDismiss: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        GuestGateSheetContent(onLoginClick = onLoginClick, onRegisterClick = onRegisterClick)
    }
}

@Composable
private fun GuestGateSheetContent(onLoginClick: () -> Unit, onRegisterClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Masuk untuk lanjutkan",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SgColor.Neutral800,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Buat atau masuk ke akun SisaGuna kamu untuk pakai fitur ini.",
            style = SgTextStyle.TextSmRegular,
            color = SgColor.Neutral500,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SgColor.Brand500, RoundedCornerShape(100.dp))
                .clickable(onClick = onLoginClick)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Masuk", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SgColor.OnBrand)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SgColor.Neutral100, RoundedCornerShape(100.dp))
                .clickable(onClick = onRegisterClick)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Daftar Akun Baru", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SgColor.Neutral800)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GuestGateSheetContentPreview() {
    SisaGunaTheme {
        GuestGateSheetContent(onLoginClick = {}, onRegisterClick = {})
    }
}
