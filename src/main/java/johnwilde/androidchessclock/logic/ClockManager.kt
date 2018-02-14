package johnwilde.androidchessclock.logic

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import johnwilde.androidchessclock.ActivityBindingModule
import johnwilde.androidchessclock.DaggerAppComponent
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.logic.GameStateHolder.GameState
import johnwilde.androidchessclock.main.MainViewState
import johnwilde.androidchessclock.prefs.PreferencesUtil
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
        @Named("white") var white: Timer,
        @Named("black") var black: Timer) {

    var gameOver : Disposable
    var timeIsNegative: Boolean = false // At least one clock has gone negative

    init {
        gameOver = gameOverSubscription()
        // Allow clocks to receive time updates from each other (enables time-gap display)
        white.initialize(black)
        black.initialize(white)
//        var whiteClock = BehaviorSubject.create<MainViewState>()
//        whiteClock.subscribe(white.mainSubject)
    }

    // Logic for "ending" the game
    private fun gameOverSubscription() : Disposable {
        timeIsNegative = false
        return Observable.merge<Long>(white.timeSubject, black.timeSubject)
                .filter{ it <= 0 }
                .take(1)
                .subscribe { _ ->
                    if (preferencesUtil.allowNegativeTime) {
                        timeIsNegative = true
                        setGameState(GameState.NEGATIVE)
                    } else {
                        if (gameState() == GameState.PLAYING) {
                            active().pause()
                        }
                        setGameState(GameState.FINISHED)
                    }
                }
    }

    // Player button was hit
    fun clockButtonTap(color: ClockView.Color) {
        when (gameState()) {
            GameState.NOT_STARTED -> {
                // Only black should start game
                if (color == ClockView.Color.BLACK) {
                    startPlayerClock(white)
                    // if NOT_STARTED neither clock is dimmed
                    black.publishInactiveState()
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
                    forColor(color).moveEnd()
                }
            }
            GameState.FINISHED -> { } // nothing
        }
    }

    // Play/Pause button was hit
    fun playPause() {
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
    }

    // Drawer was opened
    fun pause() {
        if (gameState().clockMoving()) {
            playPause()
        }
    }

    fun reset() {
        setGameState(GameState.NOT_STARTED)
        stateHolder.removeActiveClock()
        gameOver.dispose()

        // Create new timers
        val timeSource = ActivityBindingModule.providesTimeSource()
        white = ActivityBindingModule.providesWhite(preferencesUtil, stateHolder, timeSource)
        black = ActivityBindingModule.providesBlack(preferencesUtil, stateHolder, timeSource)
        white.initialize(black)
        black.initialize(white)


        // listen for end of game
        gameOver = gameOverSubscription()
    }

    private fun setGameState(state : GameState) {
        stateHolder.setGameStateValue(state)
    }

    private fun startPlayerClock(timer : Timer) {
        stateHolder.setActiveClock(timer)
        if (gameState() == GameState.PAUSED) {
            active().resume()
        } else {
            active().moveStart()
        }
        if (timeIsNegative) {
            setGameState(GameState.NEGATIVE)
        } else {
            setGameState(GameState.PLAYING)
        }
    }

    fun forColor(color: ClockView.Color): Timer {
        return when (color) {
            ClockView.Color.WHITE -> white
            ClockView.Color.BLACK -> black
        }
    }

    private fun forOtherColor(color: ClockView.Color): Timer {
        return when (color) {
            ClockView.Color.WHITE -> black
            ClockView.Color.BLACK -> white
        }
    }

    fun active() : Timer { return stateHolder.active!! }
    private fun gameState() : GameState { return stateHolder.gameState }
}

