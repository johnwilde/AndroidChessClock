package johnwilde.androidchessclock.logic

import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.logic.GameStateHolder.GameState.*
import timber.log.Timber
import javax.inject.Singleton

@Singleton
class GameStateHolder {
    enum class GameState {
        NOT_STARTED, PAUSED, PLAYING, NEGATIVE, FINISHED;

        fun isUnderway(): Boolean {
            return when (this) {
                PAUSED, PLAYING, NEGATIVE -> true
                else -> false
            }
        }

        fun clockMoving(): Boolean {
            return when (this) {
                PLAYING, NEGATIVE -> true
                else -> false
            }
        }
    }

    var gameState : GameState = NOT_STARTED
    val gameStateSubject: PublishSubject<GameState> = PublishSubject.create()

    // The active player (or null before game starts)
    var active : Timer? = null
    val activePlayerSubject: PublishSubject<ClockView.Color> = PublishSubject.create()

    // Time remaining on a clock
    data class TimeUpdate(val color: ClockView.Color, val ms: Long)
    var timeSubject: BehaviorSubject<TimeUpdate> = BehaviorSubject.create()

    fun removeActiveClock() {
        setActiveClock(null)
    }

    fun setActiveClock(clock: Timer?) {
        val value = clock?.color ?: ClockView.Color.NULL
        Timber.d("Active player is %s", value)
        active = clock
        activePlayerSubject.onNext(value)
    }

    fun setGameStateValue(newState : GameState) {
        Timber.d("new state is %s", newState)
        gameState = newState
        gameStateSubject.onNext(newState)
    }
}