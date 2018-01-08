package johnwilde.androidchessclock.clock

sealed class ClockViewState
data class ButtonViewState(
        val enabled: Boolean,
        val msToGo: Long,
        val moveCount: String) : ClockViewState()
data class PromptToMove(
        val color: ClockView.Color) : ClockViewState()
class DoNothing : ClockViewState()

