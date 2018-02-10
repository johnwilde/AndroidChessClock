package johnwilde.androidchessclock.clock

import johnwilde.androidchessclock.main.Partial

// Used to render a player button, clock, time-gap and move-count
// and a toast (prompt)
data class ClockViewState(
        val button : Button,
        val timeGap : TimeGap) : Partial<ClockViewState> {
    override fun reduce(previousState: ClockViewState): ClockViewState {
        return this
    }

    // Player's time remaining, whether button is visible (currently
    // playing or non-started state) and move-count
    data class Button(
            val enabled: Boolean,
            val msToGo: Long,
            val moveCount: String) : Partial<ClockViewState> {
        override fun reduce(previousState: ClockViewState): ClockViewState {
            // Don't change timegap state
            return ClockViewState(
                    button = this,
                    timeGap = previousState.timeGap)
        }
    }

    // The time difference between the player clocks, only show when
    // it is not player's turn
    data class TimeGap(
            val msGap: Long = 0,
            val show: Boolean = true) : Partial<ClockViewState> {
        override fun reduce(previousState: ClockViewState): ClockViewState {
            // Don't change button state
            return ClockViewState(
                    button = previousState.button,
                    timeGap = this)
        }
    }
}



