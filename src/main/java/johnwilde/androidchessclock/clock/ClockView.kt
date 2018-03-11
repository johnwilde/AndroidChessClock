package johnwilde.androidchessclock.clock

import com.hannesdorfmann.mosby3.mvp.MvpView
import io.reactivex.Observable

interface ClockView : MvpView {
    enum class Color { WHITE, BLACK }

    // Observable that triggers intent to stop the clock
    fun clickIntent() : Observable<Any>
    fun render(viewState: ClockViewState)
}