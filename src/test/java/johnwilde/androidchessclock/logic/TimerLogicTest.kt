package johnwilde.androidchessclock.logic

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import johnwilde.androidchessclock.clock.ButtonViewState
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.clock.TimeGapViewState
import johnwilde.androidchessclock.prefs.PreferencesUtil
import org.junit.Assert.assertEquals
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
    fun moveStartPauseAndFinish() {
        whenever(preferencesUtil.getBronsteinDelayMs()).thenReturn(0)
        whenever(preferencesUtil.getFischerDelayMs()).thenReturn(0)
        whenever(preferencesUtil.timeControlType).thenReturn(PreferencesUtil.TimeControlType.BASIC)

        val clockTestObserver = timerLogic.clockUpdateSubject.test()
        val expectedValues = mutableListOf<ClockViewState>()

        // simulate move start
        timerLogic.onMoveStart()

        expectedValues.add(TimeGapViewState(0)) // hides time gap
        expectedValues.add(ButtonViewState(true, 10000, "1"))

        // trigger the first time update
        testSchedulerRule.testScheduler.triggerActions()
        clockTestObserver.assertValueSequence(expectedValues)

        // move time forward while playing
        advanceTimeBy(100)

        expectedValues.add(ButtonViewState(true, 9900, "1"))
        clockTestObserver.assertValueSequence(expectedValues)

        // pause playing
        timerLogic.pause()
        assertEquals(100, timerLogic.moveTimes.last())

        // move time forward while paused
        advanceTimeBy(100)
        clockTestObserver.assertValueCount(expectedValues.size) // no new updates while paused

        // un-pause
        timerLogic.resume()
        testSchedulerRule.testScheduler.triggerActions()
        expectedValues.add(ButtonViewState(true, 9900, "1"))
        clockTestObserver.assertValueSequence(expectedValues)

        // move time forward while playing
        advanceTimeBy(100)
        expectedValues.add(ButtonViewState(true, 9800, "1"))
        clockTestObserver.assertValueSequence(expectedValues)

        // end move
        timerLogic.onMoveEnd()
        expectedValues.add(ButtonViewState(false, 9800, ""))
        clockTestObserver.assertValueSequence(expectedValues)

        // move time forward after move ends
        advanceTimeBy(100)
        clockTestObserver.assertValueCount(expectedValues.size) // no new updates after move end

        // next move
        timerLogic.onMoveStart()
        testSchedulerRule.testScheduler.triggerActions() // trigger the first time update

        expectedValues.add(TimeGapViewState(0)) // hides time gap
        expectedValues.add(ButtonViewState(true, 9800, "2"))
        clockTestObserver.assertValueSequence(expectedValues)
    }

    private fun advanceTimeBy(millis : Long) {
        timeSource.currentTime += millis
        testSchedulerRule.testScheduler.advanceTimeBy(millis, TimeUnit.MILLISECONDS)
    }
}