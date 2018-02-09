package johnwilde.androidchessclock.logic

import javax.inject.Singleton

@Singleton
class GameStateHolder {
    enum class GameState {NOT_STARTED, PAUSED, PLAYING, FINISHED}
    var gameState : GameState = GameState.NOT_STARTED
    var active : TimerLogic? = null

    fun setActiveClock(clock: TimerLogic) {
        active = clock
    }

    fun setGameStateValue(newState : GameState) {
        gameState = newState
    }
}