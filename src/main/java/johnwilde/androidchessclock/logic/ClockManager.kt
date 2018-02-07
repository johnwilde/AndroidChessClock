package johnwilde.androidchessclock.logic

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import johnwilde.androidchessclock.clock.*
import johnwilde.androidchessclock.main.PlayPauseState
import johnwilde.androidchessclock.main.PlayPauseViewState
import johnwilde.androidchessclock.prefs.PreferencesUtil
import johnwilde.androidchessclock.sound.Buzzer
import johnwilde.androidchessclock.sound.Click
import johnwilde.androidchessclock.sound.SoundViewState
import timber.log.Timber

class ClockManager(val preferencesUtil: PreferencesUtil, val timeSource: TimeSource) {
    val white = TimerLogic(this, ClockView.Color.WHITE, preferencesUtil, timeSource)
    val black = TimerLogic(this, ClockView.Color.BLACK, preferencesUtil, timeSource)
    var active = white

    enum class GameState {NOT_STARTED, PAUSED, PLAYING, FINISHED}
    var gameState : GameState = GameState.NOT_STARTED

    var playPauseSubject: BehaviorSubject<PlayPauseState> = BehaviorSubject.create()
    var clickObservable : PublishSubject<SoundViewState> = PublishSubject.create()
    var buzzerObservable = Observable.merge(white.buzzer, black.buzzer)
    var spinnerObservable = Observable.merge(white.spinner, black.spinner)

    init {
        // Allows the manager to know when a clock has expired (and game is finished)
        val ignored = buzzerObservable.subscribe { a ->
            when (a) {
                is Buzzer ->
                    if (!preferencesUtil.allowNegativeTime) setGameStateAndPublish(GameState.FINISHED)
            }
        }
        // Allow clocks to receive time updates from each other (enables time-gap display)
        white.subscribeToOtherClock()
        black.subscribeToOtherClock()
    }

    // Player button was hit
    fun moveEnd(color: ClockView.Color) : Observable<ClockViewState> {
        var result = Observable.empty<ClockViewState>()
        when (gameState) {
            GameState.PAUSED,
            GameState.NOT_STARTED -> {
                if (color == active.color) {
                    // Must tap non-active color to resume/start game
                    val otherColor = forOtherColor(color).color
                    result = Observable.just<ClockViewState>(PromptToMove(otherColor))
                } else {
                    // Start / resume play
                    clickObservable.onNext(Click())
                    startPlayerClock(forOtherColor(color))
                    forColor(color).publishMoveEnd()
                }
            }
            GameState.PLAYING -> {
                // Switch turns
                if (color == active.color) {
                    clickObservable.onNext(Click())
                    startPlayerClock(forOtherColor(color))
                    forColor(color).onMoveEnd()
                } else {
                    forColor(color).publishMoveEnd()
                }
            }
            GameState.FINISHED -> { } // nothing
        }
        return result
    }

    // Play/Pause button was hit
    fun playPause() : Observable<PlayPauseViewState> {
        when(gameState) {
            GameState.PLAYING -> {
                // Pause game
                setGameStateAndPublish(GameState.PAUSED)
                active.pause()
            }
            GameState.PAUSED,
            GameState.NOT_STARTED -> {
                // Start / resume game
                startPlayerClock(active)
                forOtherColor(active.color).publishMoveEnd() // dim other clock
            }
            // button is disabled when in finished state, so this shouldn't be possible
            GameState.FINISHED -> {}
        }
        return Observable.empty()
    }

    // Drawer was opened
    fun pause() : Observable<PlayPauseViewState> {
        return when(gameState) {
            GameState.PLAYING -> playPause()
            GameState.PAUSED, GameState.NOT_STARTED, GameState.FINISHED -> Observable.empty()
        }
    }

    fun reset() {
        setGameStateAndPublish(GameState.NOT_STARTED)
        active = white
        white.reset()
        black.reset()
    }

    private fun setGameStateAndPublish(state : GameState) {
        gameState = state
        val update = when (state) {
            GameState.NOT_STARTED -> PlayPauseState(true, false)
            GameState.PLAYING -> PlayPauseState(false, false)
            GameState.PAUSED -> PlayPauseState(true, true)
            GameState.FINISHED -> PlayPauseState(true, false, false)
        }
        playPauseSubject.onNext(update)
    }

    private fun startPlayerClock(timer : TimerLogic) {
        Timber.d("Active player is %s", timer.color)
        active = timer
        if (gameState == GameState.PAUSED) {
            active.resume()
        } else {
            active.onMoveStart()
        }
        setGameStateAndPublish(GameState.PLAYING)
    }

    fun initialState(color: ClockView.Color) : ButtonViewState {
        return forColor(color).initialState()
    }

    fun forColor(color: ClockView.Color): TimerLogic {
        return when (color) {
            ClockView.Color.WHITE -> white
            ClockView.Color.BLACK -> black
        }
    }

    fun forOtherColor(color: ClockView.Color): TimerLogic {
        return when (color) {
            ClockView.Color.WHITE -> black
            ClockView.Color.BLACK -> white
        }
    }

    fun clockUpdates(color: ClockView.Color) : Observable<ClockViewState> {
        return forColor(color).clockUpdateSubject
    }
}

