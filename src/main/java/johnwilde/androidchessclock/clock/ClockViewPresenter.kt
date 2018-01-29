package johnwilde.androidchessclock.clock

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import io.reactivex.Observable
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

        val updates : Observable<ClockViewState> =
                Observable.merge(stateUpdates, clockManager.clockUpdates(color))

        val initialState = FullViewState(clockManager.initialState(color))
        // Subscribe the view to updates from the business logic
        subscribeViewState(
                updates.scan(initialState, ::viewStateReducer).distinctUntilChanged(),
                ClockView::render)
    }

    private fun viewStateReducer(previousState : ClockViewState, stateChanges : ClockViewState)
            : ClockViewState {
        val previous = previousState as FullViewState
        return when (stateChanges) {
            is ButtonViewState -> FullViewState(stateChanges, previous.timeGap)
            is TimeGapViewState -> FullViewState(previous.button, stateChanges)
            is PromptToMove -> FullViewState(previous.button, previous.timeGap, stateChanges)
            is FullViewState -> stateChanges
        }
    }

    override fun unbindIntents() {
        super.unbindIntents()
        Timber.d("unbindIntents for %s", color)
    }

    public override fun getViewStateObservable(): Observable<ClockViewState> {
        return super.getViewStateObservable()
    }
}