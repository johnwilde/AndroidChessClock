package johnwilde.androidchessclock.logic

import android.content.res.Resources
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import johnwilde.androidchessclock.R
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.logic.GameStateHolder.GameState
import johnwilde.androidchessclock.main.MainViewState
import johnwilde.androidchessclock.main.Partial
import johnwilde.androidchessclock.prefs.PreferencesUtil
import johnwilde.androidchessclock.sound.Buzzer
import johnwilde.androidchessclock.sound.Click
import johnwilde.androidchessclock.sound.SoundViewState
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ClockManager @Inject constructor(
        preferencesUtil: PreferencesUtil,
        val resources: Resources,
        var stateHolder : GameStateHolder,
        @Named("white") val white: TimerLogic,
        @Named("black") val black: TimerLogic) {
    
    var playPauseSubject: BehaviorSubject<Partial<MainViewState>> = BehaviorSubject.create()
    var clickObservable : PublishSubject<SoundViewState> = PublishSubject.create()
    val spinnerObservable: Observable<Partial<MainViewState>> = Observable.merge(white.spinner, black.spinner)
    val buzzerObservable: Observable<SoundViewState> = Observable.merge(white.buzzer, black.buzzer)

    init {
        // Allows the manager to know when a clock has expired (and game is finished)
        val ignored = buzzerObservable.subscribe { a ->
            when (a) {
                is Buzzer -> {
                    if (!preferencesUtil.allowNegativeTime) {
                        setGameStateAndPublish(GameState.FINISHED)
                        val message = if (active().color == ClockView.Color.WHITE) {
                            resources.getString(R.string.white_lost)
                        } else {
                            resources.getString(R.string.black_lost)
                        }

                        playPauseSubject.onNext(MainViewState.Snackbar(
                                show = true,
                                message = message
                        ))
                        active().onTimeExpired()
                    }
                }
            }
        }
        // Allow clocks to receive time updates from each other (enables time-gap display)
        white.subscribeToClock(black)
        black.subscribeToClock(white)
        // White moves first so set as active player
        stateHolder.setActiveClock(white)
    }

    // Player button was hit
    fun moveEnd(color: ClockView.Color) : Observable<Partial<ClockViewState>> {
        var result = Observable.empty<Partial<ClockViewState>>()
        when (gameState()) {
            GameState.PAUSED,
            GameState.NOT_STARTED -> {
                if (color == active().color) {
                    // Handled in presenter
                } else {
                    // Start / resume play
                    clickObservable.onNext(Click())
                    startPlayerClock(forOtherColor(color))
                    // if NOT_STARTED neither clock is dimmed
                    forColor(color).publishInactiveState()
                    playPauseSubject.onNext(MainViewState.Snackbar(dismiss = true))
                }
            }
            GameState.PLAYING -> {
                // Switch turns
                if (color == active().color) {
                    clickObservable.onNext(Click())
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
                setGameStateAndPublish(GameState.PAUSED)
                active().pause()
            }
            GameState.PAUSED,
            GameState.NOT_STARTED -> {
                // Start / resume game
                startPlayerClock(active())
                forOtherColor(active().color).publishInactiveState() // dim other clock
                playPauseSubject.onNext(MainViewState.Snackbar(dismiss = true))
            }
            // button is disabled when in finished state, so this shouldn't be possible
            GameState.FINISHED -> {}
        }
        return Observable.empty()
    }

    // Drawer was opened
    fun pause() : Observable<Partial<MainViewState>> {
        return when(gameState()) {
            GameState.PLAYING -> playPause()
            GameState.PAUSED, GameState.NOT_STARTED, GameState.FINISHED -> Observable.empty()
        }
    }

    fun reset() {
        setGameStateAndPublish(GameState.NOT_STARTED)
        stateHolder.setActiveClock(white)
        white.reset()
        black.reset()
    }

    private fun setGameStateAndPublish(state : GameState) {
        stateHolder.setGameStateValue(state)
        val update = when (state) {
            GameState.NOT_STARTED -> MainViewState.initialState
            GameState.PLAYING -> MainViewState.PlayPauseButton(MainViewState.PlayPauseButton.State.PAUSE, true)
            GameState.PAUSED -> MainViewState.PlayPauseButton(MainViewState.PlayPauseButton.State.PLAY, true)
            GameState.FINISHED -> MainViewState.PlayPauseButton(MainViewState.PlayPauseButton.State.PLAY, false)
        }
        playPauseSubject.onNext(update)
    }

    private fun startPlayerClock(timer : TimerLogic) {
        stateHolder.setActiveClock(timer)
        if (gameState() == GameState.PAUSED) {
            active().resume()
        } else {
            active().onMoveStart()
        }
        setGameStateAndPublish(GameState.PLAYING)
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

    private fun active() : TimerLogic { return stateHolder.active!! }
    private fun gameState() : GameState { return stateHolder.gameState }
}

