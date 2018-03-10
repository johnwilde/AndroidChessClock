package johnwilde.androidchessclock.logic

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.main.MainViewState
import johnwilde.androidchessclock.main.Partial
import johnwilde.androidchessclock.prefs.PreferencesUtil
import timber.log.Timber
import java.util.concurrent.TimeUnit

interface PublishesClockState {
    fun publish()
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
    var moveCounter = MoveCounter(preferencesUtil)
    private var timeGap =  TimeGap(preferencesUtil, color, stateHolder, clockSubject)

    val moveTimes get() = moveCounter.moveTimes.toLongArray()
    var msToGo : Long = 0
    abstract fun timerTask() : PublishesClockState
    abstract fun setNewTime(newTime: Long)

    fun initialize() {
        moveCounter = MoveCounter(preferencesUtil)
        timeGap = TimeGap(preferencesUtil, color, stateHolder, clockSubject)
        clockSubject.onNext(initialState())
    }

    open fun moveStart() {
        moveCounter.newMove(msToGo, this)
        clockSubject.onNext(ClockViewState.Button(enabled = true))
    }

    open fun moveEnd() {
        moveCounter.updateMoveTime(msToGo)
        clockSubject.onNext(ClockViewState.Button(enabled = false))
    }

    open fun stop() {
        disposeTimeSequenceSubscription() // disposing causes final msToGo update to happen
        moveCounter.updateMoveTime(msToGo)
    }

    open fun start() {
        // Return observable that on subscription will cause various subjects to start emitting
        // state updates.  When subscription is disposed the state updates will stop.
        val clockTask = timerTask()
        clockSubscription = Observable.interval(0,100, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .map { clockTask.publish() } // each interval
                .doOnDispose { clockTask.publish() } // the final update
                .subscribe()
    }

    // Timer is being destroyed, make sure to dispose all subscriptions to avoid memory leak
    open fun destroy() {
        disposeTimeSequenceSubscription()
        timeGap.destroy()
    }

    fun updateAndPublishMsToGo(newValue: Long) {
        msToGo = newValue
        stateHolder.timeSubject.onNext(GameStateHolder.TimeUpdate(color, newValue))
    }

    fun initialState() : ClockViewState {
        return ClockViewState(
                button = ClockViewState.Button(enabled = true),
                time = ClockViewState.Time(msToGo = msToGo),
                timeGap = ClockViewState.TimeGap(show = false),
                prompt = ClockViewState.Snackbar(dismiss = true),
                moveCount = ClockViewState.MoveCount(
                        message = ClockViewState.MoveCount.Message.NONE,
                        count = 0)
        )
    }

    open fun publishActiveState() {
        clockSubject.onNext(ClockViewState.Button(enabled = true))
        clockSubject.onNext(
                ClockViewState.MoveCount(
                        message = ClockViewState.MoveCount.Message.TOTAL,
                        count = moveCounter.count)
        )
    }

    open fun publishInactiveState() {
        clockSubject.onNext(ClockViewState.Button(enabled = false))
        clockSubject.onNext(
                ClockViewState.MoveCount(
                        message = ClockViewState.MoveCount.Message.NONE,
                        count = 0)
        )
    }

    // Stop the interval updates
    private fun disposeTimeSequenceSubscription() {
        if (!clockSubscription.isDisposed) {
            // This stops the timer interval
            clockSubscription.dispose()
        }
    }
}