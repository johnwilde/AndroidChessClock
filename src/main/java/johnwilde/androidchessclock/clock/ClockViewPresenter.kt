package johnwilde.androidchessclock.clock

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import johnwilde.androidchessclock.logic.ClockManager
import timber.log.Timber

class ClockViewPresenter(val color: ClockView.Color, val clockManager: ClockManager)
    : MviBasePresenter<ClockView, ClockViewState> () {

    // Only invoked the first time view is attached to presenter
    // "forwards" intents from ClockView to ClockManager (from view to business logic)
    override fun bindIntents() {
        val stateUpdates = intent(ClockView::clickIntent)
                .flatMap {
                    clockManager.moveEnd(color)
                }

        val updates : Observable<PartialState> =
                Observable.merge(stateUpdates, clockManager.clockUpdates(color))

        val initialState = clockManager.initialState(color)
        
        // Subscribe the view to updates from the business logic
        subscribeViewState(
                updates.scan(initialState, ::reducer)
                        .observeOn(AndroidSchedulers.mainThread()),
                ClockView::render)
    }

    private fun reducer(previous : ClockViewState, updates: PartialState) : ClockViewState {
       return updates.reduce(previous)
    }

    override fun unbindIntents() {
        super.unbindIntents()
        Timber.d("unbindIntents for %s", color)
    }

    public override fun getViewStateObservable(): Observable<ClockViewState> {
        return super.getViewStateObservable()
    }
}