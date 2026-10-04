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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.ui.components.SgLogo
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.theme.SisaGunaTheme

enum class AccountType { REGULAR, MERCHANT }

private enum class RegisterStep { ACCOUNT_TYPE, DETAILS }

/** No dedicated Figma frame exists for Register (confirmed via get_metadata search — only
 * "Login", node 317:10108, is designed for this flow). Step 1 (account type) keeps its existing
 * validated layout; step 2's form fields reuse Login's exact field styling (bg #FAFAFA, border
 * #E5E5E5, radius 12dp, label style) for visual consistency rather than inventing new UI.
 * Frontend-only: "Daftar Sekarang" only does basic UI validation, no real backend call. */
@Composable
fun RegisterScreen(
    onRegisterComplete: (AccountType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(RegisterStep.ACCOUNT_TYPE) }
    var accountType by remember { mutableStateOf(AccountType.REGULAR) }

    when (step) {
        RegisterStep.ACCOUNT_TYPE -> AccountTypeStep(
            modifier = modifier,
            selected = accountType,
            onSelect = { accountType = it },
            onNext = { step = RegisterStep.DETAILS },
        )
        RegisterStep.DETAILS -> DetailsStep(
            modifier = modifier,
            onBack = { step = RegisterStep.ACCOUNT_TYPE },
            onSubmit = { onRegisterComplete(accountType) },
        )
    }
}

@Composable
private fun AccountTypeStep(
    selected: AccountType,
    onSelect: (AccountType) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    description = "Ambil makanan berlebih yang lezat dari resto sekitar dengan diskon melimpah atau gratis demi misi penyelamatan lingkungan.",
                    selected = selected == AccountType.REGULAR,
                    onClick = { onSelect(AccountType.REGULAR) },
                )
                AccountTypeCard(
                    emoji = "🏪",
                    title = "Mitra Restoran",
                    description = "Redistribusikan makanan sisa hari ini, kurangi sampah organik, dan raih profit tambahan secara cepat dan transparan.",
                    selected = selected == AccountType.MERCHANT,
                    onClick = { onSelect(AccountType.MERCHANT) },
                )
            }
        }
        PrimaryButton(label = "Lanjutkan Registrasi", onClick = onNext)
    }
}

@Composable
private fun DetailsStep(
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().background(SgColor.BaseWhite).imePadding()) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Kembali",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SgColor.Brand600,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text(text = "Lengkapi Data Diri", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SgColor.Neutral800)
                Text(text = "Data ini dipakai untuk akun SisaGuna kamu.", style = SgTextStyle.TextSmRegular, color = SgColor.Neutral500)
            }
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RegisterField(label = "Nama Lengkap", value = fullName, onValueChange = { fullName = it })
                RegisterField(label = "Email", value = email, onValueChange = { email = it }, keyboardType = KeyboardType.Email)
                RegisterField(label = "Nomor HP", value = phone, onValueChange = { phone = it }, keyboardType = KeyboardType.Phone)
                PasswordField(
                    label = "Kata Sandi",
                    value = password,
                    onValueChange = { password = it },
                    visible = passwordVisible,
                    onToggleVisible = { passwordVisible = !passwordVisible },
                )
                PasswordField(
                    label = "Konfirmasi Kata Sandi",
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    visible = passwordVisible,
                    onToggleVisible = { passwordVisible = !passwordVisible },
                )
                errorMessage?.let { message ->
                    Text(text = message, fontSize = 12.sp, color = SgColor.RedStatus)
                }
            }
        }
        PrimaryButton(
            label = "Daftar Sekarang",
            onClick = {
                errorMessage = when {
                    fullName.isBlank() || email.isBlank() || phone.isBlank() -> "Lengkapi semua data terlebih dahulu."
                    password.isBlank() -> "Kata sandi tidak boleh kosong."
                    password != confirmPassword -> "Konfirmasi kata sandi tidak cocok."
                    else -> null
                }
                if (errorMessage == null) onSubmit()
            },
        )
    }
}

@Composable
private fun RegisterField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = SgTextStyle.TextXsMedium, color = SgColor.Neutral500)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SgColor.Neutral50, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, SgColor.Neutral200), RoundedCornerShape(12.dp))
                .padding(14.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Neutral800),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = SgTextStyle.TextXsMedium, color = SgColor.Neutral500)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SgColor.Neutral50, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, SgColor.Neutral200), RoundedCornerShape(12.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Neutral800),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (visible) "Sembunyikan" else "Tampilkan",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SgColor.Brand600,
                modifier = Modifier.clickable(onClick = onToggleVisible),
            )
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .background(SgColor.Brand500, RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SgColor.OnBrand)
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
                    Text(text = "Aktif", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SgColor.OnBrand)
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
private fun RegisterScreenAccountTypePreview() {
    SisaGunaTheme {
        AccountTypeStep(selected = AccountType.REGULAR, onSelect = {}, onNext = {})
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun RegisterScreenDetailsPreview() {
    SisaGunaTheme {
        DetailsStep(onBack = {}, onSubmit = {})
    }
}
