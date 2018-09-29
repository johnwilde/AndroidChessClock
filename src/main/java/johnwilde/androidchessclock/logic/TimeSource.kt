package johnwilde.androidchessclock.logic

import android.os.SystemClock
import javax.inject.Singleton

interface TimeSource {
    fun currentTimeMillis(): Long
}

@Singleton
class SystemTime : TimeSource {
    override fun currentTimeMillis(): Long {
        return SystemClock.uptimeMillis()
    }
}

class MockSystemTime(var currentTime: Long) : TimeSource {
    override fun currentTimeMillis(): Long {
        return currentTime
    }
}
