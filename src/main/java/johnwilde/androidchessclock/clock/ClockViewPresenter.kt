package johnwilde.androidchessclock.clock

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import johnwilde.androidchessclock.logic.ClockManager
import johnwilde.androidchessclock.logic.GameStateHolder.GameState.*
import johnwilde.androidchessclock.main.Partial
import timber.log.Timber
import java.util.concurrent.TimeUnit

// Responsible for reacting to user interactions with an individual clock.
// Presents a snackbar when wrong clock was tapped. Also responsible for
// passing along updates to the two timers (above the clock and the time-gap)
class ClockViewPresenter(val color: ClockView.Color, val clockManager: ClockManager)
    : MviBasePresenter<ClockView, ClockViewState> () {

    // Only invoked the first time view is attached to presenter
    // "forwards" intents from ClockView to ClockManager (from view to business logic)
    override fun bindIntents() {
        // Not sure how to cleanly handle the case where in some situations
        // we want to return a Partial<ClockViewState>, but in others we
        // simply want to call the manager and let the update come through
        // the clockSubject.
        val empty = Observable.empty<Partial<ClockViewState>>()
        val clockTapped = intent(ClockView::clickIntent)
                .flatMap {
                    when (clockManager.stateHolder.gameState) {
                        NOT_STARTED -> {
                            if (color == ClockView.Color.WHITE) {
                                handleBadClick(ClockViewState.Snackbar.Message.START)
                            } else {
                                clockManager.clockButtonTap(color)
                                empty
                            }
                        }
                        PAUSED -> {
                            if (clockManager.stateHolder.active?.color == color) {
                                handleBadClick(ClockViewState.Snackbar.Message.RESUME)
                            } else {
                                clockManager.clockButtonTap(color)
                                empty
                            }
                        }
                        PLAYING, NEGATIVE -> {
                            clockManager.clockButtonTap(color)
                            empty
                        }
                        FINISHED -> empty
                    }
                }

        val updates: Observable<Partial<ClockViewState>> =
                Observable.merge(clockTapped, clockManager.forColor(color).clock)

        val initialState = clockManager.timerForColor(color).initialState()

        // Subscribe the view to updates from the business logic
        subscribeViewState(
                updates.scan(initialState, ::reducer)
                        .observeOn(AndroidSchedulers.mainThread()),
                ClockView::render)
    }

    private fun reducer(previous: ClockViewState, updates: Partial<ClockViewState>): ClockViewState {
        return updates.reduce(previous)
    }

    private fun handleBadClick(message: ClockViewState.Snackbar.Message): Observable<Partial<ClockViewState>> {
        // Must tap non-active color to start/start game
        // Let's show Snackbar for 2 seconds and then dismiss it
        return Observable.timer(2, TimeUnit.SECONDS)
                .map { _ -> ClockViewState.Snackbar(dismiss = true) as Partial<ClockViewState> }
                .startWith(ClockViewState.Snackbar(
                        show = true,
                        message = message) as Partial<ClockViewState>)
    }

    override fun unbindIntents() {
        super.unbindIntents()
        Timber.d("unbindIntents for %s", color)
    }

    public override fun getViewStateObservable(): Observable<ClockViewState> {
        return super.getViewStateObservable()
    }
}