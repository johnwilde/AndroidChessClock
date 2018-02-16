package johnwilde.androidchessclock.logic

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.main.Partial
import johnwilde.androidchessclock.prefs.PreferencesUtil
import timber.log.Timber

class TimeGap(val preferencesUtil : PreferencesUtil,
              val color: ClockView.Color,
              val stateHolder: GameStateHolder,
              val clockSubject: BehaviorSubject<Partial<ClockViewState>>) {

    private val disposables = CompositeDisposable()

    // last update from other clock
    private var lastMs : Long = 0
    private var lastMsOtherClock : Long = 0

    init {
        { Timber.d("hi")}
        // Immediately turn on or off the time gap when preference changes
        disposables.add(preferencesUtil.timeGap
                .subscribe { _ -> updateTimeGap() })
        disposables.add(stateHolder.timeSubject
                .map { u ->
                    Timber.d("mapper %s", u)
                    if (u.color == color) {
                        lastMs = u.ms
                    } else {
                        lastMsOtherClock = u.ms
                    }
                    updateTimeGap()
                }
                .subscribe()
        )
    }

    private fun hideTimeGap() {
        clockSubject.onNext(ClockViewState.TimeGap(show = false))
    }

    private fun updateTimeGap() {
        // Publish time updates only when the this clock is not running
        // and the game is underway
        if (stateHolder.active?.color != color
                && preferencesUtil.showTimeGap
                && stateHolder.gameState.isUnderway()) {
            clockSubject.onNext(
                    ClockViewState.TimeGap(
                            msGap = lastMs - lastMsOtherClock,
                            show = true))
        } else {
            hideTimeGap()
        }
    }

    fun dispose() {
        disposables.clear()
    }
}