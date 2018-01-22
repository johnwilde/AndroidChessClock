package johnwilde.androidchessclock.sound

import com.hannesdorfmann.mosby3.mvp.MvpView

interface SoundView : MvpView {
    fun render(viewState: SoundViewState)
}