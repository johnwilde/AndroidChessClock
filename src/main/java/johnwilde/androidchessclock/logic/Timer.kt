package johnwilde.androidchessclock.logic

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.main.MainViewState
import johnwilde.androidchessclock.main.Partial
import johnwilde.androidchessclock.prefs.PreferencesUtil
import java.util.concurrent.TimeUnit

interface PublishesClockState {
    fun publishUpdates()
}
// Responsible for updating an individual clock and publishing new view states
// on each update.
// Concrete subclasses implement the specific timing variants
abstract class Timer(val color: ClockView.Color,
            var preferencesUtil: PreferencesUtil,
            private var stateHolder : GameStateHolder,
            var timeSource: TimeSource) {

    private var clockSubscription: Disposable = Disposables.empty()

    // Updates for a player button: time and time-gap
    var clockSubject: BehaviorSubject<Partial<ClockViewState>> = BehaviorSubject.create()
    // Updates for main view: draw Bronstein-delay circle
    var mainSubject: BehaviorSubject<Partial<MainViewState>> = BehaviorSubject.create()

    // Utilities to keep track of moves and time gaps
    private var moveCounter = MoveCounter(preferencesUtil)
    private var timeGap =  TimeGap(preferencesUtil, color, stateHolder, clockSubject)

    val moveTimes get() = moveCounter.moveTimes.toLongArray()
    var msToGo : Long = 0
    abstract fun timerTask() : PublishesClockState
    abstract fun setNewTime(newTime: Long)

    open fun moveStart() {
        moveCounter.newMove(msToGo, this)
    }

    open fun moveEnd() {
        moveCounter.updateMoveTime(msToGo)
    }

    open fun pause() {
        disposeTimeSequenceSubscription()
    }

    open fun resume() {
        moveCounter.updateMoveTime(msToGo)
        // Return observable that on subscription will cause various subjects to start emitting
        // state updates.  When subscription is disposed the state updates will stop.
        val clockTask = timerTask()
        clockSubscription = Observable.interval(0, 100, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .map { clockTask.publishUpdates() }
                .subscribe()
    }

    fun updateAndPublishMsToGo(newValue: Long) {
        msToGo = newValue
        stateHolder.timeSubject.onNext(GameStateHolder.TimeUpdate(color, newValue))
    }

    fun dispose() {
        disposeTimeSequenceSubscription()
        timeGap.dispose()
    }

    fun initialize() {
        moveCounter = MoveCounter(preferencesUtil)
        timeGap = TimeGap(preferencesUtil, color, stateHolder, clockSubject)
        clockSubject.onNext(initialState())
    }

    fun initialState() : ClockViewState {
        return ClockViewState(
                button = ClockViewState.Button(
                        enabled = true,
                        msToGo = msToGo),
                timeGap = ClockViewState.TimeGap(show = false),
                prompt = ClockViewState.Snackbar(dismiss = true),
                moveCount = ClockViewState.MoveCount(
                        message = ClockViewState.MoveCount.Message.NONE,
                        count = 0)
        )
    }

    open fun publishInactiveState() {
        clockSubject.onNext(
                ClockViewState.Button(
                        enabled = false,
                        msToGo = msToGo)
        )
        clockSubject.onNext(
                ClockViewState.MoveCount(
                        message = ClockViewState.MoveCount.Message.NONE,
                        count = 0)
        )
    }

    fun buttonIsEnabled(): Boolean {
        return if (stateHolder.gameState == GameStateHolder.GameState.NOT_STARTED) {
            true
        } else {
            stateHolder.active == this
        }
    }

    // Stop the interval updates
    private fun disposeTimeSequenceSubscription() {
        if (!clockSubscription.isDisposed) {
            // This stops the timer interval
            clockSubscription.dispose()
        }
    }
}