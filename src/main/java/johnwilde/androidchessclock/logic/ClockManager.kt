package johnwilde.androidchessclock.logic

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import johnwilde.androidchessclock.ActivityBindingModule
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockView.Color.*
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
    var stateHolder: GameStateHolder,
    @Named("white") private var white: Timer,
    @Named("black") private var black: Timer
) {

    private lateinit var gameOver: Disposable
    var timeIsNegative: Boolean = false // At least one clock has gone negative
    val whiteDelegate = TimerDelegate(WHITE)
    val blackDelgate = TimerDelegate(BLACK)
    lateinit var takeBackController: TakeBackController

    // Wrapper around the timer instances, which go away if time control
    // type is changed.  Clients should use the subjects exposed by this
    // class to receive correct state updates across time control changes.
    inner class TimerDelegate(val color: ClockView.Color) {
        var clock = BehaviorSubject.create<Partial<ClockViewState>>()
        var main = BehaviorSubject.create<Partial<MainViewState>>()
        var disposables = CompositeDisposable()

        fun subscribe() {
            val realClock = timerForColor(color)
            disposables.add(realClock.clockSubject.subscribe(clock::onNext, clock::onError))
            disposables.add(realClock.mainSubject.subscribe(main::onNext, main::onError))
            realClock.clockSubject.value?.let {
                clock.onNext(it)
            }
        }

        fun unsubscribe() {
            disposables.clear()
        }
    }

    init {
        initializeGame()
    }

    private fun initializeGame() {
        whiteDelegate.subscribe()
        blackDelgate.subscribe()
        white.initialize()
        black.initialize()
        setGameState(GameState.NOT_STARTED)
        stateHolder.setActiveClock(white)
        gameOver = gameOverSubscription()
    }

    fun reset() {
        white.destroy()
        black.destroy()
        gameOver.dispose()
        whiteDelegate.unsubscribe()
        blackDelgate.unsubscribe()
        // Create new timers
        val timeSource = ActivityBindingModule.providesTimeSource()
        white = ActivityBindingModule.providesWhite(preferencesUtil, stateHolder, timeSource)
        black = ActivityBindingModule.providesBlack(preferencesUtil, stateHolder, timeSource)
        initializeGame()
    }

    // Logic for "ending" the game
    private fun gameOverSubscription(): Disposable {
        timeIsNegative = false
        return stateHolder.timeSubject
                .filter { u -> u.ms <= 0 }
                .filter { stateHolder.gameState.isUnderway() }
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
                if (color == BLACK) {
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
        when (gameState()) {
            GameState.PLAYING, GameState.NEGATIVE -> {
                // Pause game
                setGameState(GameState.PAUSED)
                active().stop()
                takeBackController = TakeBackController(active(), forOtherColor(active().color), stateHolder)
            }
            GameState.PAUSED -> {
                startPlayerClock(active())
            }
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

    private fun setGameState(state: GameState) {
        stateHolder.setGameStateValue(state)
    }

    private fun startPlayerClock(timer: Timer) {
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
            WHITE -> white
            BLACK -> black
        }
    }

    fun forColor(color: ClockView.Color): TimerDelegate {
        return when (color) {
            WHITE -> whiteDelegate
            BLACK -> blackDelgate
        }
    }

    private fun forOtherColor(color: ClockView.Color): Timer {
        return when (color) {
            WHITE -> black
            BLACK -> white
        }
    }

    fun active(): Timer { return stateHolder.active!! }
    private fun gameState(): GameState { return stateHolder.gameState }
}
