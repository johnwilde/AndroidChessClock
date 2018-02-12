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

@Singleton
class ClockManager @Inject constructor(
        val preferencesUtil: PreferencesUtil,
        var stateHolder : GameStateHolder,
        @Named("white") val white: TimerLogic,
        @Named("black") val black: TimerLogic) {

    lateinit var gameOver : Disposable
    val spinnerObservable: Observable<Partial<MainViewState>> = Observable.merge(white.spinner, black.spinner)

    init {
        gameOver = gameOverSubscription()
        // Allow clocks to receive time updates from each other (enables time-gap display)
        white.subscribeToClock(black)
        black.subscribeToClock(white)
        // White moves first so set as active player
        stateHolder.setActiveClock(white)
    }

    private fun gameOverSubscription() : Disposable {
        return Observable.merge<Long>(white.msToGoUpdateSubject, black.msToGoUpdateSubject)
                .filter{ it <= 0 }
                .take(1)
                .subscribe { _ ->
                    if (preferencesUtil.allowNegativeTime) {
                        setGameState(GameState.NEGATIVE)
                    } else {
                        setGameState(GameState.FINISHED)
                        active().onTimeExpired()
                    }
                }
    }

    // Player button was hit
    fun moveEnd(color: ClockView.Color) : Observable<Partial<ClockViewState>> {
        val result = Observable.empty<Partial<ClockViewState>>()
        when (gameState()) {
            GameState.PAUSED,
            GameState.NOT_STARTED -> {
                if (color == active().color) {
                    // Handled in presenter
                } else {
                    // Start / resume play
                    startPlayerClock(forOtherColor(color))
                    // if NOT_STARTED neither clock is dimmed
                    forColor(color).publishInactiveState()
                }
            }
            GameState.PLAYING -> {
                // Switch turns
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
            GameState.PLAYING -> {
                // Pause game
                setGameState(GameState.PAUSED)
                active().pause()
            }
            GameState.PAUSED,
            GameState.NOT_STARTED -> {
                // Start / resume game
                startPlayerClock(active())
                forOtherColor(active().color).publishInactiveState() // dim other clock
            }
            // button is disabled when in finished state, so this shouldn't be possible
            GameState.FINISHED -> {}
        }
        return Observable.empty()
    }

    // Drawer was opened
    fun pause() : Observable<Partial<MainViewState>> {
        return if (gameState().isUnderway()) {
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
        stateHolder.setActiveClock(white)
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
        setGameState(GameState.PLAYING)
    }

    fun initialState(color: ClockView.Color) : ClockViewState {
        return ClockViewState(
                button = forColor(color).initialState(),
                timeGap = ClockViewState.TimeGap(show = false),
                prompt = ClockViewState.Snackbar(dismiss = true))
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

    fun clockUpdates(color: ClockView.Color) : Observable<Partial<ClockViewState>> {
        return forColor(color).clockUpdateSubject
    }

    fun active() : TimerLogic { return stateHolder.active!! }
    private fun gameState() : GameState { return stateHolder.gameState }
}

