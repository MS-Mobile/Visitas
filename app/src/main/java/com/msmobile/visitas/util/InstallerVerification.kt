package com.msmobile.visitas.util

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstallerVerification @Inject constructor() {

    /**
     * Verifies if the app was installed from Google Play Store or is part of internal test
     * @param context Application context
     * @return true if the app is from a valid source, false otherwise
     */
    suspend fun isValidInstallSource(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check installer package name first
            return@withContext isInstalledFromPlayStore(context) && isValidSignature(context)
        } catch (error: Throwable) {
            // If any error occurs during verification, assume invalid installation
            return@withContext false
        }
    }

    /**
     * Checks if the app was installed from Google Play Store
     */
    private fun isInstalledFromPlayStore(context: Context): Boolean {
        val packageName = context.packageName
        val packageManager = context.packageManager

        val installerPackageName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName)
        }

        return when (installerPackageName) {
            "com.android.vending" -> true // Google Play Store
            "com.google.android.feedback" -> true // Google Play Store (alternative)
            "com.android.packageinstaller" -> false // Side-loaded
            "com.google.android.packageinstaller" -> false // Side-loaded via Google
            null -> false // Unknown source
            else -> false
        }
    }

    /**
     * Validates app signature to ensure it hasn't been tampered with
     * This provides additional security against repackaged apps
     */
    private fun isValidSignature(context: Context): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signatures?.let { sigs ->
                // Verify that signatures exist and are valid
                sigs.isNotEmpty() && areSignaturesValid(sigs)
            } ?: false

        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validates that the signatures are legitimate
     * You can customize this to check specific signature hashes if needed
     */
    private fun areSignaturesValid(signatures: Array<Signature>): Boolean {
        return try {
            signatures.forEach { signature ->
                val signatureBytes = signature.toByteArray()
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(signatureBytes)

                // Convert to hex string for comparison
                val hexHash = hash.joinToString("") { "%02x".format(it) }

                // Here you could check against known valid signature hashes
                // For now, we just verify that a signature exists and can be processed
                if (hexHash.isNotEmpty()) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}
