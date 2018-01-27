package johnwilde.androidchessclock.clock

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import io.reactivex.Observable
import johnwilde.androidchessclock.logic.ClockManager
import timber.log.Timber

class ClockViewPresenter(val color: ClockView.Color, val clockManager: ClockManager)
    : MviBasePresenter<ClockView, ClockViewState> () {

    // Only invoked the first time view is attached to presenter
    // "forwards" intents from ClockView to TimerLogic (from view to business logic)
    override fun bindIntents() {

        var stateUpdates = intent(ClockView::clickIntent)
                .flatMap {
                    clockManager.moveEnd(color)
                }
                .startWith(clockManager.initialState(color))

        val updates : Observable<ClockViewState> =
                Observable.merge(stateUpdates, clockManager.clockUpdates(color))

        // Subscribe the view to updates from the business logic
        subscribeViewState(updates, ClockView::render)
    }

    override fun unbindIntents() {
        super.unbindIntents()
        Timber.d("unbindIntents for %s", color)
    }

    public override fun getViewStateObservable(): Observable<ClockViewState> {
        return super.getViewStateObservable()
    }
}