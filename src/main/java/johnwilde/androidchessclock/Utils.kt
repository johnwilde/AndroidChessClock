package johnwilde.androidchessclock

import java.text.DecimalFormat

object Utils {
    fun formatTimeGap(millisIn: Long): String {
        var time = formatTime(millisIn, false, 0)
        return when {
            millisIn < -500 -> "-" + time
            millisIn >= 500 -> "+" + time
            else -> ""
        }
    }

    fun formatClockTime(millisIn: Long): String {
        var time = formatTime(millisIn, true, 10_000)
        // clock is <= -1 second, prepend a minus sign
        return when {
            millisIn <= -1000 -> "-" + time
            else -> time
        }
    }

    fun formatTime(millisIn: Long, speedUp: Boolean, startSpeedUpMs: Long): String {
        // formatters for displaying text in timer
        val dfOneDecimal = DecimalFormat("0.0")
        val dfOneDigit = DecimalFormat("0")
        val dfTwoDigit = DecimalFormat("00")

        // 1000 ms in 1 second
        // 60*1000 ms in 1 minute
        // 60*60*1000 ms in 1 hour

        var stringSec: String
        val stringMin: String
        val stringHr: String
        var millis = Math.abs(millisIn)

        // Parse the input (in ms) into integer hour, minute, and second
        // values
        val hours = millis / (1000 * 60 * 60)
        millis -= hours * (1000 * 60 * 60)

        val min = millis / (1000 * 60)
        millis -= min * (1000 * 60)

        val sec = millis / 1000
        millis -= sec * 1000

        // Construct string
        if (hours > 0)
            stringHr = dfOneDigit.format(hours) + ":"
        else
            stringHr = ""

        if (hours > 0)
            stringMin = dfTwoDigit.format(min) + ":"
        else if (min > 0)
            stringMin = dfOneDigit.format(min) + ":"
        else
            stringMin = ""

        stringSec = dfTwoDigit.format(sec)

        if (hours == 0L && min == 0L) {
            if (millisIn in -9_999..9_999) {
                stringSec = dfOneDigit.format(sec.toDouble() + millis.toDouble() / 1000.0)
            }
            if (speedUp) {
                // Desired behavior:
                //
                // for 0 <= millisIn < startSpeedUp
                // clock should read like: "N.N"
                // for -999 <= millisIn <= -1 (the second after passing 0.0)
                // clock should read "0"
                // for millisIn < -999 (all time less than -1 seconds)
                // clock should read like : "-N"

                // modify formatting when less than 10 seconds
                if (millisIn in 0..(startSpeedUpMs - 1))
                    stringSec = dfOneDecimal.format(sec.toDouble() + millis.toDouble() / 1000.0)
            }
        }

        return stringHr + stringMin + stringSec
    }
}
