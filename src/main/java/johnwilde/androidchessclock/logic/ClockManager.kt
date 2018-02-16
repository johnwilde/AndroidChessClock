package johnwilde.androidchessclock.logic

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import johnwilde.androidchessclock.ActivityBindingModule
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.logic.GameStateHolder.GameState
import johnwilde.androidchessclock.main.MainViewState
import johnwilde.androidchessclock.main.Partial
import johnwilde.androidchessclock.prefs.PreferencesUtil
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

// Manages the overall state of the game (started, finished, paused, etc).
// Contains an instance of each player's clock and is responsible for calling
// methods to synchronize the start/stop of each clock as players alternate
// turns.
@Singleton
class ClockManager @Inject constructor(
        val preferencesUtil: PreferencesUtil,
        var stateHolder : GameStateHolder,
        private @Named("white") var white: Timer,
        private @Named("black") var black: Timer) {

    private lateinit var gameOver : Disposable
    var timeIsNegative: Boolean = false // At least one clock has gone negative
    lateinit var whiteDelegate: TimerDelegate
    lateinit var blackDelgate: TimerDelegate

    // Wrapper around the timer instances, which go away if time control
    // type is changed.  Clients should use the subjects exposed by this
    // class to receive correct state updates across time control changes.
    inner class TimerDelegate(val color : ClockView.Color) {
        var clock = BehaviorSubject.create<Partial<ClockViewState>>()
        var main = BehaviorSubject.create<Partial<MainViewState>>()

        var disposables = CompositeDisposable()
        init {
            subscribe()
        }
        fun subscribe() {
            val realClock = timerForColor(color)
            disposables.add(realClock.clockSubject.subscribe(
                    clock::onNext, clock::onError))
            disposables.add(realClock.mainSubject.subscribe(
                    main::onNext, main::onError))
        }
        fun unsubscribe() {
            disposables.clear()
        }
    }

    init {
        initializeTimers()
    }

    private fun initializeTimers() {
        gameOver = gameOverSubscription()
        white.initialize()
        black.initialize()
        if (!::whiteDelegate.isInitialized) {
            whiteDelegate = TimerDelegate(ClockView.Color.WHITE)
            blackDelgate = TimerDelegate(ClockView.Color.BLACK)
        }
        whiteDelegate.unsubscribe()
        whiteDelegate.subscribe()
        blackDelgate.unsubscribe()
        blackDelgate.subscribe()
    }

    // Logic for "ending" the game
    private fun gameOverSubscription() : Disposable {
        timeIsNegative = false
        return stateHolder.timeSubject
                .filter{ u ->  u.ms <= 0 }
                .take(1)
                .subscribe { _ ->
                    if (preferencesUtil.allowNegativeTime) {
                        timeIsNegative = true
                        setGameState(GameState.NEGATIVE)
                    } else {
                        if (gameState() == GameState.PLAYING) {
                            active().stop()
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
                // Can un-stop when non-active player hits their button
                if (color != active().color) {
                    // Resume play
                    startPlayerClock(forOtherColor(color))
                }
            }
            GameState.PLAYING, GameState.NEGATIVE -> {
                // Switch turns (ignore taps on non-active button)
                if (color == active().color) {
                    startPlayerClock(forOtherColor(color))
                    timerForColor(color).moveEnd()
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
                active().stop()
            }
            GameState.PAUSED ->
                startPlayerClock(active())
            GameState.NOT_STARTED -> {
                // Start / start game
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
        white.destroy()
        black.destroy()

        // Create new timers
        val timeSource = ActivityBindingModule.providesTimeSource()
        white = ActivityBindingModule.providesWhite(preferencesUtil, stateHolder, timeSource)
        black = ActivityBindingModule.providesBlack(preferencesUtil, stateHolder, timeSource)

        initializeTimers()
    }

    private fun setGameState(state : GameState) {
        stateHolder.setGameStateValue(state)
    }

    private fun startPlayerClock(timer : Timer) {
        stateHolder.setActiveClock(timer)
        if (gameState() == GameState.PAUSED) {
            active().start()
        } else {
            active().moveStart()
        }
        if (timeIsNegative) {
            setGameState(GameState.NEGATIVE)
        } else {
            setGameState(GameState.PLAYING)
        }
    }

    // This returns an actual timer instance, which may change if user
    // changes time control.  For that reason, this reference should
    // not be saved.
    fun timerForColor(color: ClockView.Color): Timer {
        return when (color) {
            ClockView.Color.WHITE -> white
            ClockView.Color.BLACK -> black
        }
    }

    fun forColor(color: ClockView.Color): TimerDelegate {
        return when (color) {
            ClockView.Color.WHITE -> whiteDelegate
            ClockView.Color.BLACK -> blackDelgate
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

