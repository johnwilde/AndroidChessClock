package johnwilde.androidchessclock.logic

import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import io.reactivex.internal.schedulers.ExecutorScheduler
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.Executor


class TestSchedulerRule : TestRule {
    private val immediate = object : Scheduler() {
        override fun createWorker(): Scheduler.Worker {
            return ExecutorScheduler.ExecutorWorker(Executor { it.run() })
        }
    }
    val testScheduler = TestScheduler()

    override fun apply(base: Statement, d: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                RxJavaPlugins.setIoSchedulerHandler { scheduler -> testScheduler }
                RxJavaPlugins.setComputationSchedulerHandler { scheduler -> testScheduler }
                RxJavaPlugins.setNewThreadSchedulerHandler { scheduler -> testScheduler }
                RxAndroidPlugins.setMainThreadSchedulerHandler { scheduler -> immediate }

                // https@ //github.com/peter-tackage/rxjava2-scheduler-examples/blob/master/main-thread-example/app/src/test/java/com/petertackage/rxjava2scheduling/MainPresenterTest.java
                RxAndroidPlugins.setInitMainThreadSchedulerHandler { scheduler -> Schedulers.trampoline() }

                try {
                    base.evaluate()
                } finally {
                    RxJavaPlugins.reset()
                    RxAndroidPlugins.reset()
                }
            }
        }
    }
}