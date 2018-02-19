package johnwilde.androidchessclock.logic

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.prefs.PreferencesUtil
import timber.log.Timber

class Hourglass(color: ClockView.Color,
                preferencesUtil: PreferencesUtil,
                val stateHolder : GameStateHolder,
                timeSource: TimeSource)
    : Timer(color, preferencesUtil, stateHolder, timeSource) {

    private lateinit var lastMsOtherClock : GameStateHolder.TimeUpdate
    private val inactiveDisposable = CompositeDisposable()
    private val timeDisposable = CompositeDisposable()
    private val gameStateDisposable = CompositeDisposable()

    init {
        // Save last time update from other clock
        timeDisposable.add(stateHolder.timeSubject
                .filter { u -> u.color != color }
                .map { u -> lastMsOtherClock = u }
                .observeOn(Schedulers.computation())
                .subscribe()
        )

        // When game starts...
        if (color == ClockView.Color.BLACK) {
            stateHolder.gameStateSubject
                    .filter { it.isUnderway() }
                    .take(1)
                    .map { subscribeWhileInactive() }
                    .observeOn(Schedulers.computation())
                    .subscribe()
        }

        // When game pauses or resumes...
        gameStateDisposable.add(stateHolder.gameStateSubject
                .scan { previous, new ->
                    when (previous) {
                       GameStateHolder.GameState.PAUSED ->
                           if (new.clockMoving() && stateHolder.active != this) {
                               // Start new subscription when game is resumed
                               subscribeWhileInactive()
                           }
                        else -> {}
                    }
                    when (new) {
                        // When game is paused user can adjust their time, we don't want
                        // to receive the updated time so clear subscription on stop
                        GameStateHolder.GameState.PAUSED -> inactiveDisposable.clear()
                        else -> {}
                    }
                    // `scan` was just used to process the previous and new states
                    new
                }
                .subscribe()
        )

        updateAndPublishMsToGo(preferencesUtil.initialDurationSeconds * 1000.toLong())
    }

    private fun subscribeWhileInactive() {
        inactiveDisposable.add(stateHolder.timeSubject
                // only look for updates from other clock while it is running
                .filter { u -> u.color != color }
                .filter { stateHolder.gameState.clockMoving() }
                .scan{ previous, new ->
                    // Just add delta from previous and current state
                    setNewTime(msToGo + previous.ms - new.ms)
                    new
                }
                // Start with the last time we observed (initial time or after a time adjustment)
                .startWith(lastMsOtherClock)
                .subscribe())
    }

    override fun moveStart() {
        super.moveStart()
        start()
    }

    override fun moveEnd() {
        super.moveEnd()
        stop()
        publishInactiveState()
    }

    override fun stop() {
        super.stop()
        subscribeWhileInactive()
    }

    override fun start() {
        super.start()
        // when resumed we ignore other clock
        inactiveDisposable.clear()
    }

    override fun destroy() {
        super.destroy()
        inactiveDisposable.clear()
        timeDisposable.clear()
        gameStateDisposable.clear()
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
                clockSubject.onNext(ClockViewState.Time(msToGo = msToGo))
            }
        }
    }

    override fun setNewTime(newTime: Long) {
        updateAndPublishMsToGo(newTime)
        clockSubject.onNext(ClockViewState.Time(msToGo = msToGo))
    }
}