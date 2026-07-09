package com.msmobile.visitas.extension

import android.content.ActivityNotFoundException
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

/**
 * Requests an Uber ride to ([latitude], [longitude]) with [label] as the dropoff nickname.
 * Pickup defaults to the rider's current location (resolved by Uber). Opens the Uber app via
 * a `uber://` deep link when installed; otherwise falls back to the `m.uber.com` universal
 * link in a browser.
 */
fun Context.launchUber(label: String, latitude: Double, longitude: Double) {
    val encodedLabel = Uri.encode(label)
    val deepLinkParams =
        "action=setPickup&pickup=my_location" +
            "&dropoff[latitude]=$latitude&dropoff[longitude]=$longitude&dropoff[nickname]=$encodedLabel"

    val uberUri = Uri.parse("uber://?$deepLinkParams")
    val uberIntent = Intent(Intent.ACTION_VIEW, uberUri)
    uberIntent.setPackage("com.ubercab")

    if (uberIntent.resolveActivity(packageManager) != null) {
        startActivity(uberIntent)
    } else {
        // Fallback to the Uber universal web link if the app isn't installed
        val fallbackUri = Uri.parse("https://m.uber.com/ul/?$deepLinkParams")
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

/** Opens the dialer with [phone] pre-filled. ACTION_DIAL needs no permission. */
fun Context.launchDialer(phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}"))
    startActivitySafely(intent)
}

/** Opens the default messaging app with [phone] pre-filled. */
fun Context.launchSms(phone: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${phone.trim()}"))
    startActivitySafely(intent)
}

/**
 * Opens a WhatsApp chat with [phone] via a `https://wa.me/<digits>` link. The number is
 * sanitized to digits only, as wa.me requires a bare international number. If WhatsApp is
 * installed the link opens the app; otherwise it falls back to WhatsApp Web in a browser.
 */
fun Context.launchWhatsApp(phone: String) {
    val digits = phone.filter { it.isDigit() }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))
    startActivitySafely(intent)
}

private fun Context.startActivitySafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No app can handle this action on the device; nothing to do.
    }
}
