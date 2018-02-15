package johnwilde.androidchessclock.logic

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.main.MainViewState
import johnwilde.androidchessclock.main.Partial
import johnwilde.androidchessclock.prefs.PreferencesUtil
import timber.log.Timber
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

// Responsible for updating an individual clock and publishing new view states
// on each update.
// Concrete subclasses implement the specific timing variants
abstract class Timer(val color: ClockView.Color,
            var preferencesUtil: PreferencesUtil,
            private var stateHolder : GameStateHolder,
            var timeSource: TimeSource) {

    private var clockSubscription: Disposable? = null

    // Time remaining on this clock, which other clock can subscribe to
    var timeSubject: BehaviorSubject<Long> = BehaviorSubject.create()
    // Player buttons, time and time-gap
    var clockSubject: BehaviorSubject<Partial<ClockViewState>> = BehaviorSubject.create()
    // Updates for view that draws Bronstein-delay circle
    var mainSubject: BehaviorSubject<Partial<MainViewState>> = BehaviorSubject.create()

    // last update from other clock
    var lastMsOtherClock : Long = 0

    private var moveCounter = MoveCounter(preferencesUtil)

    val moveTimes get() = moveCounter.moveTimes.toLongArray()
    var msToGo : Long = 0

    val disposables = CompositeDisposable()

    init {
        // Immediately turn on or off the time gap when preference changes
        disposables.add(preferencesUtil.timeGap
                .subscribe({ value ->
                    if (value) {
                        publishTimeGap(lastMsOtherClock)
                    } else {
                        // timeGap preference was turned off
                        hideTimeGap()
                    }}))
    }

    fun dispose() {
        disposeTimeSequenceSubscription()
        disposables.clear()
    }

    fun initialize(otherClock : Timer) {
        moveCounter = MoveCounter(preferencesUtil)
        disposables.add(otherClock.timeSubject.subscribe { ms ->
            lastMsOtherClock = ms
            publishTimeGap(ms)
        })

        clockSubject.onNext(initialState())
    }

    private fun hideTimeGap() {
        clockSubject.onNext(ClockViewState.TimeGap(show = false))
    }

    private fun publishTimeGap(otherClockMsToGo : Long) {
        // Publish time updates only when the this clock is not running
        // and the game is underway
        if (stateHolder.active != this
                && preferencesUtil.showTimeGap
                && stateHolder.gameState.isUnderway()) {
            clockSubject.onNext(
                    ClockViewState.TimeGap(
                            msGap = msToGo - otherClockMsToGo,
                            show = true))
        }
    }

    open fun moveStart() {
        moveCounter.newMove(msToGo, this)
        // Hide the time-gap clock
        hideTimeGap()
    }

    open fun moveEnd() {
        moveCounter.updateMoveTime(msToGo)
    }

    open fun pause() {
        disposeTimeSequenceSubscription()
    }

    open fun resume() {
        moveCounter.updateMoveTime(msToGo)
        clockSubscription = timeSequence().subscribe()
    }

    abstract fun setNewTime(newTime: Long)

    fun updateAndPublishMsToGo(newValue: Long) {
        msToGo = newValue
        timeSubject.onNext(newValue)
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
        if (clockSubscription != null && !clockSubscription!!.isDisposed()) {
            // This stops the timer interval
            clockSubscription!!.dispose()
        }
    }

    interface PublishesClockState {
        fun publishUpdates()
    }

    abstract fun timerTask() : PublishesClockState

    // Return observable that on subscription will cause various subjects to start emitting
    // state updates.  When subscription is disposed the state updates will stop.
    private fun timeSequence() : Observable<Any> {
        val clockTask = timerTask()
        return Observable.interval(0, 100, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .map { clockTask.publishUpdates() }
    }
}