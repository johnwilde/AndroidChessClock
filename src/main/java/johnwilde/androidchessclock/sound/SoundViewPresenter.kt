package johnwilde.androidchessclock.sound

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import io.reactivex.Observable
import johnwilde.androidchessclock.logic.ClockManager
import johnwilde.androidchessclock.prefs.PreferencesUtil
import timber.log.Timber

class SoundViewPresenter(val clockManager: ClockManager, val preferencesUtil: PreferencesUtil)
    : MviBasePresenter<SoundView, SoundViewState> () {

    // Only invoked the first time view is attached to presenter
    override fun bindIntents() {
        Timber.d("bindIntents for SoundViewPresenter")

        var updates = Observable.merge(
                clockManager.clickObservable.filter{ preferencesUtil.playSoundOnButtonTap },
                clockManager.buzzerObservable.filter{ preferencesUtil.playBuzzerAtEnd }
        )

        subscribeViewState(updates, SoundView::render)
    }

    override fun unbindIntents() {
        super.unbindIntents()
        Timber.d("unbindIntents for playPausePresenter")
    }
}