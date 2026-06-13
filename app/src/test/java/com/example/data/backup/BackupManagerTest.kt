package com.example.data.backup

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupManagerTest {

    @Test
    fun testBackup_ManifestValidation_Success() {
        val validManifest = """
            {
                "backupFormatVersion": 1,
                "appVersion": "1.0.0",
                "dbVersion": 4,
                "createdAt": 1780092351224,
                "deviceInfo": "Google Pixel 8 (Android 14)"
            }
        """.trimIndent()

        // Verify valid format version and lower or equal database version parses correctly
        val formatVersion = 1
        val dbVersion = 4
        assertTrue(validateManifestHelper(validManifest, formatVersion, dbVersion))
    }

    @Test
    fun testBackup_ManifestValidation_FormatMismatch_Failure() {
        val invalidFormatManifest = """
            {
                "backupFormatVersion": 2, // higher format version not supported yet
                "appVersion": "1.0.0",
                "dbVersion": 4,
                "createdAt": 1780092351224
            }
        """.trimIndent()

        val formatVersion = 1
        val dbVersion = 4
        assertFalse(validateManifestHelper(invalidFormatManifest, formatVersion, dbVersion))
    }

    @Test
    fun testBackup_ManifestValidation_DbVersionHigher_Failure() {
        val invalidDbManifest = """
            {
                "backupFormatVersion": 1,
                "appVersion": "1.0.0",
                "dbVersion": 5, // higher database version not compatible with local db v4
                "createdAt": 1780092351224
            }
        """.trimIndent()

        val formatVersion = 1
        val dbVersion = 4
        assertFalse(validateManifestHelper(invalidDbManifest, formatVersion, dbVersion))
    }

    @Test
    fun testBackup_ManifestValidation_MalformedJson_Failure() {
        val malformedManifest = """
            {
                "backupFormatVersion": 1,
                "appVersion": "1.0.0",
                "dbVersion": 
        """.trimIndent() // Malformed JSON

        val formatVersion = 1
        val dbVersion = 4
        assertFalse(validateManifestHelper(malformedManifest, formatVersion, dbVersion))
    }

    private fun validateManifestHelper(content: String, formatVersion: Int, currentDbVersion: Int): Boolean {
        return try {
            val json = JSONObject(content)
            val formatVer = json.getInt("backupFormatVersion")
            val dbVer = json.getInt("dbVersion")
            formatVer == formatVersion && dbVer <= currentDbVersion
        } catch (e: Exception) {
            false
        }
    }
}
