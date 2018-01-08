package johnwilde.androidchessclock.clock

import android.view.MotionEvent
import com.hannesdorfmann.mosby3.mvp.MvpView
import io.reactivex.Observable

interface ClockView : MvpView {
    enum class Color {WHITE, BLACK }

    // Observable that triggers intent to stop the clock
    fun clickIntent() : Observable<MotionEvent>
    fun render(viewState: ClockViewState)
}