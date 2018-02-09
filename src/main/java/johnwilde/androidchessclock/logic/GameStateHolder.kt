package johnwilde.androidchessclock.logic

import timber.log.Timber
import javax.inject.Singleton

@Singleton
class GameStateHolder {
    enum class GameState {NOT_STARTED, PAUSED, PLAYING, FINISHED}
    var gameState : GameState = GameState.NOT_STARTED
    var active : TimerLogic? = null

    fun setActiveClock(clock: TimerLogic) {
        Timber.d("Active player is %s", clock.color)
        active = clock
    }

    fun setGameStateValue(newState : GameState) {
        Timber.d("new state is %s", newState)
        gameState = newState
    }
}