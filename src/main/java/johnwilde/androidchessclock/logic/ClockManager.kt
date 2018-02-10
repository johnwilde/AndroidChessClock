package johnwilde.androidchessclock.logic

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import johnwilde.androidchessclock.clock.*
import johnwilde.androidchessclock.logic.GameStateHolder.GameState
import johnwilde.androidchessclock.main.PlayPauseState
import johnwilde.androidchessclock.main.PlayPauseViewState
import johnwilde.androidchessclock.prefs.PreferencesUtil
import johnwilde.androidchessclock.sound.Buzzer
import johnwilde.androidchessclock.sound.Click
import johnwilde.androidchessclock.sound.SoundViewState
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ClockManager @Inject constructor(
        preferencesUtil: PreferencesUtil,
        private var stateHolder : GameStateHolder,
        @Named("white") val white: TimerLogic,
        @Named("black") val black: TimerLogic) {
    
    var playPauseSubject: BehaviorSubject<PlayPauseState> = BehaviorSubject.create()
    var clickObservable : PublishSubject<SoundViewState> = PublishSubject.create()
    val spinnerObservable: Observable<PlayPauseViewState> = Observable.merge(white.spinner, black.spinner)
    val buzzerObservable: Observable<SoundViewState> = Observable.merge(white.buzzer, black.buzzer)

    init {
        // Allows the manager to know when a clock has expired (and game is finished)
        val ignored = buzzerObservable.subscribe { a ->
            when (a) {
                is Buzzer -> {
                    if (!preferencesUtil.allowNegativeTime) {
                        setGameStateAndPublish(GameState.FINISHED)
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
    fun moveEnd(color: ClockView.Color) : Observable<PartialState> {
        var result = Observable.empty<PartialState>()
        when (gameState()) {
            GameState.PAUSED,
            GameState.NOT_STARTED -> {
                if (color == active().color) {
                    // Must tap non-active color to resume/start game
                    val otherColor = forOtherColor(color).color
                    result = Observable.just<PartialState>(PromptToMove(otherColor))
                } else {
                    // Start / resume play
                    clickObservable.onNext(Click())
                    startPlayerClock(forOtherColor(color))
                    // if NOT_STARTED neither clock is
                    forColor(color).publishInactiveState()
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
    fun playPause() : Observable<PlayPauseViewState> {
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
            }
            // button is disabled when in finished state, so this shouldn't be possible
            GameState.FINISHED -> {}
        }
        return Observable.empty()
    }

    // Drawer was opened
    fun pause() : Observable<PlayPauseViewState> {
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
            GameState.NOT_STARTED -> PlayPauseState(true, false)
            GameState.PLAYING -> PlayPauseState(false, false)
            GameState.PAUSED -> PlayPauseState(true, true)
            GameState.FINISHED -> PlayPauseState(true, false, false)
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
                timeGap = TimeGapViewState(show = false),
                prompt = null)
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

    fun clockUpdates(color: ClockView.Color) : Observable<PartialState> {
        return forColor(color).clockUpdateSubject
    }

    private fun active() : TimerLogic { return stateHolder.active!! }
    private fun gameState() : GameState { return stateHolder.gameState }

}

