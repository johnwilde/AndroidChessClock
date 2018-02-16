package johnwilde.androidchessclock.prefs

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import johnwilde.androidchessclock.R
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesUtil @Inject constructor(
        context: Context,
        rxSharedPreferences: RxSharedPreferences) {
    val sharedPreferences : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    // Clock subscribes to this so it can update view state when it changes
    val timeGap = rxSharedPreferences
            .getBoolean(TimerPreferenceFragment.Key.TIME_GAP.toString())
            .asObservable()

    var showTimeGap: Boolean = true
        get() = sharedPreferences.getBoolean(TimerPreferenceFragment.Key.TIME_GAP.toString(), true)

    var allowNegativeTime: Boolean = false
        get() = sharedPreferences.getBoolean(TimerPreferenceFragment.Key.NEGATIVE_TIME.toString(), false)

    var playBuzzerAtEnd: Boolean = false
        get() = sharedPreferences.getBoolean(TimerPreferenceFragment.Key.PLAY_BELL.toString(), false)

    var playSoundOnButtonTap: Boolean = false
        get() = sharedPreferences.getBoolean(TimerPreferenceFragment.Key.PLAY_CLICK.toString(), false)

    enum class TimeControlType {
        BASIC, TOURNAMENT, HOURGLASS
    }
    internal enum class DelayType {
        FISCHER, BRONSTEIN
    }
    var timeControlType: TimeControlType = TimeControlType.BASIC
    private var delayType: DelayType = DelayType.FISCHER

    var phase1NumberOfMoves: Int = 0
    var phase1Minutes: Int = 0
    var initialDurationSeconds: Int = 0
    private var mIncrementSeconds: Int = 0

    init {
        // set default values (for first run)
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false)
        loadTimeControlPreferences()
    }

    // Run after the user has changeda preference and during onCreate()
    fun loadTimeControlPreferences() {
        val tournament = sharedPreferences.getBoolean(
                TimerPreferenceFragment.Key.TOURNAMENT_TIME_CONTROL_CHECKBOX.toString(),
                false)

        val hourglass = sharedPreferences.getBoolean(
                TimerPreferenceFragment.Key.HOURGLASS_TIME_CONTROL_CHECKBOX.toString(),
                false)

        when {
            hourglass -> {
                timeControlType = TimeControlType.HOURGLASS
                val minutes = getTimerOptionsValue(TimerPreferenceFragment.Key.HOURGLASS_MINUTES)
                val seconds = getTimerOptionsValue(TimerPreferenceFragment.Key.HOURGLASS_SECONDS)
                initialDurationSeconds = minutes * 60 + seconds
            }
            tournament -> {
                timeControlType = TimeControlType.TOURNAMENT
                loadAdvancedTimeControlUserPreference()
            }
            else -> {
                timeControlType = TimeControlType.BASIC
                loadBasicTimeControlUserPreference()
            }
        }
    }

    private fun loadBasicTimeControlUserPreference() {
        loadInitialTimeUserPreferences()
        loadIncrementUserPreference(TimerPreferenceFragment.Key.INCREMENT_SECONDS)
        loadDelayTypeUserPreference(TimerPreferenceFragment.Key.DELAY_TYPE)
    }

    private fun loadAdvancedTimeControlUserPreference() {

        val minutes1 = getTimerOptionsValue(TimerPreferenceFragment.Key.FIDE_MIN_PHASE1)

        initialDurationSeconds = minutes1 * 60

        phase1NumberOfMoves = getTimerOptionsValue(TimerPreferenceFragment.Key.FIDE_MOVES_PHASE1)
        phase1Minutes = getTimerOptionsValue(TimerPreferenceFragment.Key.FIDE_MIN_PHASE2)

        loadDelayTypeUserPreference(TimerPreferenceFragment.Key.ADV_DELAY_TYPE)
        loadIncrementUserPreference(TimerPreferenceFragment.Key.ADV_INCREMENT_SECONDS)
    }


    private fun loadDelayTypeUserPreference(key: TimerPreferenceFragment.Key) {
        val delayTypeString = sharedPreferences.getString(key.toString(),
                "FISCHER")
        delayType = DelayType.valueOf(delayTypeString!!.toUpperCase())
    }

    private fun loadIncrementUserPreference(key: TimerPreferenceFragment.Key) {
        mIncrementSeconds = getTimerOptionsValue(key)
    }

    private fun loadInitialTimeUserPreferences() {
        val minutes = getTimerOptionsValue(TimerPreferenceFragment.Key.MINUTES)
        val seconds = getTimerOptionsValue(TimerPreferenceFragment.Key.SECONDS)
        initialDurationSeconds = minutes * 60 + seconds
    }

    private fun getTimerOptionsValue(key: TimerPreferenceFragment.Key): Int {
        try {
            var s: String = sharedPreferences.getString(key.toString(), "0")

            if (s.length == 0) {
                s = "0"
            }

            return Integer.parseInt(s)

        } catch (ex: NumberFormatException) {
            Timber.d("Exception: %s", ex)
            return 0
        }

    }

    fun getBronsteinDelayMs(): Long {
        val value = if (delayType == DelayType.BRONSTEIN) {
            (mIncrementSeconds * 1000).toLong()
        } else {
            0
        }
        return value
    }

    fun getFischerDelayMs(): Long {
        val value = if (delayType == DelayType.FISCHER) {
            (mIncrementSeconds * 1000).toLong()
        } else {
            0
        }
        return value
    }
}
