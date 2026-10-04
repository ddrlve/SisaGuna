package com.sisaguna.android.ui.domain

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sisaguna.android.data.model.UserProfile
import com.sisaguna.android.ui.i18n.l

/**
 * Default profile avatars: a fruit on a soft pastel disc, on brand for a food-rescue app.
 * The pastels are fixed (not theme tokens) because the avatar is content, like a photo.
 */
enum class FruitAvatar(val key: String, val emoji: String, val bg: Long, val nameId: String, val nameEn: String) {
    AVOCADO("avocado", "🥑", 0xFFE2F2D5, "Alpukat", "Avocado"),
    ORANGE("orange", "🍊", 0xFFFFE6CC, "Jeruk", "Orange"),
    WATERMELON("watermelon", "🍉", 0xFFFFDDE1, "Semangka", "Watermelon"),
    STRAWBERRY("strawberry", "🍓", 0xFFFFE3EA, "Stroberi", "Strawberry"),
    BANANA("banana", "🍌", 0xFFFFF3C4, "Pisang", "Banana"),
    PINEAPPLE("pineapple", "🍍", 0xFFFFEFCB, "Nanas", "Pineapple"),
    KIWI("kiwi", "🥝", 0xFFE5F3D6, "Kiwi", "Kiwi"),
    GRAPES("grapes", "🍇", 0xFFEBDDF8, "Anggur", "Grapes");

    val label: String get() = l(nameId, nameEn)

    companion object {
        fun of(key: String?): FruitAvatar? = entries.firstOrNull { it.key == key }

        /** Stable pick from the name, so a new account gets a fruit without choosing. */
        fun defaultFor(name: String): FruitAvatar = entries[Math.floorMod(name.trim().lowercase().hashCode(), entries.size)]
    }
}

/** The fruit on its disc, emoji sized to the circle. */
@Composable
fun FruitAvatarBadge(fruit: FruitAvatar, size: Dp, modifier: Modifier = Modifier) {
    val emojiSize = with(LocalDensity.current) { (size * 0.52f).toSp() }
    Box(
        modifier.size(size).background(Color(fruit.bg), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // Material Text on purpose: an emoji must not go through translation.
        Text(fruit.emoji, style = TextStyle(fontSize = emojiSize))
    }
}

/** Photo if there is one, otherwise the chosen (or default) fruit. */
@Composable
fun UserAvatar(profile: UserProfile, size: Dp, modifier: Modifier = Modifier, ring: Color? = null) {
    val ringed = if (ring != null) modifier.border(2.dp, ring, CircleShape).padding(3.dp) else modifier
    if (profile.photoUri != null) {
        AsyncImage(
            profile.photoUri,
            contentDescription = l("Foto profil", "Profile photo"),
            contentScale = ContentScale.Crop,
            modifier = ringed.size(size).clip(CircleShape),
        )
    } else {
        FruitAvatarBadge(FruitAvatar.of(profile.avatar) ?: FruitAvatar.defaultFor(profile.name), size, ringed)
    }
}

