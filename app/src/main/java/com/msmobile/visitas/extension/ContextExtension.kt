package com.msmobile.visitas.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.msmobile.visitas.R

fun Context.launchGoogleMaps(label: String, latitude: Double, longitude: Double) {
    val encodedLabel = Uri.encode(label)
    // Create a geo URI with the marker
    val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
    mapIntent.setPackage("com.google.android.apps.maps")

    if (mapIntent.resolveActivity(packageManager) != null) {
        startActivity(mapIntent)
    } else {
        // Fallback to browser if Google Maps isn't installed
        val fallbackUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
        val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
        startActivity(fallbackIntent)
    }
}

fun Context.showShareIntent(shareFileUri: Uri, mime: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, shareFileUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    this.startActivity(Intent.createChooser(shareIntent, this.getString(R.string.share_backup)))
}
