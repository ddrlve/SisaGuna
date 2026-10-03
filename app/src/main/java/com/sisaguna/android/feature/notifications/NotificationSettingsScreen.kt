package com.sisaguna.android.feature.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.sisaguna.android.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sisaguna.android.R
import com.sisaguna.android.data.model.NotificationPrefs
import com.sisaguna.android.ui.components.SgTopBar
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle

private data class PrefRow(
    val title: Int,
    val body: Int,
    val value: (NotificationPrefs) -> Boolean,
    val set: NotificationPrefs.(Boolean) -> NotificationPrefs,
)

private val prefRows = listOf(
    PrefRow(R.string.notif_pref_pickup, R.string.notif_pref_pickup_body, { it.pickup }, { copy(pickup = it) }),
    PrefRow(R.string.notif_pref_order, R.string.notif_pref_order_body, { it.order }, { copy(order = it) }),
    PrefRow(R.string.notif_pref_merchant, R.string.notif_pref_merchant_body, { it.merchant }, { copy(merchant = it) }),
    PrefRow(R.string.notif_pref_promo, R.string.notif_pref_promo_body, { it.promo }, { copy(promo = it) }),
    PrefRow(R.string.notif_pref_impact, R.string.notif_pref_impact_body, { it.impact }, { copy(impact = it) }),
)

/** Spec addendum: changes save immediately — no save button. */
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val childAlpha by animateFloatAsState(if (prefs.enabled) 1f else 0.38f, tween(180), label = "prefsAlpha")

    Scaffold(
        containerColor = SgColor.Page,
        topBar = { SgTopBar(title = stringResource(R.string.notif_settings_title), onBack = onBack) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SgSpacing.Gutter, vertical = SgSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(SgSpacing.Md),
        ) {
            SettingsCard {
                SwitchRow(
                    title = stringResource(R.string.notif_settings_master),
                    body = stringResource(R.string.notif_settings_master_body),
                    checked = prefs.enabled,
                    enabled = true,
                    onCheckedChange = { on -> viewModel.update { copy(enabled = on) } },
                )
            }
            AnimatedVisibility(
                visible = !prefs.enabled,
                enter = fadeIn(tween(180)) + expandVertically(tween(180)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(120)),
            ) {
                Text(stringResource(R.string.notif_settings_off_note), style = SgTextStyle.Caption, modifier = Modifier.padding(horizontal = SgSpacing.Xs))
            }
            Text(
                stringResource(R.string.notif_settings_section),
                style = SgTextStyle.Title,
                modifier = Modifier.padding(top = SgSpacing.Md),
            )
            SettingsCard(modifier = Modifier.alpha(childAlpha)) {
                prefRows.forEachIndexed { index, row ->
                    SwitchRow(
                        title = stringResource(row.title),
                        body = stringResource(row.body),
                        checked = row.value(prefs),
                        enabled = prefs.enabled,
                        onCheckedChange = { on -> viewModel.update { row.set(this, on) } },
                    )
                    if (index < prefRows.lastIndex) HorizontalDivider(color = SgColor.Hairline, modifier = Modifier.padding(start = SgSpacing.Lg))
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SgRadius.Card))
            .background(SgColor.BaseWhite)
            .border(1.dp, SgColor.Hairline, RoundedCornerShape(SgRadius.Card)),
    ) { content() }
}

/** Whole row is the toggle target (not just the 52dp switch), per Material switch guidance. */
@Composable
private fun SwitchRow(
    title: String,
    body: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = SgSpacing.Lg, vertical = SgSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = SgTextStyle.Label)
            Text(body, style = SgTextStyle.Caption)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = SgColor.Brand500,
                checkedThumbColor = SgColor.OnBrand,
                uncheckedTrackColor = SgColor.Hairline,
                uncheckedBorderColor = SgColor.Hairline,
                uncheckedThumbColor = SgColor.OnBrand,
                disabledCheckedTrackColor = SgColor.Brand500,
                disabledCheckedThumbColor = SgColor.OnBrand,
                disabledUncheckedTrackColor = SgColor.Hairline,
                disabledUncheckedThumbColor = SgColor.OnBrand,
            ),
        )
    }
}
