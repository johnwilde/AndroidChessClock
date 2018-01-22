
package johnwilde.androidchessclock

import android.app.Application
import android.content.Context
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import johnwilde.androidchessclock.DependencyInjection
import timber.log.Timber

/**
 * A custom Application class mainly used to provide dependency injection
 */
class ChessApplication : Application() {

    lateinit var dependencyInjection : DependencyInjection

    init {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private var refWatcher: RefWatcher? = null

    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        refWatcher = LeakCanary.install(this)
        Timber.d("Starting Application")
        dependencyInjection = DependencyInjection(this)
    }

    companion object {

        fun getDependencyInjection(context: Context): DependencyInjection {
            return (context.applicationContext as ChessApplication).dependencyInjection
        }


        fun getRefWatcher(context: Context): RefWatcher? {
            val application = context.applicationContext as ChessApplication
            return application.refWatcher
        }
    }
}

