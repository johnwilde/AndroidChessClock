
package johnwilde.androidchessclock

import android.content.Context
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import timber.log.Timber

/**
 * A custom Application class mainly used to provide dependency injection
 */
class ChessApplication : DaggerApplication() {

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
    }

    companion object {
        fun getRefWatcher(context: Context): RefWatcher? {
            val application = context.applicationContext as ChessApplication
            return application.refWatcher
        }
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent.builder().create(this)
    }
}

