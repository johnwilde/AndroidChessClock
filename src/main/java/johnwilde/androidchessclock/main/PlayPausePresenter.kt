package johnwilde.androidchessclock.main

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import johnwilde.androidchessclock.logic.ClockManager
import timber.log.Timber

class PlayPausePresenter(val clockManager: ClockManager)
    : MviBasePresenter<PlayPauseView, PlayPauseViewState> () {

    // Only invoked the first time view is attached to presenter
    override fun bindIntents() {
        Timber.d("bindIntents for playPausePresenter")
        // Tapped play/pause
        val playPauseIntent = intent(PlayPauseView::playPauseIntent)
                .flatMap { clockManager.playPause() }

        // Drawer opened
        val drawerOpened = intent(PlayPauseView::drawerOpened)
                .flatMap { clockManager.pause() }

        var updates = Observable.merge(
                playPauseIntent,
                drawerOpened,
                // Allows this view to update the Spinner
                clockManager.spinnerObservable,
                // Allows play/pause button to react to game state change triggered by a player button tap
                clockManager.playPauseSubject)
                .observeOn(AndroidSchedulers.mainThread())

        subscribeViewState(updates, PlayPauseView::render)
    }

    override fun unbindIntents() {
        super.unbindIntents()
        Timber.d("unbindIntents for playPausePresenter")
    }
}