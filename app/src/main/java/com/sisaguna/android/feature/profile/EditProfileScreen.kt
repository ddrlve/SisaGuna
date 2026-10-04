package com.sisaguna.android.feature.profile

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import com.sisaguna.android.ui.i18n.SgSnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.R
import com.sisaguna.android.ui.components.SgTopBar
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.domain.InitialAvatar
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import kotlinx.coroutines.launch

/** Figma 273:12569. */
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    var showDiscard by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.common_coming_soon)

    // Reading form keeps these in sync with each keystroke.
    val dirty = form.let { viewModel.isDirty }
    val canSave = form.let { viewModel.canSave }

    val attemptBack = { if (dirty) showDiscard = true else onBack() }
    BackHandler(enabled = dirty) { showDiscard = true }

    Scaffold(
        containerColor = SgColor.Page,
        topBar = { SgTopBar(title = stringResource(R.string.edit_profile_title), onBack = attemptBack) },
        snackbarHost = { SgSnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Button(
                onClick = { if (viewModel.save()) onSaved() },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SgColor.Brand500,
                    disabledContainerColor = SgColor.Hairline,
                    disabledContentColor = SgColor.InkMuted,
                ),
                shape = RoundedCornerShape(SgRadius.Tile),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SgColor.Page)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Md)
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.edit_profile_save), style = SgTextStyle.Label, color = if (canSave) SgColor.BaseWhite else SgColor.InkMuted)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SgSpacing.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val profile by viewModel.profile.collectAsStateWithLifecycle()
            com.sisaguna.android.ui.domain.UserAvatar(
                profile = profile,
                size = 88.dp,
                ring = SgColor.Brand300,
                modifier = Modifier.padding(top = SgSpacing.Lg),
            )
            Text(
                stringResource(R.string.edit_profile_change_photo),
                style = SgTextStyle.Label,
                color = SgColor.Brand600,
                modifier = Modifier
                    .padding(top = SgSpacing.Sm)
                    .clip(RoundedCornerShape(SgRadius.Pill))
                    .pressable({
                        scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            snackbar.showSnackbar(comingSoon)
                        }
                    })
                    .padding(horizontal = SgSpacing.Md, vertical = SgSpacing.Xs),
            )
            Column(
                modifier = Modifier
                    .padding(top = SgSpacing.Xl)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SgRadius.Card))
                    .background(SgColor.BaseWhite)
                    .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Card))
                    .padding(SgSpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(SgSpacing.Lg),
            ) {
                ProfileTextField(
                    label = stringResource(R.string.edit_profile_name),
                    value = form.name,
                    onValueChange = viewModel::onNameChange,
                    error = form.visibleError(ProfileField.NAME),
                    onBlur = { viewModel.onBlur(ProfileField.NAME) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                )
                ProfileTextField(
                    label = stringResource(R.string.edit_profile_email),
                    value = form.email,
                    onValueChange = viewModel::onEmailChange,
                    error = form.visibleError(ProfileField.EMAIL),
                    onBlur = { viewModel.onBlur(ProfileField.EMAIL) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
                ProfileTextField(
                    label = stringResource(R.string.edit_profile_phone),
                    value = form.phone,
                    onValueChange = viewModel::onPhoneChange,
                    error = form.visibleError(ProfileField.PHONE),
                    onBlur = { viewModel.onBlur(ProfileField.PHONE) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    prefix = "+62 ",
                )
            }
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.edit_profile_discard_title), style = SgTextStyle.Title) },
            text = { Text(stringResource(R.string.edit_profile_discard_body), style = SgTextStyle.Body) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    onBack()
                }) { Text(stringResource(R.string.edit_profile_discard_confirm), style = SgTextStyle.Label, color = SgColor.RedStatus) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) {
                    Text(stringResource(R.string.common_cancel), style = SgTextStyle.Label, color = SgColor.Ink)
                }
            },
            containerColor = SgColor.BaseWhite,
        )
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    onBlur: () -> Unit,
    keyboardOptions: KeyboardOptions,
    prefix: String? = null,
) {
    var hadFocus by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = SgTextStyle.Label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            isError = error != null,
            textStyle = SgTextStyle.TextSmRegular.copy(color = SgColor.Ink),
            prefix = prefix?.let { { Text(it, style = SgTextStyle.TextSmRegular, color = SgColor.InkMuted) } },
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(SgRadius.Thumb),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SgColor.Brand500,
                unfocusedBorderColor = SgColor.Hairline,
                errorBorderColor = SgColor.RedStatus,
                cursorColor = SgColor.Brand500,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) hadFocus = true
                    else if (hadFocus) onBlur()
                },
        )
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(tween(150)) + expandVertically(tween(150)),
            exit = fadeOut(tween(100)) + shrinkVertically(tween(100)),
        ) {
            Text(error.orEmpty(), style = SgTextStyle.Caption, color = SgColor.RedStatus)
        }
    }
}
