package app.intervaltimer.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DurationEntryTest {
    private fun DurationEntry.typed(vararg digits: Int) = digits.fold(this) { acc, d -> acc.type(d) }

    @Test
    fun `first digit replaces the selected field`() {
        val entry = DurationEntry(minutes = 25).select(TimeField.MINUTES).type(4)
        assertEquals(4, entry.minutes)
        assertEquals(TimeField.MINUTES, entry.selected)
        assertEquals(1, entry.typedDigits)
    }

    @Test
    fun `two digits complete a field and move to the next`() {
        val entry = DurationEntry().select(TimeField.HOURS).typed(1, 2)
        assertEquals(12, entry.hours)
        assertEquals(TimeField.MINUTES, entry.selected)
        assertEquals(0, entry.typedDigits)
    }

    @Test
    fun `a digit that cannot be a tens digit completes minutes at once`() {
        val entry = DurationEntry().select(TimeField.MINUTES).type(7)
        assertEquals(7, entry.minutes)
        assertEquals(TimeField.SECONDS, entry.selected)
    }

    @Test
    fun `hours accept a leading nine`() {
        val entry = DurationEntry().select(TimeField.HOURS).type(9)
        assertEquals(9, entry.hours)
        assertEquals(TimeField.HOURS, entry.selected)
        assertEquals(99, entry.type(9).hours)
    }

    @Test
    fun `typing a whole duration left to right`() {
        val entry = DurationEntry().select(TimeField.HOURS).typed(0, 1, 3, 0, 4, 5)
        assertEquals(DurationEntry(1, 30, 45, TimeField.SECONDS, 0), entry)
        assertEquals((3600L + 30 * 60 + 45) * 1000, entry.totalMs)
    }

    @Test
    fun `after seconds the next digit starts seconds over`() {
        val entry = DurationEntry(seconds = 45, selected = TimeField.SECONDS).typed(3, 0, 8)
        assertEquals(8, entry.seconds)
        assertEquals(TimeField.SECONDS, entry.selected)
    }

    @Test
    fun `selecting another field keeps the other values`() {
        val entry = DurationEntry(hours = 1, minutes = 5, seconds = 30).select(TimeField.SECONDS).type(1)
        assertEquals(DurationEntry(1, 5, 1, TimeField.SECONDS, 1), entry)
    }

    @Test
    fun `backspace drops the last digit and typing continues`() {
        val entry = DurationEntry(minutes = 45).select(TimeField.MINUTES).backspace()
        assertEquals(4, entry.minutes)
        assertEquals(42, entry.type(2).minutes)
        assertEquals(0, entry.backspace().minutes)
        assertEquals(0, entry.backspace().typedDigits)
    }

    @Test
    fun `clear zeroes only the selected field`() {
        val entry = DurationEntry(hours = 2, minutes = 15, seconds = 9, selected = TimeField.MINUTES).clearField()
        assertEquals(DurationEntry(2, 0, 9, TimeField.MINUTES, 0), entry)
    }

    @Test
    fun `fromMillis splits and caps`() {
        assertEquals(DurationEntry(0, 25, 0), DurationEntry.fromMillis(25 * 60_000L))
        assertEquals(DurationEntry(1, 30, 5), DurationEntry.fromMillis((5400L + 5) * 1000))
        assertEquals(DurationEntry(99, 59, 59), DurationEntry.fromMillis(Long.MAX_VALUE))
    }

    @Test
    fun `encode round trips`() {
        val entry = DurationEntry(3, 4, 5, TimeField.SECONDS, 1)
        assertEquals(entry, DurationEntry.decode(entry.encode()))
        assertNull(DurationEntry.decode("garbage"))
        assertNull(DurationEntry.decode("1,99,0,1,0"))
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
