package johnwilde.androidchessclock.logic

import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.main.MainViewState
import johnwilde.androidchessclock.main.Partial
import timber.log.Timber
import java.util.*

class TakeBackController(val a: Timer, val o: Timer, val stateHolder: GameStateHolder) {

    class Wrapper(val t: Timer) {
        private var moves: LongArray
        private var at: Int
        init {
            moves = t.moveTimes
            at = t.moveTimes.lastIndex
            Timber.d("%s: %s", t.color, Arrays.toString(moves))
        }

        fun time(): Long {
            return moves[at]
        }

        private fun isStartOfFirstMove(): Boolean {
            return t.color == ClockView.Color.WHITE &&
                   t.moveTimes.isEmpty()
        }

        fun canMoveBack(): Boolean {
           return !isStartOfFirstMove()
        }

        fun canMoveForward(): Boolean {
            return (at != moves.lastIndex || isStartOfFirstMove())
        }

        // give player back their time for this move
        fun moveBack() {
            t.setNewTime(t.msToGo + time() - t.bonusMsPerMove())
            t.moveCounter.popMove(t)
            at--
        }
        // take away time for a move
        fun moveForward() {
            at++
            t.setNewTime(t.msToGo - time() + t.bonusMsPerMove())
            t.moveCounter.pushMove(time(), t)
        }
    }

    // Player who's turn it is when game is paused
    private var active: TakeBackController.Wrapper
    private var other: TakeBackController.Wrapper
    private var first: TakeBackController.Wrapper

    init {
        active = Wrapper(a)
        other = Wrapper(o)
        first = active
    }

    fun swap() {
        val temp = other
        other = active
        active = temp
        active.t.publishActiveState()
        other.t.publishInactiveState()
    }

    fun goBack() {
        Timber.d("take back from %s", active.t.color)
        active.moveBack()
        if (!isFirstMove()) {
            stateHolder.setActiveClock(other.t)
            swap()
        } else {
            stateHolder.setGameStateValue(GameStateHolder.GameState.NOT_STARTED)
        }
        Timber.d("%s now active", active.t.color)
    }

    fun goForward() {
        if (isFirstMove()) {
            active.moveForward()
            stateHolder.setGameStateValue(GameStateHolder.GameState.PAUSED)
        } else {
            other.moveForward()
            stateHolder.setActiveClock(other.t)
            swap()
        }
    }

    fun back(): Partial<MainViewState> {
        Timber.d("is last move: %s", isLastMove())
        Timber.d("is first move: %s", isFirstMove())
        goBack()
        Timber.d("is last move: %s", isLastMove())
        Timber.d("is first move: %s", isFirstMove())
        return MainViewState.TakeBack(!isFirstMove(), !isLastMove())
    }

    fun forward(): Partial<MainViewState> {
        Timber.d("is last move: %s", isLastMove())
        Timber.d("is first move: %s", isFirstMove())
        goForward()
        Timber.d("is last move: %s", isLastMove())
        Timber.d("is first move: %s", isFirstMove())
        return MainViewState.TakeBack(!isFirstMove(), !isLastMove())
    }

    fun isLastMove(): Boolean {
        return (active == first && !active.canMoveForward())
    }

    fun isFirstMove(): Boolean {
        return (active.t.color == ClockView.Color.WHITE && !active.canMoveBack())
    }
}