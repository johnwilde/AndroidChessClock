package johnwilde.androidchessclock.main

import johnwilde.androidchessclock.main.MainViewState.PlayPauseButton.*

data class MainViewState(
        val button : PlayPauseButton,
        val prompt : Snackbar,
        val spinner : Spinner) : Partial<MainViewState> {

    override fun reduce(previousState: MainViewState): MainViewState {
        return this
    }

    companion object {
        @JvmStatic
        val initialState = MainViewState(
                button = PlayPauseButton(State.PLAY, true),
                prompt = Snackbar(show = false, dismiss = true),
                spinner = Spinner(0))
    }

    data class PlayPauseButton(
            val buttonState: State,
            val visible: Boolean = true) : Partial<MainViewState> {
        enum class State {PLAY, PAUSE}
        override fun reduce(previousState: MainViewState): MainViewState {
            return previousState.copy(button = this)
        }
    }

    data class Spinner(
            val msDelayToGo: Long) : Partial<MainViewState> {
        override fun reduce(previousState: MainViewState): MainViewState {
            return previousState.copy(spinner = this)
        }
    }

    // Publish a toast
    data class Snackbar(
            val message: String = "",
            val show: Boolean = false,
            val dismiss: Boolean = false) : Partial<MainViewState> {
        override fun reduce(previousState: MainViewState): MainViewState {
            return previousState.copy(prompt = this)
        }
    }
}


