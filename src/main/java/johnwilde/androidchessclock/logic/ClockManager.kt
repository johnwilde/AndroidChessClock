package johnwilde.androidchessclock.logic

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.logic.GameStateHolder.GameState
import johnwilde.androidchessclock.main.MainViewState
import johnwilde.androidchessclock.main.Partial
import johnwilde.androidchessclock.prefs.PreferencesUtil
import johnwilde.androidchessclock.sound.Buzzer
import johnwilde.androidchessclock.sound.Click
import johnwilde.androidchessclock.sound.SoundViewState
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

// Manages the overall state of the game (started, finished, paused, etc).
// Contains an instance of each player's clock and is responsible for calling
// methods to synchronize the start/pause of each clock as players alternate
// turns.
@Singleton
class ClockManager @Inject constructor(
        val preferencesUtil: PreferencesUtil,
        var stateHolder : GameStateHolder,
        @Named("white") val white: TimerLogic,
        @Named("black") val black: TimerLogic) {

    var gameOver : Disposable
    var timeIsNegative: Boolean = false // At least one clock has gone negative

    init {
        gameOver = gameOverSubscription()
        // Allow clocks to receive time updates from each other (enables time-gap display)
        white.subscribeToClock(black)
        black.subscribeToClock(white)
    }

    // Logic for "ending" the game
    private fun gameOverSubscription() : Disposable {
        timeIsNegative = false
        return Observable.merge<Long>(white.msToGoUpdateSubject, black.msToGoUpdateSubject)
                .filter{ it <= 0 }
                .take(1)
                .subscribe { _ ->
                    if (preferencesUtil.allowNegativeTime) {
                        timeIsNegative = true
                        setGameState(GameState.NEGATIVE)
                    } else {
                        setGameState(GameState.FINISHED)
                        active().onTimeExpired()
                    }
                }
    }

    // Player button was hit
    fun clockButtonTap(color: ClockView.Color) : Observable<Partial<ClockViewState>> {
        val result = Observable.empty<Partial<ClockViewState>>()
        when (gameState()) {
            GameState.NOT_STARTED -> {
                // Only black should start game
                if (color == ClockView.Color.BLACK) {
                    startPlayerClock(white)
                    // if NOT_STARTED neither clock is dimmed
                    forColor(color).publishInactiveState()
                }
            }
            GameState.PAUSED -> {
                // Can un-pause when non-active player hits their button
                if (color != active().color) {
                    // Resume play
                    startPlayerClock(forOtherColor(color))
                }
            }
            GameState.PLAYING, GameState.NEGATIVE -> {
                // Switch turns (ignore taps on non-active button)
                if (color == active().color) {
                    startPlayerClock(forOtherColor(color))
                    forColor(color).onMoveEnd()
                }
            }
            GameState.FINISHED -> { } // nothing
        }
        return result
    }


    // Play/Pause button was hit
    fun playPause() : Observable<Partial<MainViewState>> {
        when(gameState()) {
            GameState.PLAYING, GameState.NEGATIVE -> {
                // Pause game
                setGameState(GameState.PAUSED)
                active().pause()
            }
            GameState.PAUSED ->
                startPlayerClock(active())
            GameState.NOT_STARTED -> {
                // Start / resume game
                startPlayerClock(white)
                forOtherColor(active().color).publishInactiveState() // dim other clock
            }
            // button is disabled when in finished state, so this shouldn't be possible
            GameState.FINISHED -> {}
        }
        return Observable.empty()
    }

    // Drawer was opened
    fun pause() : Observable<Partial<MainViewState>> {
        return if (gameState().clockMoving()) {
            playPause()
        } else {
            Observable.empty<Partial<MainViewState>>()
        }
    }

    fun reset() {
        setGameState(GameState.NOT_STARTED)
        gameOver.dispose()
        white.reset()
        black.reset()
        gameOver = gameOverSubscription()
    }

    private fun setGameState(state : GameState) {
        stateHolder.setGameStateValue(state)
    }

    private fun startPlayerClock(timer : TimerLogic) {
        stateHolder.setActiveClock(timer)
        if (gameState() == GameState.PAUSED) {
            active().resume()
        } else {
            active().onMoveStart()
        }
        if (timeIsNegative) {
            setGameState(GameState.NEGATIVE)
        } else {
            setGameState(GameState.PLAYING)
        }
    }

    fun forColor(color: ClockView.Color): TimerLogic {
        return when (color) {
            ClockView.Color.WHITE -> white
            ClockView.Color.BLACK -> black
        }
    }

    private fun forOtherColor(color: ClockView.Color): TimerLogic {
        return when (color) {
            ClockView.Color.WHITE -> black
            ClockView.Color.BLACK -> white
        }
    }

    fun active() : TimerLogic { return stateHolder.active!! }
    private fun gameState() : GameState { return stateHolder.gameState }
}

