package com.qdash

import org.junit.Assert.assertEquals
import org.junit.Test

class TimePickerMappingTest {

    // Helper conversion logic from 24h to 12h (displayHour, isPm)
    private fun to12HourFormat(hour24: Int): Pair<Int, Boolean> {
        val displayHour = if (hour24 == 0) 12 else if (hour24 > 12) hour24 - 12 else hour24
        val isPm = hour24 >= 12
        return Pair(displayHour, isPm)
    }

    // Helper conversion logic from 12h + isPm to 24h
    private fun to24HourFormat(displayHour: Int, isPm: Boolean): Int {
        return if (isPm) {
            if (displayHour == 12) 12 else displayHour + 12
        } else {
            if (displayHour == 12) 0 else displayHour
        }
    }

    @Test
    fun test24HourTo12HourConversion() {
        // Midnight (00:00) -> 12 AM
        val (h0, pm0) = to12HourFormat(0)
        assertEquals(12, h0)
        assertEquals(false, pm0)

        // Noon (12:00) -> 12 PM
        val (h12, pm12) = to12HourFormat(12)
        assertEquals(12, h12)
        assertEquals(true, pm12)

        // 1 AM (01:00) -> 1 AM
        val (h1, pm1) = to12HourFormat(1)
        assertEquals(1, h1)
        assertEquals(false, pm1)

        // 1 PM (13:00) -> 1 PM
        val (h13, pm13) = to12HourFormat(13)
        assertEquals(1, h13)
        assertEquals(true, pm13)

        // 11 PM (23:00) -> 11 PM
        val (h23, pm23) = to12HourFormat(23)
        assertEquals(11, h23)
        assertEquals(true, pm23)
    }

    @Test
    fun test12HourTo24HourConversion() {
        // 12 AM -> 0
        assertEquals(0, to24HourFormat(12, false))

        // 12 PM -> 12
        assertEquals(12, to24HourFormat(12, true))

        // 1 AM -> 1
        assertEquals(1, to24HourFormat(1, false))

        // 1 PM -> 13
        assertEquals(13, to24HourFormat(1, true))

        // 11 PM -> 23
        assertEquals(23, to24HourFormat(11, true))
    }
}
