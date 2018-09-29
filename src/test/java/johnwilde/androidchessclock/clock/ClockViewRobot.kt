package johnwilde.androidchessclock.clock

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class ClockViewRobot(presenter: ClockViewPresenter) {
    val clickSubject = BehaviorSubject.create<Any>()
    val viewStateObservable = PublishSubject.create<ClockViewState>()
    inner class RobotClockView : ClockView {
        override fun clickIntent(): Observable<Any> {
            return clickSubject
        }

        override fun render(viewState: ClockViewState) {
            viewStateObservable.onNext(viewState)
        }
    }
    init {
        presenter.attachView(RobotClockView())
    }
    fun fireClickIntent() {
        clickSubject.onNext(1)
    }
}