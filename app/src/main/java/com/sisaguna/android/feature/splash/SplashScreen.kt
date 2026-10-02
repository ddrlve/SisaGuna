package com.sisaguna.android.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sisaguna.android.R
import com.sisaguna.android.ui.components.SgEaseOut
import com.sisaguna.android.ui.theme.Inter
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import com.sisaguna.android.ui.theme.SisaGunaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Brand opening (Figma "Splash screen" 255:5689–255:5722 is a motion sequence; its frames are
 * mid-animation keyframes). Sequence, ~1.9s total:
 * 1. a soft light disc grows from the centre (depth behind the mark);
 * 2. the mark springs in from 0.6 with a slight overshoot and a quarter turn;
 * 3. the wordmark slides out from behind the mark (clipped, so it reveals rather than flies);
 * 4. the tagline fades up.
 * Sizes are smaller than before (mark 40→30dp, text 28→22sp) after device feedback that the
 * logo felt oversized next to Surplus.
 */
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val disc = remember { Animatable(0f) }
    val mark = remember { Animatable(0f) }
    val word = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { disc.animateTo(1f, tween(900, easing = SgEaseOut)) }
        delay(150)
        launch { mark.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
        delay(350)
        launch { word.animateTo(1f, tween(450, easing = SgEaseOut)) }
        delay(350)
        launch { tagline.animateTo(1f, tween(400, easing = SgEaseOut)) }
        delay(1_050)
        onTimeout()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(SgColor.Brand500),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(520.dp)
                .graphicsLayer {
                    scaleX = 0.2f + 0.8f * disc.value
                    scaleY = 0.2f + 0.8f * disc.value
                    alpha = 0.18f * disc.value
                }
                .background(Color.White, CircleShape),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_logo_mark),
                    contentDescription = null,
                    tint = SgColor.BaseWhite,
                    modifier = Modifier
                        .size(30.dp)
                        .graphicsLayer {
                            val s = 0.6f + 0.4f * mark.value
                            scaleX = s
                            scaleY = s
                            rotationZ = -90f * (1f - mark.value)
                            alpha = mark.value.coerceIn(0f, 1f)
                        },
                )
                Box(Modifier.clipToBounds()) {
                    Text(
                        text = "sisaguna",
                        fontFamily = Inter,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SgColor.BaseWhite,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .graphicsLayer {
                                translationX = -size.width * (1f - word.value)
                                alpha = word.value
                            },
                    )
                }
            }
            Text(
                "Selamatkan makanan, lindungi bumi",
                style = SgTextStyle.Caption,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .graphicsLayer {
                        alpha = tagline.value
                        translationY = 8.dp.toPx() * (1f - tagline.value)
                    },
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 852)
@Composable
private fun SplashScreenPreview() {
    SisaGunaTheme {
        SplashScreen(onTimeout = {})
    }
}
