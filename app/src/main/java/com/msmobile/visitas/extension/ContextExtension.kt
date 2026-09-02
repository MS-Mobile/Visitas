package com.msmobile.visitas.extension

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.msmobile.visitas.R

fun Context.launchGoogleMaps(label: String, latitude: Double, longitude: Double) {
    val encodedLabel = Uri.encode(label)
    // Create a geo URI with the marker
    val geoUri = "geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)".toUri()
    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
    mapIntent.setPackage("com.google.android.apps.maps")

    if (mapIntent.resolveActivity(packageManager) != null) {
        startActivity(mapIntent)
    } else {
        // Fallback to browser if Google Maps isn't installed
        val fallbackUri = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude".toUri()
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

    val uberUri = "uber://?$deepLinkParams".toUri()
    val uberIntent = Intent(Intent.ACTION_VIEW, uberUri)
    uberIntent.setPackage("com.ubercab")

    if (uberIntent.resolveActivity(packageManager) != null) {
        startActivity(uberIntent)
    } else {
        // Fallback to the Uber universal web link if the app isn't installed
        val fallbackUri = "https://m.uber.com/ul/?$deepLinkParams".toUri()
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
    val intent = Intent(Intent.ACTION_DIAL, "tel:${phone.trim()}".toUri())
    startActivitySafely(intent)
}

/** Opens the default messaging app with [phone] pre-filled. */
fun Context.launchSms(phone: String) {
    val intent = Intent(Intent.ACTION_SENDTO, "smsto:${phone.trim()}".toUri())
    startActivitySafely(intent)
}

/**
 * Opens a WhatsApp chat with [phone] via a `https://wa.me/<digits>` link. The number is
 * sanitized to digits only, as wa.me requires a bare international number. If WhatsApp is
 * installed the link opens the app; otherwise it falls back to WhatsApp Web in a browser.
 */
fun Context.launchWhatsApp(phone: String) {
    val digits = phone.filter { it.isDigit() }
    val intent = Intent(Intent.ACTION_VIEW, "https://wa.me/$digits".toUri())
    startActivitySafely(intent)
}

/** Opens [url] with the default handler (browser). Scheme-less URLs get https:// prefixed. */
fun Context.launchUrl(url: String) {
    val uri = url.toUri().let { parsed ->
        if (parsed.scheme == null) "https://$url".toUri() else parsed
    }
    startActivitySafely(Intent(Intent.ACTION_VIEW, uri))
}

private fun Context.startActivitySafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No app can handle this action on the device; nothing to do.
    }
}
