package johnwilde.androidchessclock.logic

import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.prefs.PreferencesUtil

class Fischer(color: ClockView.Color,
              preferencesUtil: PreferencesUtil,
              stateHolder : GameStateHolder,
              timeSource: TimeSource)
    : Timer(color, preferencesUtil, stateHolder, timeSource) {

    init {
        updateAndPublishMsToGo(preferencesUtil.initialDurationSeconds * 1000.toLong())
    }

    override fun moveStart() {
        super.moveStart()
        updateAndPublishMsToGo(msToGo + preferencesUtil.getFischerDelayMs())
        start()
    }

    override fun moveEnd() {
        super.moveEnd()
        stop()
        publishInactiveState()
    }

    override fun timerTask(): PublishesClockState {
        return object : PublishesClockState {
            private var lastUpdateMs: Long = timeSource.currentTimeMillis()

            override fun publish() {
                val now = timeSource.currentTimeMillis()
                val dt = now - lastUpdateMs
                lastUpdateMs = now

                updateAndPublishMsToGo(msToGo - dt)
                // After decrementing clock, publish new time
                clockSubject.onNext(
                        ClockViewState.Time(msToGo = msToGo)
                )
            }
        }
    }

    override fun setNewTime(newTime: Long) {
        updateAndPublishMsToGo(newTime)
        clockSubject.onNext(
                ClockViewState.Time(msToGo = msToGo)
                )
    }
}