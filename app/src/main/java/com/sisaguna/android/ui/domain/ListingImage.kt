package com.sisaguna.android.ui.domain

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.sisaguna.android.R
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.ui.theme.SgColor

/**
 * A listing's photo. Uploads carry a content:// or https URL (loaded with Coil); seed data has
 * none, so human food falls back to the Figma food photo and farm/compost tiers to their
 * category illustration on the tier tint.
 */
@Composable
fun ListingImage(
    imageUrl: String,
    tier: ListingTier,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            imageUrl.isNotBlank() -> AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(SgColor.Hairline),
            )
            tier == ListingTier.HUMAN -> Image(
                painter = painterResource(R.drawable.listing_ayam_olie),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (tier == ListingTier.ANIMAL_FEED) SgColor.Farm else SgColor.Compost),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(if (tier == ListingTier.ANIMAL_FEED) R.drawable.category_animal else R.drawable.category_compost),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(0.55f),
                )
            }
        }
    }
}
