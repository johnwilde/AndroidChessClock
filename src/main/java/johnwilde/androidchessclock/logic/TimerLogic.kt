package johnwilde.androidchessclock.logic

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import johnwilde.androidchessclock.clock.ButtonViewState
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.PartialState
import johnwilde.androidchessclock.clock.TimeGapViewState
import johnwilde.androidchessclock.logic.GameStateHolder.GameState
import johnwilde.androidchessclock.main.MainStateUpdate
import johnwilde.androidchessclock.main.SpinnerStateUpdate
import johnwilde.androidchessclock.prefs.PreferencesUtil
import johnwilde.androidchessclock.sound.Buzzer
import johnwilde.androidchessclock.sound.SoundViewState
import timber.log.Timber
import java.util.concurrent.TimeUnit

class TimerLogic(val color: ClockView.Color,
                 var preferencesUtil: PreferencesUtil,
                 private var stateHolder : GameStateHolder,
                 var timeSource: TimeSource) {
    var msToGo : Long = 0
    var msDelayToGo: Long = 0
    var msToGoMoveStart : Long = 0
    var playedBuzzer : Boolean = false


    private var msToGoUpdateSubject: BehaviorSubject<Long> = BehaviorSubject.create()
    // Player buttons, time and time-gap
    var clockUpdateSubject: PublishSubject<PartialState> = PublishSubject.create()
    // Updates for view that draws Bronstein-delay circle
    var spinner: BehaviorSubject<MainStateUpdate> = BehaviorSubject.create()
    // When time runs out send an update
    var buzzer: BehaviorSubject<SoundViewState> = BehaviorSubject.create()

    private var clockSubscription: Disposable? = null

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
    private var moveCounter = MoveCounter()
    val moveTimes get() = moveCounter.moveTimes.toLongArray()

    init {
        setInitialTime()
        val ignored = preferencesUtil.timeGap.asObservable()
                .subscribe({ value ->
                    if (value) {
                        publishTimeGap(lastMsOtherClock)
                    } else {
                        updateTimeGap(TimeGapViewState(show = false), forceUpdate = true)
                    }})
    }


    private fun updateTimeGap(newState : TimeGapViewState, forceUpdate : Boolean = false) {
        if (preferencesUtil.showTimeGap || forceUpdate) {
            clockUpdateSubject.onNext(newState)
        }
    }

    fun publishTimeGap(otherClockMsToGo : Long) {
        if (stateHolder.active != this) {
            // Update the time-gap clock
            updateTimeGap(TimeGapViewState(msToGo - otherClockMsToGo))
        }
    }


    var lastMsOtherClock : Long = 0
    fun subscribeToClock(otherClock : TimerLogic) {
        val ignored = otherClock.msToGoUpdateSubject.subscribe { ms ->
            lastMsOtherClock = ms
            publishTimeGap(ms)
        }
    }

    private fun setInitialTime() {
        msToGo  = (preferencesUtil.initialDurationSeconds * 1000).toLong()
        msDelayToGo = 0
        playedBuzzer = false
        moveCounter = MoveCounter()
    }

    fun initialState() : ButtonViewState {
        val state =  ButtonViewState(buttonIsEnabled(), msToGo, "")
        Timber.d("%s initialState: %s", color, state)
        return state
    }

    fun onMoveStart() {
        msDelayToGo = preferencesUtil.getBronsteinDelayMs()
        updateAndPublishMsToGo(msToGo + preferencesUtil.getFischerDelayMs())
        moveCounter.newMove()

        // This task will publish clock updates to clockUpdateSubject
        resume()

        // Hide the time-gap clock
        updateTimeGap(TimeGapViewState(show = false))
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
        spinner.onNext(SpinnerStateUpdate(0))
        clockUpdateSubject.onNext(ButtonViewState(false, msToGo, ""))
    }

    fun onTimeExpired() {
        moveCounter.updateMoveTime()
    }

    fun reset() {
        disposeTimeSequenceSubscription()
        setInitialTime()
        spinner.onNext(SpinnerStateUpdate(0))
        clockUpdateSubject.onNext(initialState())
        updateTimeGap(TimeGapViewState(show = false))
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
                clockUpdateSubject.onNext(ButtonViewState(true, msToGo, moveCounter.display()))
                spinner.onNext(SpinnerStateUpdate(msDelayToGo))
                true
            } else {
                updateAndPublishMsToGo(msToGo - dt)
                // After decrementing clock, publish new time
                clockUpdateSubject.onNext(ButtonViewState(true, msToGo, moveCounter.display()))
                if (msToGo > 0) {
                    true
                } else {
                    if (!playedBuzzer) {
                        playedBuzzer = true
                        buzzer.onNext(Buzzer())
                    }
                    preferencesUtil.allowNegativeTime // continues update if true
                }
            }
        }
    }

    fun setNewTime(newTime: Long) {
        updateAndPublishMsToGo(newTime)
        clockUpdateSubject.onNext(ButtonViewState(buttonIsEnabled(), msToGo, moveCounter.display()))
    }

    private fun buttonIsEnabled(): Boolean {
        return if (stateHolder.gameState == GameState.NOT_STARTED) {
            true
        } else {
            stateHolder.active == this
        }
    }
}