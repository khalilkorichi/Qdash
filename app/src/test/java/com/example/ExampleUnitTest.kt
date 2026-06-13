package com.example

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
}
