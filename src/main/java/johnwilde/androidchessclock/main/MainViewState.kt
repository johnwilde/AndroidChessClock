package johnwilde.androidchessclock.main

import johnwilde.androidchessclock.main.MainButtonStateUpdate.*

interface MainStateUpdate {
    fun reduce(previousState: MainViewState) : MainViewState
}

class MainViewState(
        val button : MainButtonStateUpdate,
        val prompt : SnackbarPromptUpdate,
        val spinner : SpinnerStateUpdate)  : MainStateUpdate {
    companion object {
        @JvmStatic
        val initialState = MainViewState(
                button = MainButtonStateUpdate(State.PLAY, true),
                prompt = SnackbarPromptUpdate(show = false, dismiss = true),
                spinner = SpinnerStateUpdate(0))
    }
    override fun reduce(previousState: MainViewState): MainViewState {
        return this
    }
}

data class MainButtonStateUpdate(
        val buttonState: State,
        val visible: Boolean = true) : MainStateUpdate {
    enum class State {PLAY, PAUSE}
    override fun reduce(previousState: MainViewState): MainViewState {
        return MainViewState(
                button = this,
                prompt = previousState.prompt,
                spinner = previousState.spinner
        )
    }
}

data class SpinnerStateUpdate(
        val msDelayToGo: Long) : MainStateUpdate {
    override fun reduce(previousState: MainViewState): MainViewState {
        return MainViewState(
                button = previousState.button,
                prompt = previousState.prompt,
                spinner = this)
    }
}

// Publish a toast
data class SnackbarPromptUpdate(
        val message: String = "",
        val show: Boolean = false,
        val dismiss: Boolean = false) : MainStateUpdate {
    override fun reduce(previousState: MainViewState): MainViewState {
        return MainViewState(
                button = previousState.button,
                prompt = this,
                spinner = previousState.spinner)
    }

}

