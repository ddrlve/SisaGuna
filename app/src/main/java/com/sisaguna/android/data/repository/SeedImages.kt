package com.sisaguna.android.data.repository

/** Photo bundled in the app (assets/img) for the landing and promo cards. Coil loads `file:///android_asset/…` directly. */
fun seedImage(name: String) = "file:///android_asset/img/$name.jpg"
