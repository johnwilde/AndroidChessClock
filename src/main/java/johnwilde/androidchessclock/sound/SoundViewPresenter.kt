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
                .doOnNext{ s -> Timber.d("%s", s) }
                .filter{ it == GameStateHolder.GameState.FINISHED }
                .filter{ preferencesUtil.playBuzzerAtEnd }
                .map{ Observable.just<SoundViewState>(Buzzer()) }

//        val stateChange = clockManager.stateHolder.gameStateSubject
//                .takeLast(2)
//                .scan { t1: GameStateHolder.GameState, t2: GameStateHolder.GameState ->
//                   if (t1 == GameStateHolder.GameState.NOT_STARTED
//                           && t2 == GameStateHolder.GameState.PLAYING) {
//                       Click()
//                   } else {
//                       null
//                   }
//                }
//                .flatMap { state ->
//                    val change :  Any = when {
//                        state == GameStateHolder.GameState.NOT_STARTED -> MainViewState.initialState
//                        state.isUnderway() -> MainViewState.PlayPauseButton(MainViewState.PlayPauseButton.State.PAUSE, true)
//                        else -> onGameOver()
//                    }
//                    val result = if (change is Observable<*>) {
//                        change
//                    } else {
//                        Observable.just(change)
//                    }
//                    result as Observable<Partial<MainViewState>>
//                }

        var updates = Observable.merge(
                buzzer
        )

        subscribeViewState(updates, SoundView::render)
    }

    override fun unbindIntents() {
        super.unbindIntents()
        Timber.d("unbindIntents for playPausePresenter")
    }
}