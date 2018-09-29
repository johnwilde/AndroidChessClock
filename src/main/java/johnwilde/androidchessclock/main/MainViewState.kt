package johnwilde.androidchessclock.main

import johnwilde.androidchessclock.main.MainViewState.PlayPauseButton.*

data class MainViewState(
    val button: PlayPauseButton,
    val prompt: Snackbar,
    val spinner: Spinner,
    val takeBack: TakeBack
) : Partial<MainViewState> {

    override fun reduce(previousState: MainViewState): MainViewState {
        return this
    }

    companion object {
        @JvmStatic
        val initialState = MainViewState(
                button = PlayPauseButton(State.NEW),
                prompt = Snackbar(show = false, dismiss = true),
                spinner = Spinner(0),
                takeBack = TakeBack(false, false))
    }

    data class PlayPauseButton(
        val buttonState: State
    ) : Partial<MainViewState> {
        enum class State { NEW, PLAY, PAUSE, FINISHED }
        override fun reduce(previousState: MainViewState): MainViewState {
            return previousState.copy(button = this)
        }
    }

    data class Spinner(
        val msDelayToGo: Long
    ) : Partial<MainViewState> {
        override fun reduce(previousState: MainViewState): MainViewState {
            return previousState.copy(spinner = this)
        }
    }

    data class TakeBack(
        val backEnabled: Boolean,
        val forwardEnabled: Boolean
    ) : Partial<MainViewState> {
        override fun reduce(previousState: MainViewState): MainViewState {
            return previousState.copy(takeBack = this)
        }
    }

    // Publish a toast
    data class Snackbar(
        val message: Message? = null,
        val show: Boolean = false,
        val dismiss: Boolean = false
    ) : Partial<MainViewState> {
        enum class Message { WHITE_LOST, BLACK_LOST }
        override fun reduce(previousState: MainViewState): MainViewState {
            return previousState.copy(prompt = this)
        }
    }
}

