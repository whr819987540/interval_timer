package app.intervaltimer.timer

enum class TimeField(val max: Int) {
    HOURS(99),
    MINUTES(59),
    SECONDS(59),
}

/**
 * Duration edited one field at a time: the user taps 时, 分 or 秒 to select it, then types the
 * value on the keypad.
 *
 * The first digit after selecting a field replaces its value; a second digit appends to it. A
 * field is complete after two digits, or after one digit that cannot be a tens digit (7 in
 * minutes can only mean 07), and the selection then moves on to the next field.
 */
data class DurationEntry(
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0,
    val selected: TimeField = TimeField.MINUTES,
    /** Digits typed into [selected] since it was selected; 0 means the next digit replaces it. */
    val typedDigits: Int = 0,
) {
    init {
        require(hours in 0..TimeField.HOURS.max && minutes in 0..TimeField.MINUTES.max && seconds in 0..TimeField.SECONDS.max) {
            "Out of range: $hours:$minutes:$seconds"
        }
    }

    val totalMs: Long get() = (hours * 3600L + minutes * 60L + seconds) * 1000L

    fun valueOf(field: TimeField): Int = when (field) {
        TimeField.HOURS -> hours
        TimeField.MINUTES -> minutes
        TimeField.SECONDS -> seconds
    }

    fun select(field: TimeField): DurationEntry = copy(selected = field, typedDigits = 0)

    fun type(digit: Int): DurationEntry {
        require(digit in 0..9)
        val value = if (typedDigits == 0) digit else valueOf(selected) * 10 + digit
        val typed = typedDigits + 1
        val updated = withValue(selected, value.coerceAtMost(selected.max)).copy(typedDigits = typed)
        val complete = typed >= 2 || value * 10 > selected.max
        return if (complete) updated.advance() else updated
    }

    /** Drops the last digit of the selected field; typing then continues from what is left. */
    fun backspace(): DurationEntry {
        val value = valueOf(selected) / 10
        return withValue(selected, value).copy(typedDigits = if (value == 0) 0 else 1)
    }

    fun clearField(): DurationEntry = withValue(selected, 0).copy(typedDigits = 0)

    /** Compact form for SavedStateHandle. */
    fun encode(): String = "$hours,$minutes,$seconds,${selected.ordinal},$typedDigits"

    private fun withValue(field: TimeField, value: Int): DurationEntry = when (field) {
        TimeField.HOURS -> copy(hours = value)
        TimeField.MINUTES -> copy(minutes = value)
        TimeField.SECONDS -> copy(seconds = value)
    }

    private fun advance(): DurationEntry = when (selected) {
        TimeField.HOURS -> select(TimeField.MINUTES)
        TimeField.MINUTES -> select(TimeField.SECONDS)
        // Last field: stay, and let the next digit start it over.
        TimeField.SECONDS -> copy(typedDigits = 0)
    }

    companion object {
        private const val MAX_SECONDS = 99 * 3600L + 59 * 60L + 59L

        fun fromMillis(ms: Long, selected: TimeField = TimeField.MINUTES): DurationEntry {
            val total = (ms / 1000).coerceIn(0, MAX_SECONDS)
            return DurationEntry(
                hours = (total / 3600).toInt(),
                minutes = (total % 3600 / 60).toInt(),
                seconds = (total % 60).toInt(),
                selected = selected,
            )
        }

        fun decode(text: String): DurationEntry? = runCatching {
            val (h, m, s, field, typed) = text.split(',').map(String::toInt)
            DurationEntry(h, m, s, TimeField.entries[field], typed.coerceIn(0, 1))
        }.getOrNull()
    }
}
