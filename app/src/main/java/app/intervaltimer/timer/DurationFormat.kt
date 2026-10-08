package app.intervaltimer.timer

import android.content.res.Resources
import app.intervaltimer.R
import java.util.Locale

object DurationFormat {
    /** Rounds up so the display shows 00:00 only when time is actually up. */
    fun ceilSeconds(ms: Long): Long = (ms.coerceAtLeast(0) + 999) / 1000

    /** `MM:SS`, or `H:MM:SS` from one hour up. */
    fun clock(ms: Long): String {
        val total = ceilSeconds(ms)
        val h = total / 3600
        val m = total % 3600 / 60
        val s = total % 60
        return if (h > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", m, s)
        }
    }

    /** Human readable label such as "1 小时 30 分钟". */
    fun label(res: Resources, ms: Long): String {
        val total = ceilSeconds(ms)
        val h = (total / 3600).toInt()
        val m = (total % 3600 / 60).toInt()
        val s = (total % 60).toInt()
        val parts = buildList {
            if (h > 0) add(res.getString(R.string.duration_hours, h))
            if (m > 0) add(res.getString(R.string.duration_minutes, m))
            if (s > 0 || isEmpty()) add(res.getString(R.string.duration_seconds, s))
        }
        return parts.joinToString(" ")
    }
}
