package johnwilde.androidchessclock.logic

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import johnwilde.androidchessclock.clock.ButtonViewState
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.clock.TimeGapViewState
import johnwilde.androidchessclock.main.PlayPauseViewState
import johnwilde.androidchessclock.main.SpinnerViewState
import johnwilde.androidchessclock.prefs.PreferencesUtil
import johnwilde.androidchessclock.sound.Buzzer
import johnwilde.androidchessclock.sound.SoundViewState
import timber.log.Timber
import java.util.concurrent.TimeUnit

class TimerLogic(val manager: ClockManager,
                 val color: ClockView.Color,
                 val preferencesUtil: PreferencesUtil,
                 val timeSource: TimeSource) {
    var moveCount : Int = 0
    var msToGo : Long = 0
    var msDelayToGo: Long = 0
    var msToGoMoveStart : Long = 0
    var playedBuzzer : Boolean = false
    val moveTimes : ArrayList<Long> = ArrayList()

    private var msToGoUpdateSubject: BehaviorSubject<Long> = BehaviorSubject.create()
    // Player buttons, time and time-gap
    var clockUpdateSubject: PublishSubject<ClockViewState> = PublishSubject.create()
    // Updates for view that draws Bronstein-delay circle
    var spinner: BehaviorSubject<PlayPauseViewState> = BehaviorSubject.create()
    // When time runs out send an update
    var buzzer: BehaviorSubject<SoundViewState> = BehaviorSubject.create()

    private var clockTask = UpdateTime()
    private var clockSubscription: Disposable? = null

    init {
        setInitialTime()
    }

    fun subscribeToOtherClock() {
        val ignored = manager.forOtherColor(color).msToGoUpdateSubject.subscribe { ms ->
            if (manager.active != this) {
                // Update the time-gap clock
                clockUpdateSubject.onNext(TimeGapViewState(msToGo - ms))
            }
        }
    }

    private fun setInitialTime() {
        msToGo  = (preferencesUtil.initialDurationSeconds * 1000).toLong()
        msDelayToGo = 0
        moveCount = 0
        playedBuzzer = false
        moveTimes.clear()
    }

    fun initialState() : ButtonViewState {
        val enabled = if (manager.gameState == ClockManager.GameState.NOT_STARTED) true else manager.active == this
        val state =  ButtonViewState(enabled, msToGo, "")
        Timber.d("%s initialState: %s", color, state)
        return state
    }

    fun updateAndPublishMsToGo(newValue: Long) {
        msToGo = newValue
        msToGoUpdateSubject.onNext(newValue)
    }

    fun onMoveStart() {
        msDelayToGo = preferencesUtil.getBronsteinDelayMs()
        updateAndPublishMsToGo(msToGo + preferencesUtil.getFischerDelayMs())
        moveCount += 1
        msToGoMoveStart = msDelayToGo + msToGo
        // This task will publish clock updates to clockUpdateSubject
        resume()
        // Hide the time-gap clock
        clockUpdateSubject.onNext(TimeGapViewState( 0))
    }

    fun onMoveEnd() {
        pause()
        val msMoveTime = msToGoMoveStart - msDelayToGo - msToGo
        moveTimes.add(msMoveTime)
        if (preferencesUtil.timeControlType != PreferencesUtil.TimeControlType.BASIC) {
            if (moveCount == preferencesUtil.phase1NumberOfMoves) {
                updateAndPublishMsToGo(msToGo + preferencesUtil.phase1Minutes * 60 * 1000)
            }
        }
        // At end of turn, dim the button and remove the spinner
        publishMoveEnd()
    }

    fun publishMoveEnd() {
        spinner.onNext(SpinnerViewState(0))
        clockUpdateSubject.onNext(ButtonViewState(false, msToGo, ""))
    }

    fun reset() {
        pause()
        setInitialTime()
        spinner.onNext(SpinnerViewState(0))
        clockUpdateSubject.onNext(initialState())
        clockUpdateSubject.onNext(TimeGapViewState( 0))
    }

    fun resume() {
        Timber.d("Start new subscription to timer state")
        clockSubscription = timeSequence().subscribe()
    }

    fun pause() {
        if (clockSubscription != null && !clockSubscription!!.isDisposed()) {
            Timber.d("Disposing subscription to timer state")
            // This stops the timer interval
            clockSubscription!!.dispose()
        }
    }

    private fun timeSequence() : Observable<Any> {
        clockTask = UpdateTime()
        return Observable.interval(0, 100, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .map { if (!clockTask.publishUpdates()) pause() }
    }

    private inner class UpdateTime {
        private var lastUpdateMs: Long = timeSource.currentTimeMillis()

        fun publishUpdates(): Boolean {
            val now = timeSource.currentTimeMillis()
            val dt = now - lastUpdateMs
            lastUpdateMs = now

            return if (msDelayToGo > 0) {
                // While in Bronstein period, we just decrement delay time
                msDelayToGo -= dt
                clockUpdateSubject.onNext(ButtonViewState(true, msToGo, moveCount.toString()))
                spinner.onNext(SpinnerViewState(msDelayToGo))
                true
            } else {
                updateAndPublishMsToGo(msToGo - dt)
                // After decrementing clock, publish new time
                clockUpdateSubject.onNext(ButtonViewState(true, msToGo, moveCount.toString()))
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
        val enabled = if (manager.gameState == ClockManager.GameState.NOT_STARTED) true else manager.active == this
        updateAndPublishMsToGo(newTime)
        clockUpdateSubject.onNext(ButtonViewState(enabled, msToGo, moveCount.toString()))
    }
}