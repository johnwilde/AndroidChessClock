package johnwilde.androidchessclock.logic

import android.os.SystemClock

interface TimeSource {
    fun currentTimeMillis() : Long
}

class SystemTime : TimeSource {
    override fun currentTimeMillis(): Long {
        return SystemClock.uptimeMillis()
    }
}

class MockSystemTime(var currentTime : Long) : TimeSource {
    override fun currentTimeMillis(): Long {
        return currentTime
    }
}
