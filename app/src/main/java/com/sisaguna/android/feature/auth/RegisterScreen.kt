package com.sisaguna.android.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.SgEaseOut
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.i18n.l
import kotlinx.coroutines.delay
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

/**
 * Step 1: who is this account for. Two radio cards; the selected one opens to show what the
 * role gets, so the choice is made on content rather than on a title. The CTA names the role
 * ("Lanjut sebagai Mitra") so the button confirms what was picked.
 */
@Composable
private fun AccountTypeStep(
    selected: AccountType,
    onSelect: (AccountType) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(SgColor.Page)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            SgLogo(markSize = 28, textSize = 17, textColor = SgColor.Ink, modifier = Modifier.padding(top = 24.dp))
            Text(
                text = l("Mau pakai SisaGuna sebagai apa?", "How will you use SisaGuna?"),
                style = SgTextStyle.Display.copy(fontSize = 26.sp, lineHeight = 32.sp),
                color = SgColor.Ink,
                modifier = Modifier.padding(top = 28.dp),
            )
            Text(
                text = l("Pilih satu. Kamu bisa menggantinya nanti di Pengaturan.", "Pick one. You can switch later in Settings."),
                style = SgTextStyle.Body,
                color = SgColor.InkMuted,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AccountTypeCard(
                    index = 0,
                    icon = Icons.Rounded.ShoppingBag,
                    iconBg = SgColor.Mint,
                    iconTint = SgColor.Brand700,
                    title = l("Pembeli", "Buyer"),
                    subtitle = l("Selamatkan makanan enak di sekitarmu", "Rescue good food near you"),
                    perks = listOf(
                        l("Diskon 50-70%, ada juga yang gratis", "50-70% off, some for free"),
                        l("Ambil sendiri atau kirim pakai kurir", "Pick up yourself or get it delivered"),
                        l("Bayar QRIS atau e-wallet", "Pay with QRIS or e-wallet"),
                    ),
                    selected = selected == AccountType.REGULAR,
                    onClick = { onSelect(AccountType.REGULAR) },
                )
                AccountTypeCard(
                    index = 1,
                    icon = Icons.Rounded.Storefront,
                    iconBg = SgColor.Farm,
                    iconTint = SgColor.FarmInk,
                    title = l("Mitra penjual", "Seller partner"),
                    subtitle = l("Warung, bakery, kantin, atau dapur rumahan", "Food stalls, bakeries, canteens or home kitchens"),
                    perks = listOf(
                        l("Jual makanan berlebih sebelum toko tutup", "Sell extra food before closing"),
                        l("Tanpa biaya awal, komisi 10% per penjualan", "No upfront cost, 10% per sale"),
                        l("Sisa yang tak layak jadi pakan atau kompos", "Leftovers go to feed or compost"),
                    ),
                    selected = selected == AccountType.MERCHANT,
                    onClick = { onSelect(AccountType.MERCHANT) },
                )
            }
        }
        Column(Modifier.background(SgColor.Page).navigationBarsPadding().padding(24.dp)) {
            SgButton(
                text = if (selected == AccountType.MERCHANT) l("Lanjut sebagai Mitra", "Continue as Seller") else l("Lanjut sebagai Pembeli", "Continue as Buyer"),
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                height = 54.dp,
            )
        }
    }
}

@Composable
private fun AccountTypeCard(
    index: Int,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    perks: List<String>,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // First-time screen: one short staggered entrance, then nothing moves except the choice.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(60L * index)
        entrance.animateTo(1f, tween(320, easing = SgEaseOut))
    }
    val border by animateColorAsState(if (selected) SgColor.Brand500 else SgColor.Hairline, tween(150), label = "typeBorder")
    val bg by animateColorAsState(if (selected) SgColor.BaseWhite else SgColor.BaseWhite.copy(alpha = 0.6f), tween(150), label = "typeBg")
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .graphicsLayer {
                alpha = entrance.value
                translationY = (1f - entrance.value) * 12.dp.toPx()
            }
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, border, shape)
            .pressable(onClick, role = Role.RadioButton, pressedScale = 0.98f)
            .semantics { this.selected = selected }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(iconBg, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, style = SgTextStyle.Title, color = SgColor.Ink)
                Text(subtitle, style = SgTextStyle.Caption, color = SgColor.InkMuted, modifier = Modifier.padding(top = 2.dp))
            }
            RadioDot(selected)
        }
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(180, easing = SgEaseOut)) + expandVertically(tween(220, easing = SgEaseOut)),
            exit = fadeOut(tween(100)) + shrinkVertically(tween(180, easing = SgEaseOut)),
        ) {
            Column(
                Modifier.padding(top = 14.dp).fillMaxWidth().background(SgColor.Page, RoundedCornerShape(14.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                perks.forEach { perk ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SgColor.Brand500, modifier = Modifier.size(16.dp))
                        Text(perk, style = SgTextStyle.Body, color = SgColor.Ink, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

/** Radio indicator: ring when idle, filled ring with a dot when picked. */
@Composable
private fun RadioDot(selected: Boolean) {
    val ring by animateColorAsState(if (selected) SgColor.Brand500 else SgColor.Neutral300, tween(150), label = "radioRing")
    val dot by animateFloatAsState(if (selected) 1f else 0f, tween(180, easing = SgEaseOut), label = "radioDot")
    Box(
        Modifier.size(22.dp).border(2.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .graphicsLayer {
                    scaleX = 0.5f + 0.5f * dot
                    scaleY = 0.5f + 0.5f * dot
                    alpha = dot
                }
                .background(SgColor.Brand500, CircleShape),
        )
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
