package com.qdash.core.data.local

import com.qdash.core.data.ALL_MIGRATIONS
import com.qdash.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationCoverageTest {

    @Test
    fun testAllMigrationsAreRegistered() {
        val currentDbVersion = findAppDatabaseVersion()
        
        val oldestSupportedVersion = 3
        val expectedMigrationCount = currentDbVersion - oldestSupportedVersion
        val registeredTransitions = ALL_MIGRATIONS.map { it.startVersion to it.endVersion }.toSet()
        
        for (version in oldestSupportedVersion until currentDbVersion) {
            val nextVersion = version + 1
            val hasMigration = registeredTransitions.contains(version to nextVersion)
            assertTrue(
                "Missing migration from version $version to $nextVersion in ALL_MIGRATIONS array!",
                hasMigration
            )
        }
        
        assertEquals(
            "The registered migrations count does not match expected version count!",
            expectedMigrationCount,
            ALL_MIGRATIONS.size
        )
    }

    private fun findAppDatabaseVersion(): Int {
        val paths = listOf(
            "src/main/java/com/qdash/data/local/AppDatabase.kt",
            "app/src/main/java/com/qdash/data/local/AppDatabase.kt",
            "../app/src/main/java/com/qdash/data/local/AppDatabase.kt"
        )
        for (path in paths) {
            val file = java.io.File(path)
            if (file.exists()) {
                val content = file.readText()
                val match = Regex("""version\s*=\s*(\d+)""").find(content)
                if (match != null) {
                    return match.groupValues[1].toInt()
                }
            }
        }
        throw IllegalStateException("Could not find AppDatabase.kt file to parse version")
    }
}
