package johnwilde.androidchessclock.logic

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import johnwilde.androidchessclock.clock.ButtonViewState
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.clock.TimeGapViewState
import johnwilde.androidchessclock.prefs.PreferencesUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import java.util.concurrent.TimeUnit

class TimerLogicTest {
   val manager : ClockManager = mock()
   val color = ClockView.Color.WHITE
   val preferencesUtil: PreferencesUtil = mock()
   val timeSource = MockSystemTime(0)

    lateinit var timerLogic: TimerLogic

    companion object {
        @ClassRule
        @JvmField
        val testSchedulerRule = TestSchedulerRule()
    }

    @Before
    fun beforeTest() {
        whenever(preferencesUtil.initialDurationSeconds).thenReturn(10)
        timerLogic = TimerLogic(manager, color, preferencesUtil, timeSource)
        testSchedulerRule.testScheduler.triggerActions()
    }

    @Test
    fun moveStart() {
        whenever(preferencesUtil.getBronsteinDelayMs()).thenReturn(0)
        whenever(preferencesUtil.getFischerDelayMs()).thenReturn(0)
        val o = timerLogic.clockUpdateSubject.test()
        timerLogic.onMoveStart()


        val timeGap = TimeGapViewState(0)
        val t0 = ButtonViewState(true, 10000, "1")

        testSchedulerRule.testScheduler.triggerActions()
        o.assertValues(timeGap, t0)

        advanceTimeBy(100)

        val t1 = ButtonViewState(true, 9900, "1")
        o.assertValueAt(2) { it == t1 }
    }

    private fun advanceTimeBy(millis : Long) {
        timeSource.currentTime += millis
        testSchedulerRule.testScheduler.advanceTimeBy(millis, TimeUnit.MILLISECONDS)
    }
}