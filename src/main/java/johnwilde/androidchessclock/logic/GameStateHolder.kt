package johnwilde.androidchessclock.logic

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
    }

    var gameState : GameState = NOT_STARTED
    var active : TimerLogic? = null
    val gameStateSubject = PublishSubject.create<GameState>()

    fun setActiveClock(clock: TimerLogic) {
        Timber.d("Active player is %s", clock.color)
        active = clock
    }


    fun setGameStateValue(newState : GameState) {
        Timber.d("new state is %s", newState)
        gameState = newState
        gameStateSubject.onNext(newState)
    }
}