package com.example.core.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12

    // Secure static passphrase to derive a 256-bit AES key.
    // This allows portable backup decryption across different user devices.
    private const val PASSPHRASE = "FinTrack-DZ-Secure-Backup-Passphrase-2026-dz"

    private val secretKeySpec: SecretKeySpec by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(PASSPHRASE.toByteArray(Charsets.UTF_8))
        SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    /**
     * Encrypts the input plain text using AES-256 GCM.
     * Prepend the 12-byte IV to the encrypted bytes and returns the Base64-encoded string.
     */
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Combine IV and CipherText
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    /**
     * Decrypts the Base64-encoded GCM-encrypted string.
     * Extracts the IV from the first 12 bytes and decrypts the remaining payload.
     */
    fun decrypt(encryptedBase64: String): String {
        val combined = Base64.decode(encryptedBase64, Base64.DEFAULT)
        if (combined.size < IV_LENGTH_BYTES) {
            throw IllegalArgumentException("الملف المجلد غير صالح أو مشوه!")
        }

        val iv = ByteArray(IV_LENGTH_BYTES)
        val cipherText = ByteArray(combined.size - IV_LENGTH_BYTES)

        System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES)
        System.arraycopy(combined, IV_LENGTH_BYTES, cipherText, 0, cipherText.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec)
        val decryptedBytes = cipher.doFinal(cipherText)

        return String(decryptedBytes, Charsets.UTF_8)
    }
}
