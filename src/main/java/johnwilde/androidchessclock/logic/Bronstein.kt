package johnwilde.androidchessclock.logic

import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.main.MainViewState
import johnwilde.androidchessclock.prefs.PreferencesUtil

class Bronstein(color: ClockView.Color,
                preferencesUtil: PreferencesUtil,
                stateHolder : GameStateHolder,
                timeSource: TimeSource)
    : Timer(color, preferencesUtil, stateHolder, timeSource) {

    var delay: Long = 0

    init {
        updateAndPublishMsToGo(preferencesUtil.initialDurationSeconds * 1000.toLong())
        delay = 0
    }

    override fun moveStart() {
        super.moveStart()
        delay = preferencesUtil.getBronsteinDelayMs()
        updateAndPublishMsToGo(msToGo)
        resume()
    }

    override fun moveEnd() {
        super.moveEnd()
        pause()
        publishInactiveState()
    }

    override fun publishInactiveState() {
        super.publishInactiveState()
        mainSubject.onNext(MainViewState.Spinner(0))
    }

    override fun timerTask(): PublishesClockState {
        return UpdateTime()
    }

    private inner class UpdateTime : PublishesClockState {
        private var lastUpdateMs: Long = timeSource.currentTimeMillis()

        override fun publishUpdates() {
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
}