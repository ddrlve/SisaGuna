package com.sisaguna.android.ui.domain

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.sisaguna.android.ui.theme.SgColor
import kotlin.random.Random

/** Decorative QR-looking grid with the three finder squares — not a scannable code. */
@Composable
fun FakeQr(seed: Int, modifier: Modifier = Modifier) {
    val cells = 25
    val bits = remember(seed) { Random(seed).let { r -> List(cells * cells) { r.nextFloat() > 0.52f } } }
    Canvas(modifier.fillMaxSize()) {
        val cell = size.minDimension / cells
        fun finder(cx: Int, cy: Int) {
            drawRect(SgColor.Ink, Offset(cx * cell, cy * cell), Size(cell * 7, cell * 7))
            drawRect(Color.White, Offset((cx + 1) * cell, (cy + 1) * cell), Size(cell * 5, cell * 5))
            drawRect(SgColor.Ink, Offset((cx + 2) * cell, (cy + 2) * cell), Size(cell * 3, cell * 3))
        }
        for (y in 0 until cells) for (x in 0 until cells) {
            val inFinder = (x < 8 && y < 8) || (x >= cells - 8 && y < 8) || (x < 8 && y >= cells - 8)
            if (!inFinder && bits[y * cells + x]) drawRect(SgColor.Ink, Offset(x * cell, y * cell), Size(cell, cell))
        }
        finder(0, 0); finder(cells - 7, 0); finder(0, cells - 7)
    }
}

