package johnwilde.androidchessclock

import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import johnwilde.androidchessclock.clock.ClockFragment
import johnwilde.androidchessclock.logic.ClockManager
import johnwilde.androidchessclock.logic.SystemTime
import johnwilde.androidchessclock.logic.TimeSource
import johnwilde.androidchessclock.main.MainActivity
import johnwilde.androidchessclock.prefs.PreferencesUtil
import johnwilde.androidchessclock.sound.SoundFragment

@Module
internal abstract class ActivityBindingModule {

    @ContributesAndroidInjector()
    internal abstract fun mainActivity(): MainActivity

    @ContributesAndroidInjector()
    internal abstract fun barChartActivity(): BarChartActivity

    @ContributesAndroidInjector()
    internal abstract fun clockFragment(): ClockFragment

    @ContributesAndroidInjector()
    internal abstract fun soundFragment(): SoundFragment

    @Module
    companion object {
        @JvmStatic
        @Provides
        internal fun providesTimeSource() : TimeSource { return SystemTime() }
    }

}