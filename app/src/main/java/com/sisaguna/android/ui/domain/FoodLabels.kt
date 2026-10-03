package com.sisaguna.android.ui.domain

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.data.model.Allergen
import com.sisaguna.android.data.model.HalalStatus
import com.sisaguna.android.data.model.SafetyAssessment
import com.sisaguna.android.data.model.SafetyCheck
import com.sisaguna.android.data.model.SafetyLevel
import com.sisaguna.android.data.model.StorageMethod
import com.sisaguna.android.ui.i18n.Text
import com.sisaguna.android.ui.i18n.l
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgTextStyle
import java.time.Duration
import java.time.Instant

private data class LevelStyle(val fg: Color, val bg: Color, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun SafetyLevel.style(): LevelStyle = when (this) {
    SafetyLevel.SAFE -> LevelStyle(SgColor.Brand700, SgColor.Mint, Icons.Rounded.CheckCircle)
    SafetyLevel.CAUTION -> LevelStyle(SgColor.YellowStatus, SgColor.Yellow50, Icons.Rounded.WarningAmber)
    SafetyLevel.UNSAFE -> LevelStyle(SgColor.RedStatus, SgColor.RedStatus.copy(alpha = 0.1f), Icons.Rounded.ErrorOutline)
    SafetyLevel.NOT_FOR_HUMANS -> LevelStyle(SgColor.CompostInk, SgColor.Compost, Icons.Rounded.HealthAndSafety)
}

/** Compact "✓ Lolos cek layak" chip for cards and lists. */
@Composable
fun SafetyChip(assessment: SafetyAssessment, modifier: Modifier = Modifier) {
    val st = assessment.level.style()
    Row(
        modifier = modifier.background(st.bg, RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(st.icon, contentDescription = null, tint = st.fg, modifier = Modifier.size(13.dp))
        Text(
            when (assessment.level) {
                SafetyLevel.SAFE -> l("Layak konsumsi", "Safe to eat")
                SafetyLevel.CAUTION -> l("Segera konsumsi", "Eat soon")
                SafetyLevel.UNSAFE -> l("Tidak layak", "Not safe")
                SafetyLevel.NOT_FOR_HUMANS -> l("Bukan untuk manusia", "Not for people")
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = st.fg,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/**
 * The full "Cek Kelayakan" card on the listing page: verdict, a freshness bar (time used of
 * the safe window), when it was cooked, how it's stored, and every self-check the merchant
 * ticked. Ends with the honest caveat that this is a rule, not a lab test.
 */
@Composable
fun SafetyCard(
    assessment: SafetyAssessment,
    madeAt: Instant?,
    storage: StorageMethod,
    modifier: Modifier = Modifier,
    now: Instant = Instant.now(),
) {
    val st = assessment.level.style()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SgColor.BaseWhite)
            .border(1.dp, st.fg.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(st.bg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(st.icon, contentDescription = null, tint = st.fg, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(l("Cek kelayakan", "Food safety check"), style = SgTextStyle.Caption)
                Text(assessment.headline, style = SgTextStyle.Title.copy(color = st.fg))
            }
            if (assessment.level != SafetyLevel.NOT_FOR_HUMANS) {
                Text(
                    "${assessment.score}/${SafetyCheck.entries.size}",
                    style = SgTextStyle.Label,
                    color = st.fg,
                    modifier = Modifier.background(st.bg, RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Text(assessment.advice, style = SgTextStyle.Body, modifier = Modifier.padding(top = 10.dp))

        if (madeAt != null && assessment.safeUntil != null) {
            val total = Duration.between(madeAt, assessment.safeUntil).toMinutes().coerceAtLeast(1)
            val used = Duration.between(madeAt, now).toMinutes().coerceIn(0, total)
            val left = Duration.between(now, assessment.safeUntil)
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = SgColor.InkMuted, modifier = Modifier.size(16.dp))
                Text(
                    l("Dimasak ", "Made ") + formatClock(madeAt) + " · " + storage.label,
                    style = SgTextStyle.Caption,
                    modifier = Modifier.padding(start = 6.dp).weight(1f),
                )
                Text(
                    if (left.isNegative) l("Lewat batas", "Expired") else l("Aman ", "Safe ") + humanDuration(left),
                    style = SgTextStyle.Caption.copy(fontWeight = FontWeight.Bold, color = st.fg),
                )
            }
            LinearProgressIndicator(
                progress = { 1f - used.toFloat() / total },
                color = st.fg,
                trackColor = SgColor.Hairline,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(6.dp),
            )
        }

        if (assessment.level != SafetyLevel.NOT_FOR_HUMANS) {
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SafetyCheck.entries.forEach { check ->
                    val ok = check in assessment.passedChecks
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (ok) SgColor.Brand500 else SgColor.Neutral300,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(check.label, style = SgTextStyle.Label.copy(fontSize = 13.sp))
                            Text(check.detail, style = SgTextStyle.Caption)
                        }
                    }
                }
            }
        }
        Text(
            l(
                "Dicek oleh mitra saat upload + aturan waktu simpan SisaGuna. Bukan uji lab, kalau ada yang janggal, laporkan lewat Komplain.",
                "Checked by the seller at upload + SisaGuna storage-time rules. Not a lab test, report anything off via Complaint.",
            ),
            style = SgTextStyle.Caption.copy(fontSize = 11.sp),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

fun humanDuration(d: Duration): String {
    val h = d.toHours()
    val m = d.toMinutes() % 60
    return when {
        h >= 24 -> "${h / 24} hari ${h % 24} jam"
        h > 0 -> "$h jam $m mnt"
        else -> "$m mnt"
    }
}

@Composable
fun HalalBadge(status: HalalStatus, modifier: Modifier = Modifier) {
    val (fg, bg) = when (status) {
        HalalStatus.HALAL_CERTIFIED -> SgColor.Brand700 to SgColor.Mint
        HalalStatus.NON_HALAL -> SgColor.RedStatus to SgColor.RedStatus.copy(alpha = 0.1f)
        HalalStatus.UNVERIFIED -> SgColor.YellowStatus to SgColor.Yellow50
        HalalStatus.OTHER -> SgColor.InkMuted to SgColor.Page
    }
    Text(
        text = when (status) {
            HalalStatus.HALAL_CERTIFIED -> l("☪ Halal", "☪ Halal")
            HalalStatus.NON_HALAL -> l("Non-halal", "Non-halal")
            HalalStatus.UNVERIFIED -> l("Belum terverifikasi halal", "Halal not verified")
            HalalStatus.OTHER -> l("Halal: lainnya", "Halal: other")
        },
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = fg,
        modifier = modifier.background(bg, RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Allergen chips ("🥜 Kacang tanah"). Shows a neutral "no common allergens" line when empty. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllergenChips(allergens: Set<Allergen>, modifier: Modifier = Modifier) {
    if (allergens.isEmpty()) {
        Text(l("Tidak ada alergen umum yang dideklarasikan", "No common allergens declared"), style = SgTextStyle.Caption, modifier = modifier)
        return
    }
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        allergens.forEach { a ->
            Text(
                "${a.emoji} ${a.label}",
                style = SgTextStyle.TextXsMedium,
                color = SgColor.FarmInk,
                modifier = Modifier.background(SgColor.Farm, RoundedCornerShape(SgRadius.Pill)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}
