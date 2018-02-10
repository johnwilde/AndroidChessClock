package johnwilde.androidchessclock.main

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import johnwilde.androidchessclock.logic.ClockManager
import timber.log.Timber

class PlayPausePresenter(val clockManager: ClockManager)
    : MviBasePresenter<PlayPauseView, MainViewState> () {

    // Only invoked the first time view is attached to presenter
    override fun bindIntents() {
        Timber.d("bindIntents for playPausePresenter")
        // Tapped play/pause
        val playPauseIntent = intent(PlayPauseView::playPauseIntent)
                .flatMap { clockManager.playPause() }

        // Drawer opened
        val drawerOpened = intent(PlayPauseView::drawerOpened)
                .flatMap { clockManager.pause() }

        // Drawer opened
        val snackBarDismissed = intent(PlayPauseView::snackBarDismissed)
                .flatMap { _ ->
                    val dismiss = MainViewState.Snackbar("", false, true) as MainStateUpdate
                    Observable.just(dismiss)
                }

        val intents = mutableListOf(
                playPauseIntent,
                drawerOpened,
                snackBarDismissed,
                // Allows this view to update the Spinner
                clockManager.spinnerObservable,
                // Allows play/pause button to react to game state change triggered by a player button tap
                clockManager.playPauseSubject)

        var updates = Observable.merge(intents)

        subscribeViewState(
                updates.scan(MainViewState.initialState, ::reducer)
                        .observeOn(AndroidSchedulers.mainThread()),
                PlayPauseView::render)
    }

    private fun reducer(previous : MainViewState, updates: MainStateUpdate) : MainViewState {
        return updates.reduce(previous)
    }

    override fun unbindIntents() {
        super.unbindIntents()
        Timber.d("unbindIntents for playPausePresenter")
    }
}