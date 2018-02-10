package johnwilde.androidchessclock.main

import com.hannesdorfmann.mosby3.mvp.MvpView
import io.reactivex.Observable

interface PlayPauseView : MvpView {
    fun drawerOpened() : Observable<Any>
    fun playPauseIntent() : Observable<Any>
    fun snackBarDismissed() : Observable<Any>
    fun render(viewState: MainViewState)
}