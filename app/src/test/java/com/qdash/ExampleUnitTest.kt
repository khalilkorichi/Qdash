package com.qdash

import org.junit.Test
import java.io.File

class ExampleUnitTest {
    @Test
    fun dumpDb() {
        var dbFile = File("kdach_database.db")
        if (!dbFile.exists()) {
            dbFile = File("../kdach_database.db")
        }
        if (!dbFile.exists()) {
            println("DB file does not exist!")
            return
        }
        
        val bytes = dbFile.readBytes()
        val byteRun = mutableListOf<Byte>()
        for (b in bytes) {
            val u = b.toInt() and 0xFF
            val isPrintable = u in 32..126 || u == 9 || u == 10 || u == 13 || u in 128..255
            if (isPrintable) {
                byteRun.add(b)
            } else {
                if (byteRun.size > 2) {
                    try {
                        val s = String(byteRun.toByteArray(), Charsets.UTF_8).trim()
                        if (s.contains("شخصي") || s.contains("راتب") || s.contains("EXPENSE") || s.contains("INCOME") || s.contains("work") || s.contains("person")) {
                            println("Found: $s")
                        }
                    } catch (e: Exception) {}
                }
                byteRun.clear()
            }
        }
    }

    @Test
    fun encryptKey() {
        val newKey = "YOUR_GEMINI_API_KEY_HERE"
        
        val passphrase = "FinTrack-DZ-Secure-Backup-Passphrase-2026-dz"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(passphrase.toByteArray(Charsets.UTF_8))
        val secretKeySpec = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")

        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        java.security.SecureRandom().nextBytes(iv)
        val parameterSpec = javax.crypto.spec.GCMParameterSpec(128, iv)

        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec)
        val cipherText = cipher.doFinal(newKey.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

        val encrypted = java.util.Base64.getEncoder().encodeToString(combined)
        println("ENCRYPTED_KEY_START")
        println(encrypted)
        println("ENCRYPTED_KEY_END")
    }
}
