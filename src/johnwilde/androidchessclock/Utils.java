package johnwilde.androidchessclock;

import java.text.DecimalFormat;

 class Utils {
    public static String formatTime(long millisIn) {
        // formatters for displaying text in timer
        DecimalFormat dfOneDecimal = new DecimalFormat("0.0");
        DecimalFormat dfOneDigit = new DecimalFormat("0");
        DecimalFormat dfTwoDigit = new DecimalFormat("00");
   
        // 1000 ms in 1 second
        // 60*1000 ms in 1 minute
        // 60*60*1000 ms in 1 hour

        String stringSec, stringMin, stringHr;
        long millis = Math.abs(millisIn);

        // Parse the input (in ms) into integer hour, minute, and second
        // values
        long hours = millis / (1000 * 60 * 60);
        millis -= hours * (1000 * 60 * 60);

        long min = millis / (1000 * 60);
        millis -= min * (1000 * 60);

        long sec = millis / 1000;
        millis -= sec * 1000;

        // Construct string
        if (hours > 0)
            stringHr = dfOneDigit.format(hours) + ":";
        else
            stringHr = "";

        if (hours > 0)
            stringMin = dfTwoDigit.format(min) + ":";
        else if (min > 0)
            stringMin = dfOneDigit.format(min) + ":";
        else
            stringMin = "";

        stringSec = dfTwoDigit.format(sec);

        if (hours == 0 && min == 0) {
            // Desired behavior:
            //
            // for 0 <= millisIn < 10000 (between 0 and 10 seconds)
            // clock should read like: "N.N"
            // for -999 <= millisIn <= -1 (the second after passing 0.0)
            // clock should read "0"
            // for millisIn < -999 (all time less than -1 seconds)
            // clock should read like : "-N"

            // modify formatting when less than 10 seconds
            if (sec < 10 && millisIn >= 0) // between 0 and 9 seconds
                stringSec = dfOneDecimal.format((double) sec
                        + (double) millis / 1000.0);
            else if (sec < 10 && millisIn < 0) // between -1 and -9
                stringSec = dfOneDigit.format((double) sec
                        + (double) millis / 1000.0);
        }

        // clock is <= -1 second, prepend a minus sign
        if (millisIn <= -1000) {
            return "-" + stringHr + stringMin + stringSec;
        }

        return stringHr + stringMin + stringSec;

    }
}
