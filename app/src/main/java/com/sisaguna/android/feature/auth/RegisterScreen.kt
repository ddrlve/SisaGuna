package com.sisaguna.android.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.ui.components.SgLogo
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.theme.SisaGunaTheme

enum class AccountType { REGULAR, MERCHANT }

/** Matches Figma node 70:2407 "Register". Account-type step only — the rest of registration
 * (form fields, submit) isn't in the Figma file yet. */
@Composable
fun RegisterScreen(
    onContinue: (AccountType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(AccountType.REGULAR) }

    Column(modifier = modifier.fillMaxSize().background(SgColor.Neutral50)) {
        Column(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SgLogo(textColor = SgColor.Brand500)
                Text(
                    text = "Pilih Tipe Akun Anda",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SgColor.Neutral800,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Sesuaikan peran Anda untuk mengakses sistem terbaik SisaGuna.",
                    style = SgTextStyle.TextSmRegular,
                    color = SgColor.Neutral500,
                    textAlign = TextAlign.Center,
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AccountTypeCard(
                    emoji = "😋",
                    title = "Pengguna Biasa",
                    description = "Ambil makanan surplus lezat dari resto sekitar dengan diskon melimpah atau gratis demi misi penyelamatan lingkungan.",
                    selected = selected == AccountType.REGULAR,
                    onClick = { selected = AccountType.REGULAR },
                )
                AccountTypeCard(
                    emoji = "🏪",
                    title = "Mitra Restoran",
                    description = "Redistribusikan makanan sisa hari ini, kurangi sampah organik, dan raih profit tambahan secara cepat dan transparan.",
                    selected = selected == AccountType.MERCHANT,
                    onClick = { selected = AccountType.MERCHANT },
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(SgColor.Brand500, RoundedCornerShape(100.dp))
                .clickable { onContinue(selected) }
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Lanjutkan Registrasi", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SgColor.BaseWhite)
        }
    }
}

@Composable
private fun AccountTypeCard(
    emoji: String,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) SgColor.Green50 else SgColor.BaseWhite, RoundedCornerShape(20.dp))
            .border(
                BorderStroke(1.dp, if (selected) SgColor.Brand500 else SgColor.Neutral200),
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = emoji, fontSize = 22.sp)
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) SgColor.Brand600 else SgColor.Neutral800,
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .background(SgColor.Brand500, RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(text = "Aktif", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SgColor.BaseWhite)
                }
            }
        }
        Text(
            text = description,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = if (selected) SgColor.Neutral800 else SgColor.Neutral500,
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun RegisterScreenPreview() {
    SisaGunaTheme {
        RegisterScreen(onContinue = {})
    }
}
