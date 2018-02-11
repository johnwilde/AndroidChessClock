package johnwilde.androidchessclock.clock

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import johnwilde.androidchessclock.logic.ClockManager
import johnwilde.androidchessclock.logic.GameStateHolder
import johnwilde.androidchessclock.logic.TestSchedulerRule
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test

class ClockViewPresenterTest {
    lateinit var robot : ClockViewRobot
    lateinit var presenter : ClockViewPresenter
    val initialState = ClockViewState(
            button =  ClockViewState.Button(true, 10_000, ""),
            timeGap = ClockViewState.TimeGap(show = false))
    var clockManager : ClockManager = mock()

    companion object {
        @ClassRule
        @JvmField
        val testSchedulerRule = TestSchedulerRule()
    }

    @Before
    fun beforeTest() {
        whenever(clockManager.initialState(any())).thenReturn(initialState)
        whenever(clockManager.clockUpdates(any())).thenReturn(Observable.empty())
        presenter = ClockViewPresenter(ClockView.Color.WHITE, clockManager)
        robot = ClockViewRobot(presenter)
    }

    @Test
    fun startPlayOnButtonClick() {
        var mockStateHolder : GameStateHolder = mock()
        whenever(mockStateHolder.gameState).thenReturn(GameStateHolder.GameState.NOT_STARTED)
        whenever(clockManager.stateHolder).thenReturn(mockStateHolder)
        robot.fireClickIntent()
        verify(clockManager).playPause()
    }
}