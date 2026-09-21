package com.sisaguna.android.feature.address

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.sisaguna.android.R
import com.sisaguna.android.ui.theme.SgColor
import com.sisaguna.android.ui.theme.SgTextStyle
import kotlinx.coroutines.launch

data class SavedAddress(val label: String, val fullAddress: String)

private val mockSavedAddresses = listOf(
    SavedAddress("Rumah", "Jl. Sutera Onyx XII No.30, RT.003/RW.010, Kunciran, Kec. Pinang, Kota Tangerang, Banten 15144"),
    SavedAddress("Apartement mecca", "Komplek Business Park Kebon Jeruk Ruko AB-6, RT.1/RW.5, Meruya Utara, Kec. Kembangan, Kota Jakarta Barat, Daerah Khusus Ibukota Jakarta 11620"),
)

/** Matches Figma node 43:6960 "Select location". "Lokasi saat ini" does a real GPS fetch +
 * reverse geocode (see LocationFetcher.kt) — everything else here is UI only (no backend to
 * persist a chosen address to yet). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerSheet(
    onDismiss: () -> Unit,
    onLocationResolved: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LocationPickerContent(onLocationResolved = onLocationResolved)
    }
}

@Composable
private fun LocationPickerContent(onLocationResolved: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isFetching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun startFetch() {
        isFetching = true
        errorMessage = null
        scope.launch {
            try {
                val location = fetchCurrentLocation(context)
                val label = reverseGeocodeLabel(context, location)
                onLocationResolved(label)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Gagal mengambil lokasi. Coba lagi."
            } finally {
                isFetching = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startFetch() else errorMessage = "Izin lokasi ditolak."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 21.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 23.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(text = "Pilih Lokasi", style = SgTextStyle.TextLgSemibold, color = SgColor.Neutral800)

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SgColor.Neutral100, RoundedCornerShape(30.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_location_card),
                        contentDescription = null,
                        tint = SgColor.Neutral500,
                        modifier = Modifier.width(20.dp),
                    )
                    Text(
                        text = "Cari alamat",
                        style = SgTextStyle.TextXsRegular,
                        color = SgColor.Neutral500,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(SgColor.Neutral100, RoundedCornerShape(30.dp))
                            .clickable {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (granted) startFetch() else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                    ) {
                        if (isFetching) {
                            CircularProgressIndicator(modifier = Modifier.width(16.dp), strokeWidth = 2.dp, color = SgColor.Brand500)
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_my_location),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.width(16.dp),
                            )
                        }
                        Text(
                            text = "Lokasi saat ini",
                            style = SgTextStyle.TextXsRegular,
                            color = SgColor.Neutral800,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(SgColor.Neutral100, RoundedCornerShape(30.dp))
                            .clickable {
                                val uri = Uri.parse("geo:0,0?q=lokasi")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_map_search),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.width(16.dp),
                        )
                        Text(
                            text = "pilih di maps",
                            style = SgTextStyle.TextXsRegular,
                            color = SgColor.Neutral800,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                errorMessage?.let {
                    Text(text = it, fontSize = 11.sp, color = SgColor.RedStatus)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(SgColor.Neutral100))
        }

        Spacer(modifier = Modifier.height(17.dp))
        Text(
            text = "Alamat tersimpan",
            style = SgTextStyle.TextSmSemibold,
            color = SgColor.Neutral500,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 23.dp),
        )
        Spacer(modifier = Modifier.height(17.dp))

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp),
        ) {
            mockSavedAddresses.forEach { address ->
                SavedAddressCard(address = address, onClick = { onLocationResolved(address.label) })
            }
        }

        Spacer(modifier = Modifier.height(21.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(SgColor.Brand500, RoundedCornerShape(50.dp))
                .clickable { /* Tambah alamat baru — not built yet this session */ }
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Icon(painter = painterResource(R.drawable.ic_add_circle), contentDescription = null, tint = Color.Unspecified)
            Text(text = "Tambah alamat baru", style = SgTextStyle.TextSmSemibold, color = SgColor.Neutral50)
        }
    }
}

@Composable
private fun SavedAddressCard(address: SavedAddress, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SgColor.BaseWhite, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, SgColor.Neutral100), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_bookmark_fill),
                    contentDescription = null,
                    tint = SgColor.Neutral800,
                    modifier = Modifier.width(14.dp),
                )
                Text(
                    text = address.label,
                    style = SgTextStyle.TextXsMedium,
                    color = SgColor.Neutral800,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_more_horiz),
                contentDescription = "Opsi lainnya",
                tint = SgColor.Neutral800,
                modifier = Modifier.width(20.dp),
            )
        }
        Text(text = address.fullAddress, fontSize = 10.sp, color = SgColor.Neutral500)
    }
}
