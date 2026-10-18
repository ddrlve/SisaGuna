package com.sisaguna.android.feature.auth

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.ui.components.SgLogo
import com.sisaguna.android.ui.i18n.Text
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle

/** Reset a forgotten password in two steps: email, then the 6-digit code from that email and a new password. */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }

    var email by remember { mutableStateOf(state.email) }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val onCodeStep = state.step == ResetStep.CODE

    Column(Modifier.fillMaxSize().background(SgColor.BaseWhite).imePadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(
                Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SgLogo(textColor = SgColor.Neutral800)
                Text(
                    text = "Kembali",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SgColor.Brand600,
                    modifier = Modifier.clickable { if (onCodeStep) viewModel.back() else onBack() },
                )
                Text(
                    text = if (onCodeStep) "Masukkan Kode" else "Lupa Kata Sandi?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = SgColor.Neutral800,
                )
                Text(
                    text = if (onCodeStep) "Kami mengirim kode 6 digit ke ${state.email}. Kode berlaku 15 menit."
                    else "Masukkan email akunmu. Kami kirim kode 6 digit untuk mengatur ulang kata sandi.",
                    style = SgTextStyle.TextSmRegular,
                    color = SgColor.Neutral500,
                )
            }
            Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!onCodeStep) {
                    LabeledField(label = "Email") {
                        BasicTextField(
                            value = email,
                            onValueChange = { email = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Neutral800),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    LabeledField(label = "Kode 6 digit") {
                        BasicTextField(
                            value = code,
                            onValueChange = { code = it.filter(Char::isDigit).take(6) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Neutral800, letterSpacing = 4.sp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    LabeledField(label = "Kata Sandi Baru") {
                        PasswordInput(password, { password = it }, visible) { visible = !visible }
                    }
                    LabeledField(label = "Konfirmasi Kata Sandi Baru") {
                        PasswordInput(confirm, { confirm = it }, visible) { visible = !visible }
                    }
                    Text(
                        text = if (state.cooldown > 0) "Kirim ulang kode dalam ${state.cooldown} detik" else "Kirim ulang kode",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (state.cooldown > 0) SgColor.Neutral400 else SgColor.Brand600,
                        modifier = Modifier.clickable(enabled = state.cooldown == 0 && !state.loading) { viewModel.resend() },
                    )
                }
                state.error?.let { Text(text = it, fontSize = 12.sp, color = SgColor.RedStatus) }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(SgColor.Brand500, RoundedCornerShape(100.dp))
                .clickable(enabled = !state.loading) {
                    if (onCodeStep) viewModel.reset(code, password, confirm) else viewModel.requestCode(email)
                }
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when {
                    state.loading -> "Memproses…"
                    onCodeStep -> "Atur Ulang Kata Sandi"
                    else -> "Kirim Kode"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SgColor.OnBrand,
            )
        }
    }
}

@Composable
private fun PasswordInput(value: String, onChange: (String) -> Unit, visible: Boolean, onToggle: () -> Unit) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
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
            modifier = Modifier.clickable(onClick = onToggle),
        )
    }
}
