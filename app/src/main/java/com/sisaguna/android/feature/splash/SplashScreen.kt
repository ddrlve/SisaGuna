package com.sisaguna.android.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.ui.components.SgEaseInOut
import com.sisaguna.android.ui.components.SgEaseOut
import com.sisaguna.android.ui.components.rememberReducedMotion
import com.sisaguna.android.ui.i18n.l
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgFont
import com.sisaguna.android.ui.theme.SisaGunaTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.Text as RawText

// The two halves of ic_logo_mark (32×32 viewport). Kept as path data so each half can move on
// its own — the mark is two arrows chasing each other, food going round again.
private const val HALF_A = "M10.2295 0L14.1639 3.98689L0 14.2689L6.50492 20.0393L14.8984 17.2066L10.859 14.2689L18.0984 8.02623L19.2525 9.44262L23.1869 0.891803L10.2295 0Z"
private const val HALF_B = "M21.7705 32L17.8361 28.0131L32 17.7311L25.4951 11.9607L17.1016 14.7934L21.141 17.7311L13.9016 23.9738L12.7475 22.5574L8.81311 31.1082L21.7705 32Z"

private val MarkSize = 56.dp
private val RingRadius = 50.dp

/**
 * Splash, built around what the mark means rather than a generic logo fade:
 *
 * 1. **Ring** (0–650ms): a thin white ring draws clockwise from 12 o'clock — the pickup clock
 *    the app shows on every listing. Strong ease-in-out: it's on-screen movement.
 * 2. **Lock** (from 420ms): the two arrow halves come in from opposite corners and lock into
 *    the mark. Spring, damping 0.78: they were "thrown", so a hint of settle is earned.
 * 3. **Release** (on lock): the ring expands and fades — the moment the food is rescued.
 * 4. **Name** (from 900ms): the wordmark slides out from behind the mark (clipped, so it's
 *    revealed, not flown in) while the group re-centres; the tagline follows.
 *
 * Only transform/alpha/draw-phase values animate, nothing relayouts. With system animations
 * off it shows the finished lockup immediately. Background stays brand green so the hand-off
 * to Landing (also green) is seamless.
 */
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val reduceMotion = rememberReducedMotion()
    val ring = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val lock = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val release = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val word = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val tagline = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            delay(900)
            onTimeout()
            return@LaunchedEffect
        }
        coroutineScope {
            launch { ring.animateTo(1f, tween(650, easing = SgEaseInOut)) }
            launch {
                delay(420)
                lock.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 260f))
            }
            launch {
                delay(760)
                release.animateTo(1f, tween(560, easing = SgEaseOut))
            }
            launch {
                delay(900)
                word.animateTo(1f, tween(480, easing = SgEaseOut))
            }
            launch {
                delay(1_180)
                tagline.animateTo(1f, tween(360, easing = SgEaseOut))
            }
        }
        delay(450) // let the finished lockup sit for a beat before Landing takes over
        onTimeout()
    }

    val halfA = remember { PathParser().parsePathString(HALF_A).toPath() }
    val halfB = remember { PathParser().parsePathString(HALF_B).toPath() }
    var wordWidthPx by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier.fillMaxSize().background(SgColor.Brand500),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            // Before the name shows, shift the group right by half the name's width so the
            // mark is dead centre; as the name reveals, the group glides back to centre.
            modifier = Modifier.graphicsLayer {
                translationX = (wordWidthPx + 12.dp.toPx()) / 2f * (1f - word.value)
            },
        ) {
            Canvas(
                modifier = Modifier
                    .size(MarkSize)
                    .drawBehind { drawRing(ring.value, release.value) },
            ) {
                val s = size.width / 32f
                val travel = 26.dp.toPx() * (1f - lock.value)
                val a = lock.value.coerceIn(0f, 1f)
                scale(s, pivot = Offset.Zero) {
                    // Half A arrives from the upper left, half B from the lower right.
                    translate(left = -travel / s, top = -travel / s) { drawPath(halfA, Color.White.copy(alpha = a)) }
                    translate(left = travel / s, top = travel / s) { drawPath(halfB, Color.White.copy(alpha = a)) }
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.clipToBounds()) {
                RawText(
                    text = "sisaguna",
                    fontFamily = SgFont,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.6).sp,
                    color = Color.White,
                    modifier = Modifier
                        .onSizeChanged { wordWidthPx = it.width }
                        .graphicsLayer {
                            translationX = -size.width * (1f - word.value)
                            alpha = word.value
                        },
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp)
                .graphicsLayer {
                    alpha = tagline.value
                    translationY = 8.dp.toPx() * (1f - tagline.value)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RawText(
                l("Selamatkan makanan, lindungi bumi", "Rescue food, protect the planet"),
                fontFamily = SgFont,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

/** Clockwise ring from 12 o'clock ([progress] 0→1), then expands and fades ([release] 0→1). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRing(progress: Float, release: Float) {
    if (release >= 1f) return
    val r = RingRadius.toPx() * (1f + 0.55f * release)
    val alpha = 0.85f * (1f - release)
    val stroke = 2.5.dp.toPx() * (1f - 0.6f * release)
    drawArc(
        color = Color.White.copy(alpha = alpha),
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = Offset(center.x - r, center.y - r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}

@Preview(showBackground = true, heightDp = 852)
@Composable
private fun SplashScreenPreview() {
    SisaGunaTheme {
        SplashScreen(onTimeout = {})
    }
}
