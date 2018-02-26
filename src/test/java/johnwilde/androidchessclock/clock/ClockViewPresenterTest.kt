package johnwilde.androidchessclock.clock

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import johnwilde.androidchessclock.logic.ClockManager
import johnwilde.androidchessclock.logic.GameStateHolder
import johnwilde.androidchessclock.logic.TestSchedulerRule
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test

class ClockViewPresenterTest {
    lateinit var robot : ClockViewRobot
    lateinit var presenter : ClockViewPresenter
    var initialState = ClockViewState(
            button = ClockViewState.Button(enabled = true),
            time = ClockViewState.Time(msToGo = 0),
            timeGap = ClockViewState.TimeGap(show = false),
            prompt = ClockViewState.Snackbar(dismiss = true),
            moveCount = ClockViewState.MoveCount(
                    message = ClockViewState.MoveCount.Message.NONE,
                    count = 0)
    )
    var clockManager : ClockManager = mock()

    companion object {
        @ClassRule
        @JvmField
        val testSchedulerRule = TestSchedulerRule()
    }

    @Before
    fun beforeTest() {
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