package johnwilde.androidchessclock.logic

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.main.MainViewState
import johnwilde.androidchessclock.main.Partial
import johnwilde.androidchessclock.prefs.PreferencesUtil
import java.util.concurrent.TimeUnit

class Bronstein(
        var preferencesUtil : PreferencesUtil,
        var stateHolder : GameStateHolder,
        var timeSource : TimeSource,
        val timeSubject: BehaviorSubject<Long>,
        val clockSubject: PublishSubject<Partial<ClockViewState>>,
        val mainSubject: BehaviorSubject<Partial<MainViewState>>)
    : BaseTimer {

    override var msToGo : Long = 0
    var delay: Long = 0
    private var clockSubscription: Disposable? = null

    init {
        updateAndPublishMsToGo(preferencesUtil.initialDurationSeconds * 1000.toLong())
        delay = 0
    }

    override fun moveStart() {
        delay = preferencesUtil.getBronsteinDelayMs()
        updateAndPublishMsToGo(msToGo)
        resume()
    }

    override fun moveEnd() {
        pause()
        publishInactiveState()
    }

    override fun pause() {
        disposeTimeSequenceSubscription()
    }

    override fun resume() {
        clockSubscription = timeSequence().subscribe()
    }

    private fun publishInactiveState() {
        // At end of turn, dim the button
        clockSubject.onNext(ClockViewState.Button(false, msToGo))
        mainSubject.onNext(MainViewState.Spinner(0))
    }

    private fun updateAndPublishMsToGo(newValue: Long) {
        msToGo = newValue
        timeSubject.onNext(newValue)
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
                .map { clockTask.publishUpdates() }
    }

    private inner class UpdateTime {
        private var lastUpdateMs: Long = timeSource.currentTimeMillis()

        fun publishUpdates() {
            val now = timeSource.currentTimeMillis()
            val dt = now - lastUpdateMs
            lastUpdateMs = now

            return if (delay > 0) {
                // While in Bronstein period, we just decrement delay time
                delay -= dt
                updateAndPublishMsToGo(msToGo)
                clockSubject.onNext(
                        ClockViewState.Button(
                                enabled = true,
                                msToGo = msToGo)
                )
                mainSubject.onNext(MainViewState.Spinner(delay))
            } else {
                updateAndPublishMsToGo(msToGo - dt)
                // After decrementing clock, publish new time
                clockSubject.onNext(
                        ClockViewState.Button(
                                enabled = true,
                                msToGo = msToGo)
                )
            }
        }
    }

    override fun setNewTime(newTime: Long) {
        updateAndPublishMsToGo(newTime)
        clockSubject.onNext(
                ClockViewState.Button(
                        enabled = buttonIsEnabled(),
                        msToGo = msToGo)
        )
    }

    private fun buttonIsEnabled(): Boolean {
        return if (stateHolder.gameState == GameStateHolder.GameState.NOT_STARTED) {
            true
        } else {
            stateHolder.active?.timerType == this
        }
    }
}