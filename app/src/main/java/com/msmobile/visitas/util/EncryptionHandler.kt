package com.msmobile.visitas.util

import android.util.Log
import com.msmobile.visitas.BuildConfig
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionHandler @Inject constructor() {

    @Throws(IOException::class)
    fun writeEncrypted(input: InputStream, output: OutputStream) {
        try {
            val cipher = createEncryptionCipher()

            // Read input data with buffer for large files
            val inputData = input.use {
                it.readBytes().also { data ->
                    Log.d(TAG, "Original data size: ${data.size} bytes")
                }
            }

            output.use { outputStream ->
                try {
                    // Write IV first
                    outputStream.write(IV_BYTES)

                    // Encrypt and write data
                    val encryptedData = cipher.doFinal(inputData)
                    Log.d(TAG, "Encrypted data size: ${encryptedData.size} bytes")
                    outputStream.write(encryptedData)
                    outputStream.flush()
                } catch (e: Exception) {
                    Log.e(TAG, "Encryption failed", e)
                    throw IOException("Failed to encrypt data: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Encryption process failed", e)
            throw IOException("Encryption failed: ${e.message}", e)
        }
    }

    @Throws(IOException::class)
    fun readEncrypted(input: InputStream, output: OutputStream) {
        try {
            // Read all data
            val allData = input.use { it.readBytes() }

            // Validate minimum size (IV + at least one block)
            if (allData.size <= IV_SIZE) {
                throw IOException("Input data too short: ${allData.size} bytes")
            }

            // Extract IV and encrypted data
            val iv = allData.copyOfRange(0, IV_SIZE)
            val encryptedData = allData.copyOfRange(IV_SIZE, allData.size)

            Log.d(TAG, "Reading: IV size=${iv.size}, encrypted size=${encryptedData.size}")

            // Validate encrypted data size
            if (encryptedData.size % BLOCK_SIZE != 0) {
                Log.e(TAG, "Invalid encrypted data size: ${encryptedData.size}")
                throw IOException("Invalid encrypted data size")
            }

            // Create cipher with extracted IV
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            }

            output.use { outputStream ->
                try {
                    val decryptedData = cipher.doFinal(encryptedData)
                    Log.d(TAG, "Decrypted size: ${decryptedData.size}")
                    outputStream.write(decryptedData)
                    outputStream.flush()
                } catch (e: Exception) {
                    Log.e(TAG, "Decryption failed", e)
                    throw IOException("Failed to decrypt data: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Decryption process failed", e)
            throw IOException("Decryption failed: ${e.message}", e)
        }
    }

    private fun createEncryptionCipher(): Cipher {
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(IV_BYTES))
        }
    }

    companion object {
        private const val TAG = "EncryptionHandler"
        private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private val PASSPHRASE = BuildConfig.ENCRYPTION_PASSPHRASE
        private const val IV_SIZE = 16
        private const val BLOCK_SIZE = 16

        // Derive key from passphrase using SHA-256
        private val KEY_BYTES = MessageDigest.getInstance("SHA-256")
            .digest(PASSPHRASE.toByteArray())
            .copyOf(16) // Take first 16 bytes for AES-128

        // Generate IV from passphrase + salt
        private val IV_BYTES = MessageDigest.getInstance("SHA-256")
            .digest((PASSPHRASE + "iv_salt").toByteArray())
            .copyOf(16)

        private val secretKey = SecretKeySpec(KEY_BYTES, "AES")
    }
}