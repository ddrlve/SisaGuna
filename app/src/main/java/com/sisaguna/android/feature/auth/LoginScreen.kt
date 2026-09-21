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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.ui.components.SgLogo
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.theme.SisaGunaTheme

/**
 * Matches Figma node 70:2365 "Login". No real auth backend this session (repo is
 * frontend-only, see README) — "Masuk" just calls [onLoginSuccess] unconditionally.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(SgColor.BaseWhite)) {
        Column(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SgLogo(textColor = SgColor.Neutral800)
                Text(text = "Selamat Datang!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SgColor.Neutral800)
                Text(text = "Masuk untuk mulai menyelamatkan makanan hari ini.", style = SgTextStyle.TextSmRegular, color = SgColor.Neutral500)
            }
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LabeledField(label = "Email atau Nomor HP") {
                    if (email.isEmpty()) {
                        Text(text = "user.sisaguna@gmail.com", style = SgTextStyle.TextSmRegular, color = SgColor.Neutral400)
                    }
                    BasicTextField(
                        value = email,
                        onValueChange = { email = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Neutral800),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Kata Sandi", style = SgTextStyle.TextXsMedium, color = SgColor.Neutral500)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SgColor.Neutral50, RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, SgColor.Neutral200), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        BasicTextField(
                            value = password,
                            onValueChange = { password = it },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Neutral800),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (passwordVisible) "Sembunyikan" else "Tampilkan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SgColor.Brand600,
                            modifier = Modifier.clickable { passwordVisible = !passwordVisible },
                        )
                    }
                }
                Text(
                    text = "Lupa kata sandi?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = SgColor.Brand600,
                    modifier = Modifier.fillMaxWidth().clickable { /* not built yet this session */ },
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SgColor.Brand500, RoundedCornerShape(100.dp))
                    .clickable(onClick = onLoginSuccess)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Masuk", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SgColor.BaseWhite)
            }
            Row {
                Text(text = "Belum punya akun? ", style = SgTextStyle.TextSmRegular, color = SgColor.Neutral500)
                Text(
                    text = "Daftar disini",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SgColor.Brand600,
                    modifier = Modifier.clickable(onClick = onRegisterClick),
                )
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = SgTextStyle.TextXsMedium, color = SgColor.Neutral500)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SgColor.Neutral50, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, SgColor.Neutral200), RoundedCornerShape(12.dp))
                .padding(14.dp),
        ) {
            content()
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun LoginScreenPreview() {
    SisaGunaTheme {
        LoginScreen(onLoginSuccess = {}, onRegisterClick = {})
    }
}
