package com.sisaguna.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Corner radii read off the Home frame in Figma: search bar/chips/pills use full pill radius
// (30dp+), category tiles 20dp, listing cards 25dp.
val SgShapes = Shapes(
    extraSmall = RoundedCornerShape(13.dp),
    small = RoundedCornerShape(17.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(25.dp),
    extraLarge = RoundedCornerShape(30.dp),
)
