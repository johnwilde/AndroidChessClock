package johnwilde.androidchessclock.logic

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import johnwilde.androidchessclock.clock.*
import johnwilde.androidchessclock.main.Partial
import johnwilde.androidchessclock.prefs.PreferencesUtil
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import java.util.concurrent.TimeUnit

class TimerLogicTest {
    var stateHolder = GameStateHolder()
    var preferencesUtil: PreferencesUtil = mock()
    var timeSource = MockSystemTime(0)

    lateinit var whiteClock: Timer

    companion object {
        @ClassRule
        @JvmField
        val testSchedulerRule = TestSchedulerRule()
    }

    @Before
    fun beforeTest() {
        preferencesUtil = mock()
        stateHolder = GameStateHolder()
        timeSource = MockSystemTime(0)
        whenever(preferencesUtil.timeGap).thenReturn(Observable.just(true))
        whenever(preferencesUtil.showTimeGap).thenReturn(true)
        whenever(preferencesUtil.initialDurationSeconds).thenReturn(10)
        whenever(preferencesUtil.getBronsteinDelayMs()).thenReturn(0)
        whenever(preferencesUtil.getFischerDelayMs()).thenReturn(0)
        whenever(preferencesUtil.timeControlType).thenReturn(PreferencesUtil.TimeControlType.BASIC)
        whiteClock = Timer(ClockView.Color.WHITE, preferencesUtil, stateHolder, timeSource)
        stateHolder.setActiveClock(whiteClock)
    }

    @Test
    fun timeGap() {
        val blackClock = Timer(ClockView.Color.BLACK, preferencesUtil, stateHolder, timeSource)
        blackClock.initialize(whiteClock)
        val blackObserver = blackClock.clockSubject.test()
        val expectedBlackValues = mutableListOf<Partial<ClockViewState>>()

        // simulate move start
        whiteClock.moveStart()  // send time gap on move start
        expectedBlackValues.add(ClockViewState.TimeGap(10_000 - 10_000))
        blackObserver.assertValueSequence(expectedBlackValues)

        // send it again at interval=0
        testSchedulerRule.testScheduler.triggerActions()
        expectedBlackValues.add(ClockViewState.TimeGap(10_000 - 10_000))
        blackObserver.assertValueSequence(expectedBlackValues)

        advanceTimeBy(100)
        expectedBlackValues.add(ClockViewState.TimeGap(10_000 - 9_900))
        blackObserver.assertValueSequence(expectedBlackValues)

        advanceTimeBy(100)
        expectedBlackValues.add(ClockViewState.TimeGap(10_000 - 9_800))
        blackObserver.assertValueSequence(expectedBlackValues)

        blackClock.reset()
        expectedBlackValues.add(ClockViewState.Button(true, 10_000, ""))
        // Turn off time gap on reset
        expectedBlackValues.add(ClockViewState.TimeGap(show = false))
        blackObserver.assertValueSequence(expectedBlackValues)
    }

    @Test
    fun moveStartPauseAndFinish() {
        val clockTestObserver = whiteClock.clockSubject.test()
        val expectedValues = mutableListOf<Partial<ClockViewState>>()

        // simulate move start
        whiteClock.moveStart()

        expectedValues.add(ClockViewState.TimeGap(show = false)) // hides time gap
        expectedValues.add(ClockViewState.Button(true, 10000, "1"))

        // trigger the first time update
        testSchedulerRule.testScheduler.triggerActions()
        clockTestObserver.assertValueSequence(expectedValues)

        // move time forward while playing
        advanceTimeBy(100)

        expectedValues.add(ClockViewState.Button(true, 9900, "1"))
        clockTestObserver.assertValueSequence(expectedValues)

        // pause playing
        whiteClock.pause()
        assertEquals(100, whiteClock.moveTimes.last())

        // move time forward while paused
        advanceTimeBy(100)
        clockTestObserver.assertValueCount(expectedValues.size) // no new updates while paused

        // un-pause
        whiteClock.resume()
        testSchedulerRule.testScheduler.triggerActions()
        expectedValues.add(ClockViewState.Button(true, 9900, "1"))
        clockTestObserver.assertValueSequence(expectedValues)

        // move time forward while playing
        advanceTimeBy(100)
        expectedValues.add(ClockViewState.Button(true, 9800, "1"))
        clockTestObserver.assertValueSequence(expectedValues)

        // end move
        whiteClock.moveEnd()
        expectedValues.add(ClockViewState.Button(false, 9800, ""))
        clockTestObserver.assertValueSequence(expectedValues)

        // move time forward after move ends
        advanceTimeBy(100)
        clockTestObserver.assertValueCount(expectedValues.size) // no new updates after move end

        // next move
        whiteClock.moveStart()
        testSchedulerRule.testScheduler.triggerActions() // trigger the first time update

        expectedValues.add(ClockViewState.TimeGap(show = false)) // hides time gap
        expectedValues.add(ClockViewState.Button(true, 9800, "2"))
        clockTestObserver.assertValueSequence(expectedValues)
    }

    private fun advanceTimeBy(millis : Long) {
        timeSource.currentTime += millis
        testSchedulerRule.testScheduler.advanceTimeBy(millis, TimeUnit.MILLISECONDS)
    }
}