package app.intervaltimer.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationDigitsTest {
    private fun typed(vararg keys: String) = keys.fold(DurationDigits.Empty) { acc, key -> acc.append(key) }

    @Test
    fun `digits fill from the right`() {
        val entry = typed("1", "3", "0")
        assertEquals("000130", entry.padded)
        assertEquals(90_000L, entry.totalMs)
    }

    @Test
    fun `leading zeros are ignored`() {
        assertEquals("", typed("0", "00").digits)
        assertEquals("500", typed("0", "5", "00").digits)
    }

    @Test
    fun `entry is capped at six digits`() {
        val entry = typed("1", "2", "3", "4", "5", "6", "7", "00")
        assertEquals("123456", entry.digits)
        assertEquals((12 * 3600L + 34 * 60 + 56) * 1000, entry.totalMs)
    }

    @Test
    fun `seconds above 59 are normalised`() {
        assertEquals(99_000L, typed("9", "9").totalMs)
    }

    @Test
    fun `backspace removes the last digit`() {
        assertEquals("12", typed("1", "2", "3").backspace().digits)
        assertEquals("", DurationDigits.Empty.backspace().digits)
    }

    @Test
    fun `fromMillis round trips`() {
        assertEquals("2500", DurationDigits.fromMillis(25 * 60_000L).digits)
        assertEquals("13000", DurationDigits.fromMillis(3600_000L + 30 * 60_000L).digits)
        assertEquals("", DurationDigits.fromMillis(0).digits)
        assertEquals("995959", DurationDigits.fromMillis(Long.MAX_VALUE).digits)
    }

    @Test
    fun `clock rounds up to whole seconds`() {
        assertEquals("00:00", DurationFormat.clock(0))
        assertEquals("00:01", DurationFormat.clock(1))
        assertEquals("05:00", DurationFormat.clock(300_000))
        assertEquals("1:00:00", DurationFormat.clock(3_600_000))
        assertEquals("12:34:56", DurationFormat.clock((12 * 3600L + 34 * 60 + 56) * 1000))
    }
}
