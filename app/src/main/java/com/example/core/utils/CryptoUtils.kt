package com.example.core.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
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

    // --- V2 Encryption APIs (PBKDF2 + AES-256-GCM) ---

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 10000
    private const val PBKDF2_KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    fun encryptBytes(plainBytes: ByteArray, key: SecretKeySpec): EncryptedPayload {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)

        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
        val cipherText = cipher.doFinal(plainBytes)

        return EncryptedPayload(
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            ciphertext = Base64.encodeToString(cipherText, Base64.NO_WRAP)
        )
    }

    fun decryptBytes(ciphertextBase64: String, ivBase64: String, key: SecretKeySpec): ByteArray {
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val cipherText = Base64.decode(ciphertextBase64, Base64.NO_WRAP)

        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)

        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
        return cipher.doFinal(cipherText)
    }

    data class EncryptedPayload(
        val iv: String,
        val ciphertext: String
    )
}
