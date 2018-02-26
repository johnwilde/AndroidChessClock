package johnwilde.androidchessclock.logic

import johnwilde.androidchessclock.clock.ClockViewState
import johnwilde.androidchessclock.prefs.PreferencesUtil
import timber.log.Timber

class MoveCounter(val pref: PreferencesUtil) {
    var count : Int = 0 // number of moves for this player
    val moveTimes = mutableListOf<Long>() // array of time for each move (ms)
    var msToGoMoveStart : Long = 0

    fun newMove(ms : Long, timer : Timer) {
        msToGoMoveStart = ms
        moveTimes.add(0)

        if (pref.timeControlType == PreferencesUtil.TimeControlType.TOURNAMENT) {
            when {
                count < pref.phase1NumberOfMoves -> {
                    val remaining = pref.phase1NumberOfMoves - count
                    publishRemainingMoves(remaining, timer)
                }
                count == pref.phase1NumberOfMoves -> {
                    timer.setNewTime(ms + pref.phase1Minutes * 60 * 1000)
                    publishCurrentMoveCount(count + 1, timer)
                }
                count > pref.phase1NumberOfMoves -> {
                    publishCurrentMoveCount(count + 1, timer)
                }
            }
        } else {
            publishCurrentMoveCount(count + 1, timer)
        }

        count += 1  // the start of the Move
    }

    fun popMove(timer: Timer) {
        msToGoMoveStart += moveTimes.last()
        moveTimes.removeAt(moveTimes.lastIndex)
        count--
        publishCurrentMoveCount(count, timer)
    }

    fun pushMove(ms: Long, timer: Timer) {
        msToGoMoveStart -= ms
        moveTimes.add(ms)
        count++
        publishCurrentMoveCount(count, timer)
    }

    fun publishCurrentMoveCount(count: Int, timer : Timer) {
        timer.clockSubject.onNext(ClockViewState.MoveCount(
                message = ClockViewState.MoveCount.Message.TOTAL,
                count = count)
        )
    }
    fun publishRemainingMoves(remaining : Int, timer: Timer) {
        timer.clockSubject.onNext(ClockViewState.MoveCount(
                message = ClockViewState.MoveCount.Message.REMAINING,
                count = remaining)
        )
    }

    fun updateMoveTime(ms : Long) {
        Timber.d("update move time[%s]: %s", moveTimes.lastIndex, msToGoMoveStart - ms)
        moveTimes[moveTimes.lastIndex] = msToGoMoveStart - ms
    }
}