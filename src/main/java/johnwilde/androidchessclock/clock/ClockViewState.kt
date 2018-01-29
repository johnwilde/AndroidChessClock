package johnwilde.androidchessclock.clock

sealed class ClockViewState
data class FullViewState(
        val button : ButtonViewState,
        val timeGap : TimeGapViewState = TimeGapViewState( 0),
        val prompt : PromptToMove? = null ) : ClockViewState()
data class ButtonViewState(
        val enabled: Boolean,
        val msToGo: Long,
        val moveCount: String) : ClockViewState()
data class PromptToMove(
        val color: ClockView.Color) : ClockViewState()
data class TimeGapViewState(
        val msGap: Long) : ClockViewState()

