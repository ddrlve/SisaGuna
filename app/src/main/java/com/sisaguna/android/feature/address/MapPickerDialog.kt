package com.sisaguna.android.feature.address

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.sisaguna.android.ui.components.SgButton
import com.sisaguna.android.ui.components.pressable
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgRadius
import com.sisaguna.android.ui.theme.SgSpacing
import com.sisaguna.android.ui.theme.SgTextStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.CustomZoomButtonsController

/**
 * Full-screen OpenStreetMap picker. The pin is fixed at the centre and the map moves under it
 * (the Gojek/Grab pattern) — easier to aim with a thumb than dragging a marker. The pin lifts
 * while the map moves and drops with a small bounce when it settles; then the centre is
 * reverse-geocoded (debounced 450ms) into the bottom card.
 */
@Composable
fun MapPickerDialog(
    initialLatitude: Double,
    initialLongitude: Double,
    onPicked: (Place) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var place by remember { mutableStateOf<Place?>(null) }
    var resolving by remember { mutableStateOf(true) }
    var locating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val pinLift = remember { Animatable(0f) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var geocodeJob by remember { mutableStateOf<Job?>(null) }

    fun settle(center: GeoPoint) {
        geocodeJob?.cancel()
        geocodeJob = scope.launch {
            pinLift.animateTo(-14f, tween(120))
            resolving = true
            delay(450)
            place = reverseGeocode(context, center.latitude, center.longitude)
            resolving = false
            pinLift.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    fun goToMyLocation() {
        locating = true
        error = null
        scope.launch {
            runCatching { fetchCurrentLocation(context) }
                .onSuccess { loc ->
                    mapView?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude), 17.5, 600L)
                }
                .onFailure { error = it.message }
            locating = false
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) goToMyLocation() else error = "Izin lokasi ditolak. Geser peta untuk memilih manual."
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(SgColor.Page)) {
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().userAgentValue = ctx.packageName
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                        controller.setZoom(16.5)
                        controller.setCenter(GeoPoint(initialLatitude, initialLongitude))
                        addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                settle(mapCenter as GeoPoint)
                                return false
                            }

                            override fun onZoom(event: ZoomEvent?): Boolean {
                                settle(mapCenter as GeoPoint)
                                return false
                            }
                        })
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            DisposableEffect(Unit) {
                onDispose { mapView?.onDetach() }
            }
            LaunchedEffect(Unit) { settle(GeoPoint(initialLatitude, initialLongitude)) }

            // Centre pin; its tip sits on the exact centre.
            Box(Modifier.align(Alignment.Center).offset(y = (-20).dp)) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 20.dp)
                        .size(10.dp, 4.dp)
                        .graphicsLayer { alpha = 0.3f + pinLift.value / 80f }
                        .background(SgColor.Ink, CircleShape),
                )
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = SgColor.RedStatus,
                    modifier = Modifier.size(44.dp).graphicsLayer { translationY = pinLift.value * density },
                )
            }

            // Frosted top bar.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(SgSpacing.Lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SgSpacing.Md),
            ) {
                RoundButton(onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali", tint = SgColor.Ink) }
                Text(
                    "Geser peta untuk memilih titik",
                    style = SgTextStyle.Label,
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(SgRadius.Pill))
                        .background(SgColor.BaseWhite.copy(alpha = 0.92f), RoundedCornerShape(SgRadius.Pill))
                        .padding(horizontal = SgSpacing.Lg, vertical = 10.dp),
                )
            }

            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding(),
                horizontalAlignment = Alignment.End,
            ) {
                RoundButton(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (granted) goToMyLocation() else permission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    modifier = Modifier.padding(SgSpacing.Lg),
                ) {
                    if (locating) CircularProgressIndicator(strokeWidth = 2.dp, color = SgColor.Brand500, modifier = Modifier.size(20.dp))
                    else Icon(Icons.Rounded.MyLocation, contentDescription = "Lokasi saya", tint = SgColor.Brand600)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(SgColor.BaseWhite)
                        .padding(SgSpacing.Gutter),
                    verticalArrangement = Arrangement.spacedBy(SgSpacing.Sm),
                ) {
                    Text("Lokasi dipilih", style = SgTextStyle.Caption)
                    if (resolving && place == null) {
                        Text("Mencari alamat…", style = SgTextStyle.Label)
                    } else {
                        Text(place?.shortLabel.orEmpty(), style = SgTextStyle.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(place?.fullAddress.orEmpty(), style = SgTextStyle.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    error?.let { Text(it, style = SgTextStyle.Caption, color = SgColor.RedStatus) }
                    SgButton(
                        text = "Pilih lokasi ini",
                        onClick = { place?.let(onPicked) },
                        enabled = place != null && !resolving,
                        modifier = Modifier.fillMaxWidth().padding(top = SgSpacing.Sm),
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(SgColor.BaseWhite)
            .pressable(onClick, pressedScale = 0.9f),
        contentAlignment = Alignment.Center,
    ) { content() }
}
