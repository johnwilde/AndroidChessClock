package johnwilde.androidchessclock.sound

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import io.reactivex.Observable
import johnwilde.androidchessclock.logic.ClockManager
import johnwilde.androidchessclock.logic.GameStateHolder
import johnwilde.androidchessclock.prefs.PreferencesUtil
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SoundViewPresenter(val clockManager: ClockManager, val preferencesUtil: PreferencesUtil)
    : MviBasePresenter<SoundView, SoundViewState> () {

    // Only invoked the first time view is attached to presenter
    override fun bindIntents() {
        Timber.d("bindIntents for SoundViewPresenter")
        // Generate a new view state when the game state changes
        val buzzer = clockManager.stateHolder.gameStateSubject
                .filter{ it == GameStateHolder.GameState.FINISHED
                        || it == GameStateHolder.GameState.NEGATIVE }
                .filter{ preferencesUtil.playBuzzerAtEnd }
                .map{ Buzzer() }

        // The active player has changed
        val clickNewMove = clockManager.stateHolder.activePlayerSubject
                .filter{ clockManager.stateHolder.gameState == GameStateHolder.GameState.PLAYING }
                .distinctUntilChanged()
                .filter{ preferencesUtil.playSoundOnButtonTap }
                .map { Click() }

        // The game started or resumed from pause
        val clickStartGame = clockManager.stateHolder.gameStateSubject
                .filter{ preferencesUtil.playSoundOnButtonTap }
                .distinctUntilChanged()
                .filter{ it == GameStateHolder.GameState.PLAYING }
                .map { Click() }

        var updates = Observable.merge(
                buzzer, clickNewMove, clickStartGame
        )

        subscribeViewState(updates, SoundView::render)
    }

    override fun unbindIntents() {
        super.unbindIntents()
        Timber.d("unbindIntents for playPausePresenter")
    }
}