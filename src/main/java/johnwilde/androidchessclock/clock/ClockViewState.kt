package johnwilde.androidchessclock.clock

sealed class ClockViewState
data class ButtonViewState(
        val enabled: Boolean,
        val msToGo: Long,
        val moveCount: String) : ClockViewState()
data class PromptToMove(
        val color: ClockView.Color) : ClockViewState()
data class TimeGapViewState(
        val enabled: Boolean,
        val msGap: Long) : ClockViewState()
class DoNothing : ClockViewState()

