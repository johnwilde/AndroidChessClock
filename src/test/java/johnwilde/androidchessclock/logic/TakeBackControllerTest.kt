package johnwilde.androidchessclock.logic

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.prefs.PreferencesUtil
import org.junit.Before

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class TakeBackControllerTest {
    lateinit var controller: TakeBackController
    lateinit var white: Timer
    lateinit var black: Timer
    var pref: PreferencesUtil = mock()
    var stateHolder = GameStateHolder()
    var timeSource = MockSystemTime(0)

    @Before
    fun setUp() {
        pref = mock()
        whenever(pref.timeGap).thenReturn(Observable.just(true))
        whenever(pref.getBronsteinDelayMs()).thenReturn(10_000)
        stateHolder = GameStateHolder()
        white = Bronstein(ClockView.Color.WHITE, pref, stateHolder, timeSource)
        black = Bronstein(ClockView.Color.BLACK, pref, stateHolder, timeSource)
        white.msToGo = 10
        black.msToGo = 10
    }

    @Test
    fun back1() {
        white.moveStart()
        white.msToGo = 9
        white.moveEnd()

        assertTrue(Arrays.equals(longArrayOf(1), white.moveTimes))
        black.moveStart()
        stateHolder.setActiveClock(black)
        black.msToGo = 8
        black.stop()
        assertTrue(Arrays.equals(longArrayOf(2), black.moveTimes))
        stateHolder.setGameStateValue(GameStateHolder.GameState.PAUSED)
        // black is active
        controller = TakeBackController(black, white, stateHolder)
        controller.goBack()
        assertEquals(ClockView.Color.WHITE, stateHolder.active?.color)
        assertEquals(10, black.msToGo)
        assertEquals(9, white.msToGo)
        assertTrue(Arrays.equals(longArrayOf(1), white.moveTimes))
        assertTrue(Arrays.equals(longArrayOf(), black.moveTimes))
    }

    @Test
    fun back2() {
        white.moveStart()
        white.msToGo = 9
        white.stop()
        stateHolder.setActiveClock(white)
        stateHolder.setGameStateValue(GameStateHolder.GameState.PAUSED)
        // white is active
        controller = TakeBackController(white, black, stateHolder)
        assertTrue(controller.isLastMove())
        assertFalse(controller.isFirstMove())
        controller.goBack()
        assertEquals(ClockView.Color.WHITE, stateHolder.active?.color)
        assertEquals(10, white.msToGo)
        assertEquals(10, black.msToGo)
        assertTrue(Arrays.equals(longArrayOf(), white.moveTimes))
        assertTrue(Arrays.equals(longArrayOf(), black.moveTimes))
        assertEquals(GameStateHolder.GameState.NOT_STARTED, stateHolder.gameState)
        assertFalse(controller.isLastMove())
        assertTrue(controller.isFirstMove())

        controller.goForward()
        assertEquals(ClockView.Color.WHITE, stateHolder.active?.color)
        assertEquals(9, white.msToGo)
        assertEquals(10, black.msToGo)
        assertTrue(controller.isLastMove())
        assertFalse(controller.isFirstMove())
    }

    @Test
    fun back3() {
        white.moveStart()
        white.msToGo = 9
        white.moveEnd()
        black.moveStart()
        black.msToGo = 8
        black.moveEnd()
        white.moveStart()
        white.msToGo = 7
        white.stop()
        stateHolder.setActiveClock(white)
        stateHolder.setGameStateValue(GameStateHolder.GameState.PAUSED)
        // white is active
        controller = TakeBackController(white, black, stateHolder)
        assertTrue(controller.isLastMove())
        controller.goBack()  //black turn
        assertFalse(controller.isLastMove())
        assertEquals(ClockView.Color.BLACK, stateHolder.active?.color)
        assertEquals(8, black.msToGo)
        assertEquals(9, white.msToGo)
        controller.goBack() // white end move 1
        controller.goBack() // white game start
        assertTrue(controller.isFirstMove())
        assertEquals(10, black.msToGo)
        assertEquals(10, white.msToGo)
    }

    @Test
    fun backDelayFisher() {
        whenever(pref.getFischerDelayMs()).thenReturn(10)
        white = Fischer(ClockView.Color.WHITE, pref, stateHolder, timeSource)
        black = Fischer(ClockView.Color.BLACK, pref, stateHolder, timeSource)
        white.msToGo = 10
        black.msToGo = 10
        white.moveStart()
        white.msToGo = 9
        white.moveEnd()

        assertTrue(Arrays.equals(longArrayOf(11), white.moveTimes))
        black.moveStart()
        stateHolder.setActiveClock(black)
        black.msToGo = 8
        black.stop()
        assertTrue(Arrays.equals(longArrayOf(12), black.moveTimes))
        stateHolder.setGameStateValue(GameStateHolder.GameState.PAUSED)
        // black is active
        controller = TakeBackController(black, white, stateHolder)
        controller.goBack()
        assertEquals(ClockView.Color.WHITE, stateHolder.active?.color)
        assertEquals(10, black.msToGo)
        assertEquals(9, white.msToGo)
        assertTrue(Arrays.equals(longArrayOf(11), white.moveTimes))
        assertTrue(Arrays.equals(longArrayOf(), black.moveTimes))
    }

}