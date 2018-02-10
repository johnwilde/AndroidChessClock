package johnwilde.androidchessclock.main

import johnwilde.androidchessclock.main.MainViewState.PlayPauseButton.*

interface MainStateUpdate {
    fun reduce(previousState: MainViewState) : MainViewState
}

data class MainViewState(
        val button : PlayPauseButton,
        val prompt : Snackbar,
        val spinner : Spinner)  : MainStateUpdate {
    interface Update{
        fun reduce(previousState: MainViewState) : MainViewState
    }
    companion object {
        @JvmStatic
        val initialState = MainViewState(
                button = PlayPauseButton(State.PLAY, true),
                prompt = Snackbar(show = false, dismiss = true),
                spinner = Spinner(0))
    }

    override fun reduce(previousState: MainViewState): MainViewState {
        return this
    }

    data class PlayPauseButton(
            val buttonState: State,
            val visible: Boolean = true) : MainStateUpdate {
        enum class State {PLAY, PAUSE}
        override fun reduce(previousState: MainViewState): MainViewState {
            return previousState.copy(button = this)
        }
    }

    data class Spinner(
            val msDelayToGo: Long) : MainStateUpdate {
        override fun reduce(previousState: MainViewState): MainViewState {
            return previousState.copy(spinner = this)
        }
    }

    // Publish a toast
    data class Snackbar(
            val message: String = "",
            val show: Boolean = false,
            val dismiss: Boolean = false) : MainStateUpdate {
        override fun reduce(previousState: MainViewState): MainViewState {
            return previousState.copy(prompt = this)
        }

    }
}


