package johnwilde.androidchessclock

import android.content.Context
import android.content.res.Resources
import android.preference.PreferenceManager
import com.f2prateek.rx.preferences2.RxSharedPreferences
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import johnwilde.androidchessclock.clock.ClockFragment
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.logic.GameStateHolder
import johnwilde.androidchessclock.logic.SystemTime
import johnwilde.androidchessclock.logic.TimeSource
import johnwilde.androidchessclock.logic.TimerLogic
import johnwilde.androidchessclock.main.MainActivity
import johnwilde.androidchessclock.prefs.PreferencesUtil
import johnwilde.androidchessclock.sound.SoundFragment
import javax.inject.Named
import javax.inject.Singleton

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
        internal fun provicesResources(context: Context) : Resources {
            return context.resources
        }

        @JvmStatic
        @Provides
        internal fun providesRxSharedPreferences(context: Context) : RxSharedPreferences {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return RxSharedPreferences.create(preferences)
        }

        @JvmStatic
        @Provides
        internal fun providesTimeSource() : TimeSource { return SystemTime() }

        @JvmStatic
        @Provides
        @Singleton
        internal fun providesGameStateHolder(): GameStateHolder { return GameStateHolder() }

        @JvmStatic
        @Provides
        @Singleton
        @Named("black")
        internal fun providesBlack(
                preferencesUtil: PreferencesUtil,
                stateHolder: GameStateHolder,
                timeSource: TimeSource): TimerLogic {
            return TimerLogic(
                    ClockView.Color.BLACK,
                    preferencesUtil,
                    stateHolder,
                    timeSource)
        }

        @JvmStatic
        @Provides
        @Singleton
        @Named("white")
        internal fun providesWhite(
                preferencesUtil: PreferencesUtil,
                stateHolder: GameStateHolder,
                timeSource: TimeSource): TimerLogic {
            return TimerLogic(
                    ClockView.Color.WHITE,
                    preferencesUtil,
                    stateHolder,
                    timeSource)
        }
    }
}