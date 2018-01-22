package johnwilde.androidchessclock.logic

import android.os.Handler
import android.os.SystemClock
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import johnwilde.androidchessclock.clock.ButtonViewState
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.main.PlayPauseViewState
import johnwilde.androidchessclock.main.SpinnerViewState
import johnwilde.androidchessclock.prefs.PreferencesUtil
import johnwilde.androidchessclock.sound.Buzzer
import johnwilde.androidchessclock.sound.SoundViewState

const val POST_FAST : Long = 100
class TimerLogic(val manager: ClockManager,
                 val color: ClockView.Color,
                 val preferencesUtil: PreferencesUtil) {
    var moveCount : Int = 0
    var msToGo : Long = 0
    var msDelayToGo: Long = 0
    var msToGoMoveStart : Long = 0
    var playedBuzzer : Boolean = false
    val moveTimes : ArrayList<Long> = ArrayList()
    var handler = Handler()
    var clockUpdateSubject: BehaviorSubject<ClockViewState> = BehaviorSubject.create()
    var spinner: BehaviorSubject<PlayPauseViewState> = BehaviorSubject.create()
    var buzzer: BehaviorSubject<SoundViewState> = BehaviorSubject.create()

    private var updateTimeTask: UpdateTimeTask? = null

    init {
        setInitialTime()
    }

    private fun setInitialTime() {
        msToGo  = (preferencesUtil.initialDurationSeconds * 1000).toLong()
        msDelayToGo = 0
        moveCount = 0
        playedBuzzer = false
        updateTimeTask = null
        moveTimes.clear()
    }

    fun initialState() : ButtonViewState {
        val enabled = if (manager.gameState == ClockManager.GameState.NOT_STARTED) true else manager.active == this
        return ButtonViewState(enabled, msToGo, "")
    }

    fun onMoveStart() {
        msDelayToGo = preferencesUtil.getBronsteinDelayMs()
        msToGo += preferencesUtil.getFischerDelayMs()
        moveCount += 1
        msToGoMoveStart = msDelayToGo + msToGo
        // This task will publish clock updates to clockUpdateSubject
        updateTimeTask = UpdateTimeTask()
        handler.post(updateTimeTask)
    }

    fun pause() {
        if (updateTimeTask != null) {
            updateTimeTask!!.kill()
        }
    }

    fun resume() {
        // This task will publish clock updates to clockUpdateSubject
        updateTimeTask = UpdateTimeTask()
        handler.post(updateTimeTask)
    }

    fun onMoveEnd() : Observable<ClockViewState> {
        if (updateTimeTask != null) {
            updateTimeTask!!.kill()
        }
        val msMoveTime = msToGoMoveStart - msDelayToGo - msToGo
        moveTimes.add(msMoveTime)
        if (preferencesUtil.timeControlType != PreferencesUtil.TimeControlType.BASIC) {
            if (moveCount == preferencesUtil.phase1NumberOfMoves) {
                msToGo += preferencesUtil.phase1Minutes * 60 * 1000
            }
        }
        // At end of turn, dim the button and remove the spinner
        return getMoveEndObservables()
    }

    fun getMoveEndObservables() : Observable<ClockViewState> {
        spinner.onNext(SpinnerViewState(0))
        return Observable.just(ButtonViewState(false, msToGo, ""))
    }

    fun publishMoveEnd() {
        for (o in getMoveEndObservables().blockingIterable()) {
            clockUpdateSubject.onNext(o)
        }
    }

    fun reset() {
        handler.removeCallbacks(updateTimeTask)
        setInitialTime()
        spinner.onNext(SpinnerViewState(0))
        clockUpdateSubject.onNext(initialState())
    }

    // this class will update itself (and call updateTimerText) accordingly:
    // if getMsToGo() > 10 * 1000, every 1000 ms
    // if getMsToGo() < 10 * 1000, every 100 ms
    // if getMsToGo() < 0 and getAllowNegativeTime is true, every 1000  ms
    internal inner class UpdateTimeTask : Runnable {
        var lastUpdateMs: Long =  SystemClock.uptimeMillis()

        fun publishUpdates() : Boolean {
            val now = SystemClock.uptimeMillis()
            val dt = now - lastUpdateMs
            lastUpdateMs = now

            return if (msDelayToGo > 0) {
                // While in Bronstein period, we just decrement delay time
                msDelayToGo -= dt
                clockUpdateSubject.onNext(ButtonViewState(true, msToGo, moveCount.toString()))
                spinner.onNext(SpinnerViewState(msDelayToGo))
                true
            } else {
                msToGo -= dt
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

        override fun run() {
            if (publishUpdates()) handler.postDelayed(updateTimeTask, POST_FAST)
        }

        fun kill() {
            publishUpdates()
            handler.removeCallbacks(this)
        }
    }

    fun setNewTime(newTime: Long) {
        val enabled = if (manager.gameState == ClockManager.GameState.NOT_STARTED) true else manager.active == this
        msToGo = newTime
        clockUpdateSubject.onNext(ButtonViewState(enabled, msToGo, moveCount.toString()))
    }
}