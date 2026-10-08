package app.intervaltimer.timer

import java.util.Locale

/**
 * Keypad entry that fills `HHMMSS` from the right, like a microwave keypad: typing 1, 3, 0 gives
 * 00:01:30. [digits] has no leading zeros and at most [MAX_DIGITS] characters. Minute and second
 * fields may exceed 59 (e.g. "90" is 90 seconds); [totalMs] normalises them.
 */
data class DurationDigits(val digits: String = "") {
    init {
        require(digits.length <= MAX_DIGITS && digits.all(Char::isDigit) && !digits.startsWith('0')) {
            "Invalid duration digits: $digits"
        }
    }

    /** Six characters, `HHMMSS`, zero padded. */
    val padded: String get() = digits.padStart(MAX_DIGITS, '0')

    val hours: Int get() = padded.substring(0, 2).toInt()
    val minutes: Int get() = padded.substring(2, 4).toInt()
    val seconds: Int get() = padded.substring(4, 6).toInt()

    val totalMs: Long get() = (hours * 3600L + minutes * 60L + seconds) * 1000L

    /** Appends a key such as "7" or "00". Leading zeros and overflow are ignored. */
    fun append(key: String): DurationDigits {
        var next = digits
        for (c in key) {
            if (!c.isDigit() || next.length >= MAX_DIGITS) break
            if (next.isEmpty() && c == '0') continue
            next += c
        }
        return copy(digits = next)
    }

    fun backspace(): DurationDigits = copy(digits = digits.dropLast(1))

    companion object {
        const val MAX_DIGITS = 6
        const val MAX_SECONDS = 99 * 3600L + 59 * 60L + 59L

        val Empty = DurationDigits()

        fun fromMillis(ms: Long): DurationDigits {
            val total = (ms / 1000).coerceIn(0, MAX_SECONDS)
            val text = String.format(Locale.ROOT, "%02d%02d%02d", total / 3600, total % 3600 / 60, total % 60)
            return DurationDigits(text.trimStart('0'))
        }
    }
}
