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
        resume()
    }

    override fun moveEnd() {
        super.moveEnd()
        pause()
        publishInactiveState()
    }

    override fun publishInactiveState() {
        // At end of turn, dim the button
        clockSubject.onNext(ClockViewState.Button(false, msToGo))
    }

    override fun timerTask(): PublishesClockState {
        return UpdateTime()
    }

    inner class UpdateTime : PublishesClockState {
        private var lastUpdateMs: Long = timeSource.currentTimeMillis()

        override fun publishUpdates() {
            val now = timeSource.currentTimeMillis()
            val dt = now - lastUpdateMs
            lastUpdateMs = now

            updateAndPublishMsToGo(msToGo - dt)
            // After decrementing clock, publish new time
            clockSubject.onNext(
                    ClockViewState.Button(
                            enabled = true,
                            msToGo = msToGo)
            )
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