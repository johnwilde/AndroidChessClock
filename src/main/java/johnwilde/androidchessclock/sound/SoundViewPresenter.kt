package johnwilde.androidchessclock.sound

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import io.reactivex.Observable
import johnwilde.androidchessclock.logic.ClockManager
import johnwilde.androidchessclock.logic.GameStateHolder
import johnwilde.androidchessclock.main.MainViewState
import johnwilde.androidchessclock.main.Partial
import johnwilde.androidchessclock.prefs.PreferencesUtil
import timber.log.Timber

class SoundViewPresenter(val clockManager: ClockManager, val preferencesUtil: PreferencesUtil)
    : MviBasePresenter<SoundView, SoundViewState> () {

    // Only invoked the first time view is attached to presenter
    override fun bindIntents() {
        Timber.d("bindIntents for SoundViewPresenter")
        // Generate a new view state when the game state changes
        val buzzer = clockManager.stateHolder.gameStateSubject
                .filter{ it == GameStateHolder.GameState.FINISHED }
                .filter{ preferencesUtil.playBuzzerAtEnd }
                .map{ Buzzer() }

        // The active player has changed
        val click = clockManager.stateHolder.activePlayerSubject
                .filter{ preferencesUtil.playSoundOnButtonTap }
                .map { Click() }

        var updates = Observable.merge(
                buzzer, click
        )

        subscribeViewState(updates, SoundView::render)
    }

    override fun unbindIntents() {
        super.unbindIntents()
        Timber.d("unbindIntents for playPausePresenter")
    }
}