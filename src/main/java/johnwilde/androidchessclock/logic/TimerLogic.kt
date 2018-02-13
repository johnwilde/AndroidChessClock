package johnwilde.androidchessclock.logic

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.logic.GameStateHolder.GameState
import johnwilde.androidchessclock.main.MainViewState
import johnwilde.androidchessclock.main.Partial
import johnwilde.androidchessclock.prefs.PreferencesUtil
import timber.log.Timber
import java.util.concurrent.TimeUnit

// Responsible for updating an individual clock and publishing new view states
// on each update.
class TimerLogic(val color: ClockView.Color,
                 var preferencesUtil: PreferencesUtil,
                 private var stateHolder : GameStateHolder,
                 var timeSource: TimeSource) {
    var msToGo : Long = 0
    var msDelayToGo: Long = 0
    var msToGoMoveStart : Long = 0

    // Time remaining on this clock, which other clock can subscribe to
    var msToGoUpdateSubject: BehaviorSubject<Long> = BehaviorSubject.create()
    // Player buttons, time and time-gap
    var clockUpdateSubject: PublishSubject<Partial<ClockViewState>> = PublishSubject.create()
    // Updates for view that draws Bronstein-delay circle
    var spinner: BehaviorSubject<Partial<MainViewState>> = BehaviorSubject.create()

    var lastMsOtherClock : Long = 0
    private var clockSubscription: Disposable? = null
    private var moveCounter = MoveCounter()
    val moveTimes get() = moveCounter.moveTimes.toLongArray()

    private inner class MoveCounter {
        var count : Int = 0 // number of moves for this player
        val moveTimes = mutableListOf<Long>() // array of time for each move (ms)

        fun newMove() {
            count += 1
            msToGoMoveStart = msDelayToGo + msToGo
            moveTimes.add(0)
        }

        fun updateMoveTime() {
            val msMoveTime = msToGoMoveStart - msDelayToGo - msToGo
            moveTimes[moveTimes.lastIndex] = msMoveTime
        }

        fun display() : String {
            return count.toString()
        }
    }

    init {
        setInitialTime()
        // Immediately turn on or off the time gap when preference changes
        val ignored = preferencesUtil.timeGap
                .subscribe({ value ->
                    if (value) {
                        publishTimeGap(lastMsOtherClock)
                    } else {
                        // timeGap preference was turned off
                        hideTimeGap()
                    }})
    }

    private fun hideTimeGap() {
        clockUpdateSubject.onNext(ClockViewState.TimeGap(show = false))
    }

    private fun publishTimeGap(otherClockMsToGo : Long) {
        // Publish time updates only when the this clock is not running
        // and the game is underway
        if (stateHolder.active != this
                && preferencesUtil.showTimeGap
                && stateHolder.gameState.isUnderway()) {
            clockUpdateSubject.onNext(
                    ClockViewState.TimeGap(
                            msGap = msToGo - otherClockMsToGo,
                            show = true))
        }
    }

    fun subscribeToClock(otherClock : TimerLogic) {
        val ignored = otherClock.msToGoUpdateSubject.subscribe { ms ->
            lastMsOtherClock = ms
            publishTimeGap(ms)
        }
    }

    private fun setInitialTime() {
        updateAndPublishMsToGo(preferencesUtil.initialDurationSeconds * 1000.toLong())
        msDelayToGo = 0
        moveCounter = MoveCounter()
    }

    fun onMoveStart() {
        msDelayToGo = preferencesUtil.getBronsteinDelayMs()
        updateAndPublishMsToGo(msToGo + preferencesUtil.getFischerDelayMs())
        moveCounter.newMove()

        // This task will publish clock updates to clockUpdateSubject
        resume()

        // Hide the time-gap clock
        hideTimeGap()
    }

    fun onMoveEnd() {
        pause()
        moveCounter.updateMoveTime()
        if (preferencesUtil.timeControlType != PreferencesUtil.TimeControlType.BASIC) {
            if (moveCounter.count == preferencesUtil.phase1NumberOfMoves) {
                updateAndPublishMsToGo(msToGo + preferencesUtil.phase1Minutes * 60 * 1000)
            }
        }
        publishInactiveState()
    }

    fun publishInactiveState() {
        // At end of turn, dim the button and remove the spinner
        spinner.onNext(MainViewState.Spinner(0))
        clockUpdateSubject.onNext(ClockViewState.Button(false, msToGo, ""))
    }

    fun onTimeExpired() {
        moveCounter.updateMoveTime()
    }

    fun reset() {
        disposeTimeSequenceSubscription()
        setInitialTime()
        spinner.onNext(MainViewState.Spinner(0))
        clockUpdateSubject.onNext(initialState())
        hideTimeGap()
    }

    fun resume() {
        clockSubscription = timeSequence().subscribe()
    }

    fun pause() {
        moveCounter.updateMoveTime()
        disposeTimeSequenceSubscription()
    }

    private fun updateAndPublishMsToGo(newValue: Long) {
        msToGo = newValue
        msToGoUpdateSubject.onNext(newValue)
    }

    // Stop the interval updates
    private fun disposeTimeSequenceSubscription() {
        if (clockSubscription != null && !clockSubscription!!.isDisposed()) {
            // This stops the timer interval
            clockSubscription!!.dispose()
        }
    }

    // Return observable that on subscription will cause various subjects to start emitting
    // state updates.  When subscription is disposed the state updates will stop.
    private fun timeSequence() : Observable<Any> {
        val clockTask = UpdateTime()
        return Observable.interval(0, 100, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .map { if (!clockTask.publishUpdates()) pause() }
    }

    // Responsible for updating `msToGo` and `msDelayToGo` timer state
    // The state variables are updated on each call to publishUpdates, and the
    // new view states are published to clock update, spinner, and buzzer subjects.
    private inner class UpdateTime {
        private var lastUpdateMs: Long = timeSource.currentTimeMillis()

        // Returns false when time has elapsed (and user doesn't want negative time)
        fun publishUpdates(): Boolean {
            val now = timeSource.currentTimeMillis()
            val dt = now - lastUpdateMs
            lastUpdateMs = now

            return if (msDelayToGo > 0) {
                // While in Bronstein period, we just decrement delay time
                msDelayToGo -= dt
                updateAndPublishMsToGo(msToGo)
                clockUpdateSubject.onNext(
                        ClockViewState.Button(
                                enabled = true,
                                msToGo = msToGo,
                                moveCount = moveCounter.display())
                )
                spinner.onNext(MainViewState.Spinner(msDelayToGo))
                true
            } else {
                updateAndPublishMsToGo(msToGo - dt)
                // After decrementing clock, publish new time
                clockUpdateSubject.onNext(
                        ClockViewState.Button(
                                enabled = true,
                                msToGo = msToGo,
                                moveCount = moveCounter.display())
                )
                if (msToGo > 0) {
                    true
                } else {
                    preferencesUtil.allowNegativeTime // continues update if true
                }
            }
        }
    }

    fun setNewTime(newTime: Long) {
        updateAndPublishMsToGo(newTime)
        clockUpdateSubject.onNext(
                ClockViewState.Button(
                        enabled = buttonIsEnabled(),
                        msToGo = msToGo,
                        moveCount = moveCounter.display()
                )
        )
    }

    private fun buttonIsEnabled(): Boolean {
        return if (stateHolder.gameState == GameState.NOT_STARTED) {
            true
        } else {
            stateHolder.active == this
        }
    }

    fun initialState() : ClockViewState {
        return ClockViewState(
                button = ClockViewState.Button(
                        enabled = buttonIsEnabled(),
                        msToGo = msToGo,
                        moveCount = ""),
                timeGap = ClockViewState.TimeGap(show = false),
                prompt = ClockViewState.Snackbar(dismiss = true))
    }

}