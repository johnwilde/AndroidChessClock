package johnwilde.androidchessclock.logic

import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.main.MainViewState
import johnwilde.androidchessclock.main.Partial
import johnwilde.androidchessclock.prefs.PreferencesUtil

// Responsible for updating an individual clock and publishing new view states
// on each update.
class TimerLogic(val color: ClockView.Color,
                 var preferencesUtil: PreferencesUtil,
                 private var stateHolder : GameStateHolder,
                 var timeSource: TimeSource) {

    lateinit var timerType: BaseTimer

    var msToGoMoveStart : Long = 0

    // Time remaining on this clock, which other clock can subscribe to
    var msToGoUpdateSubject: BehaviorSubject<Long> = BehaviorSubject.create()
    // Player buttons, time and time-gap
    var clockUpdateSubject: PublishSubject<Partial<ClockViewState>> = PublishSubject.create()
    // Updates for view that draws Bronstein-delay circle
    var spinner: BehaviorSubject<Partial<MainViewState>> = BehaviorSubject.create()

    // last update from other clock
    var lastMsOtherClock : Long = 0

    private var moveCounter = MoveCounter()

    val moveTimes get() = moveCounter.moveTimes.toLongArray()
    val msToGo get() = timerType.msToGo

    private inner class MoveCounter {
        var count : Int = 0 // number of moves for this player
        val moveTimes = mutableListOf<Long>() // array of time for each move (ms)

        fun newMove(ms : Long) {
            msToGoMoveStart = ms
            moveTimes.add(0)

            if (preferencesUtil.timeControlType == PreferencesUtil.TimeControlType.TOURNAMENT) {
                when {
                    count < preferencesUtil.phase1NumberOfMoves -> {
                        val remaining = preferencesUtil.phase1NumberOfMoves - count
                        publishRemainingMoves(remaining)
                    }
                    count == preferencesUtil.phase1NumberOfMoves -> {
                        timerType.setNewTime(timerType.msToGo + preferencesUtil.phase1Minutes * 60 * 1000)
                        publishCurrentMoveCount(count + 1)
                    }
                    count > preferencesUtil.phase1NumberOfMoves -> {
                        publishCurrentMoveCount(count + 1)
                    }
                }
            } else {
                publishCurrentMoveCount(count + 1)
            }

            count += 1  // the start of the Move
        }
        fun publishCurrentMoveCount(count: Int) {
            clockUpdateSubject.onNext(ClockViewState.MoveCount(
                    message = ClockViewState.MoveCount.Message.TOTAL,
                    count = count)
            )
        }
        fun publishRemainingMoves(remaining : Int) {
            clockUpdateSubject.onNext(ClockViewState.MoveCount(
                    message = ClockViewState.MoveCount.Message.REMAINING,
                    count = remaining)
            )
        }

        fun updateMoveTime(ms : Long) {
            moveTimes[moveTimes.lastIndex] = ms - msToGoMoveStart
        }

        fun display() : String {
            return count.toString()
        }
    }

    init {
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
                            msGap = timerType.msToGo - otherClockMsToGo,
                            show = true))
        }
    }

    fun initialize(otherClock : TimerLogic) {
        moveCounter = MoveCounter()
        val ignored = otherClock.msToGoUpdateSubject.subscribe { ms ->
            lastMsOtherClock = ms
            publishTimeGap(ms)
        }

        timerType = when (preferencesUtil.timeControlType) {
            PreferencesUtil.TimeControlType.BASIC -> {
                when {
                    preferencesUtil.getFischerDelayMs() > 0 -> {
                        Fischer(preferencesUtil, stateHolder, timeSource, msToGoUpdateSubject, clockUpdateSubject, spinner)
                    }
                    preferencesUtil.getBronsteinDelayMs() > 0 -> {
                        Bronstein(preferencesUtil, stateHolder, timeSource, msToGoUpdateSubject, clockUpdateSubject, spinner)
                    }
                    else -> {
                        Fischer(preferencesUtil, stateHolder, timeSource, msToGoUpdateSubject, clockUpdateSubject, spinner)
                    }
                }
            }
            PreferencesUtil.TimeControlType.TOURNAMENT -> {
                Fischer(preferencesUtil, stateHolder, timeSource, msToGoUpdateSubject, clockUpdateSubject, spinner)
            }
        }
        clockUpdateSubject.onNext(initialState())
    }

    fun onMoveStart() {
        timerType.moveStart()
        moveCounter.newMove(timerType.msToGo)
        // Hide the time-gap clock
        hideTimeGap()
    }

    fun onMoveEnd() {
        timerType.moveEnd()
        moveCounter.updateMoveTime(timerType.msToGo)
    }

    fun resume() {
        timerType.resume()
    }

    fun pause() {
        timerType.pause()
        moveCounter.updateMoveTime(timerType.msToGo)
    }

    fun initialState() : ClockViewState {
        return ClockViewState(
                button = ClockViewState.Button(
                        enabled = true,
                        msToGo = timerType.msToGo),
                timeGap = ClockViewState.TimeGap(show = false),
                prompt = ClockViewState.Snackbar(dismiss = true))
    }

    fun publishInactiveState() {
        clockUpdateSubject.onNext(
                ClockViewState.Button(
                        enabled = false,
                        msToGo = timerType.msToGo)
        )
    }

    fun setNewTime(newTime: Long) {
        timerType.setNewTime(newTime)
    }
}