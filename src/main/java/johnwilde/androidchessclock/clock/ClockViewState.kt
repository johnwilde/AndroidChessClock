package johnwilde.androidchessclock.clock

import johnwilde.androidchessclock.main.Partial

// Used to render a player button, clock, time-gap and move-count
// and a toast (prompt)
data class ClockViewState(
        val button : Button,
        val prompt : Snackbar?,
        val timeGap : TimeGap) : Partial<ClockViewState> {
    override fun reduce(previousState: ClockViewState): ClockViewState {
        return this
    }

    // Player's time remaining, whether button is visible (currently
    // playing or non-started state) and move-count
    data class Button(
            val enabled: Boolean,
            val msToGo: Long,
            val moveCount: MoveCount? = null) : Partial<ClockViewState> {
        override fun reduce(previousState: ClockViewState): ClockViewState {
            return previousState.copy(
                    button = this,
                    prompt = null)
        }
    }

    data class MoveCount(
            val message: Message,
            val count: Int) : Partial<ClockViewState> {
        enum class Message { TOTAL, REMAINING }
        override fun reduce(previousState: ClockViewState): ClockViewState {
            return previousState.copy(
                    button = previousState.button.copy(moveCount = this),
                    prompt = null)
        }
    }

    // The time difference between the player clocks, only show when
    // it is not player's turn
    data class TimeGap(
            val msGap: Long = 0,
            val show: Boolean = true) : Partial<ClockViewState> {
        override fun reduce(previousState: ClockViewState): ClockViewState {
            return previousState.copy(timeGap = this, prompt = null)
        }
    }

    // Publish a toast
    data class Snackbar(
            val message: Message? = null,
            val show: Boolean = false,
            val dismiss: Boolean = false) : Partial<ClockViewState> {
        enum class Message { RESUME, START }
        override fun reduce(previousState: ClockViewState): ClockViewState {
            return previousState.copy(prompt = this)
        }
    }
}



