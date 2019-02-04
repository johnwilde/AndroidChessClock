package johnwilde.androidchessclock.main

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.logic.ClockManager
import johnwilde.androidchessclock.logic.GameStateHolder.GameState
import timber.log.Timber
import java.util.concurrent.TimeUnit

// Responsible for user interactions with the play/stop button and
// the myDrawer opening.  The view keeps the play/stop button in the correct
// state and presents a snackbar telling which player ran out of time. It also
// draws the mainSubject (used in Bronstein time mode).
class MainViewPresenter(val clockManager: ClockManager)
    : MviBasePresenter<MainView, MainViewState> () {

    // Only invoked the first time view is attached to presenter
    override fun bindIntents() {
        Timber.d("bindIntents for playPausePresenter")
        // Tapped play/stop
        val playPauseIntent = intent(MainView::playPauseIntent)
                .map { clockManager.playPause() }
                .flatMap { Observable.empty<Partial<MainViewState>>() }

        // Drawer opened
        val drawerOpened = intent(MainView::drawerOpened)
                .map { clockManager.pause() }
                .flatMap { Observable.empty<Partial<MainViewState>>() }

        // User swiped away snackbar
        val snackBarDismissed = intent(MainView::snackBarDismissed)
                .flatMap { _ ->
                    val dismiss = MainViewState.Snackbar(dismiss = true)
                    Observable.just(dismiss)
                }

        // Generate a new view state when the game state changes
        val stateChange = clockManager.stateHolder.gameStateSubject
                .flatMap { state ->
                    val change: Any = when (state) {
                        GameState.NOT_STARTED -> MainViewState.initialState
                        GameState.PAUSED ->
                            Observable.fromArray(
                                    MainViewState.PlayPauseButton(MainViewState.PlayPauseButton.State.PAUSE),
                                    MainViewState.TakeBack(true, false))
                        GameState.PLAYING, GameState.NEGATIVE ->
                            Observable.fromArray(
                                    MainViewState.PlayPauseButton(MainViewState.PlayPauseButton.State.PLAY),
                                    MainViewState.TakeBack(false, false))
                        else -> onGameOver()
                    }
                    val result = if (change is Observable<*>) {
                        change
                    } else {
                        Observable.just(change)
                    }
                    result as Observable<Partial<MainViewState>>
                }

        val forward = intent(MainView::goForward)
                .flatMap { Observable.just(clockManager.takeBackController.forward()) }

        val back = intent(MainView::goBack)
                .flatMap { Observable.just(clockManager.takeBackController.back()) }

        val intents = mutableListOf(
                playPauseIntent,
                drawerOpened,
                snackBarDismissed,
                stateChange,
                forward, back,
                // Allows this view to update the Spinner
                Observable.merge(
                        clockManager.forColor(ClockView.Color.WHITE).main,
                        clockManager.forColor(ClockView.Color.BLACK).main))

        var updates = Observable.merge(intents)

        subscribeViewState(
                updates.scan(MainViewState.initialState, ::reducer)
                        .observeOn(AndroidSchedulers.mainThread()),
                MainView::render)
    }

    private fun reducer(previous: MainViewState, updates: Partial<MainViewState>): MainViewState {
        return updates.reduce(previous)
    }

    private fun onGameOver(): Observable<Partial<MainViewState>> {
        val message = if (clockManager.active().color == ClockView.Color.WHITE) {
            MainViewState.Snackbar.Message.WHITE_LOST
        } else {
            MainViewState.Snackbar.Message.BLACK_LOST
        }

        // Show message for 5 seconds
        return Observable.timer(5, TimeUnit.SECONDS)
                .map { _ -> MainViewState.Snackbar(dismiss = true) as Partial<MainViewState> }
                .startWithArray(
                        MainViewState.Snackbar(show = true, message = message),
                        MainViewState.PlayPauseButton(MainViewState.PlayPauseButton.State.FINISHED),
                        MainViewState.TakeBack(false, false)
                )
    }

    override fun unbindIntents() {
        super.unbindIntents()
        Timber.d("unbindIntents for playPausePresenter")
    }
}