package johnwilde.androidchessclock.clock

interface PartialState {
    fun reduce(previousState : ClockViewState) : ClockViewState
}

// Used to render a player button, clock, time-gap and move-count
// and a toast (prompt)
data class ClockViewState(
        val button : ButtonViewState,
        val timeGap : TimeGapViewState) : PartialState {
    override fun reduce(previousState: ClockViewState): ClockViewState {
        return this
    }
}

// Player's time remaining, whether button is visible (currently
// playing or non-started state) and move-count
data class ButtonViewState(
        val enabled: Boolean,
        val msToGo: Long,
        val moveCount: String) :  PartialState {
    override fun reduce(previousState: ClockViewState): ClockViewState {
        // Don't change timegap state
        return ClockViewState(
                button = this,
                timeGap = previousState.timeGap)
    }
}

// The time difference between the player clocks, only show when
// it is not player's turn
data class TimeGapViewState(
        val msGap: Long = 0,
        val show: Boolean = true) :  PartialState {
    override fun reduce(previousState: ClockViewState): ClockViewState {
        // Don't change button state
        return ClockViewState(
                button = previousState.button,
                timeGap = this)
    }
}

