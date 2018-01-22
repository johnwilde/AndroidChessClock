package johnwilde.androidchessclock.main

sealed class PlayPauseViewState
data class PlayPauseState(
        val showPlay: Boolean,
        val showDialog: Boolean,
        val enabled: Boolean = true) : PlayPauseViewState()
data class SpinnerViewState(
        val msDelayToGo: Long) : PlayPauseViewState()
