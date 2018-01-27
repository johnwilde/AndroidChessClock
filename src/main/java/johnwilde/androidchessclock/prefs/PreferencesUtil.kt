package johnwilde.androidchessclock.prefs

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import johnwilde.androidchessclock.R
import timber.log.Timber

class PreferencesUtil(context: Context) {
    val sharedPreferences : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    enum class TimeControlType {
        BASIC, TOURNAMENT
    }
    internal enum class DelayType {
        FISCHER, BRONSTEIN
    }
    var timeControlType: TimeControlType = TimeControlType.BASIC
    private var delayType: DelayType = DelayType.FISCHER

    var phase1NumberOfMoves: Int = 0
    var phase1Minutes: Int = 0
    var initialDurationSeconds: Int = 0
    private var  mIncrementSeconds: Int = 0
    var playBuzzerAtEnd: Boolean = false
    var playSoundOnButtonTap: Boolean = false
    var allowNegativeTime: Boolean = false

    init {
        // set default values (for first run)
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false)
        loadAllUserPreferences()
    }

    // Methods for loading USER PREFERENCES
    //
    // These methods are run after the user has changed
    // a preference and during onCreate(). The onActivityResult()
    // method is responsible for calling the method that matches
    // the preference that was changed.
    //
    // Note: the default values required by the SharedPreferences getXX
    // methods are not used. The SharedPreferences will have their default
    // values set (in onCreate() ) and those defaults are saved in
    // preferences.xml

    fun loadAllUserPreferences() {
        loadTimeControlPreferences()
        loadUiPreferences()
    }

    fun loadUiPreferences() {
        loadAudibleNotificationUserPreference()
    }


    // determine whether we're using BASIC or TOURNAMENT time control
    private fun loadTimeControlPreferences() {
        val simpleTimeControl = sharedPreferences.getBoolean(
                TimerPreferenceFragment.Key.SIMPLE_TIME_CONTROL_CHECKBOX.toString(),
                true)

        if (simpleTimeControl) {
            timeControlType = TimeControlType.BASIC
            loadBasicTimeControlUserPreference()
        } else {
            timeControlType = TimeControlType.TOURNAMENT
            loadAdvancedTimeControlUserPreference()
        }
    }

    private fun loadBasicTimeControlUserPreference() {
        loadInitialTimeUserPreferences()
        loadIncrementUserPreference(TimerPreferenceFragment.Key.INCREMENT_SECONDS)
        loadDelayTypeUserPreference(TimerPreferenceFragment.Key.DELAY_TYPE)
        loadNegativeTimeUserPreference(TimerPreferenceFragment.Key.NEGATIVE_TIME)
    }

    private fun loadAdvancedTimeControlUserPreference() {

        val minutes1 = getTimerOptionsValue(TimerPreferenceFragment.Key.FIDE_MIN_PHASE1)

        initialDurationSeconds = minutes1 * 60

        phase1NumberOfMoves = getTimerOptionsValue(TimerPreferenceFragment.Key.FIDE_MOVES_PHASE1)
        phase1Minutes = getTimerOptionsValue(TimerPreferenceFragment.Key.FIDE_MIN_PHASE2)

        loadDelayTypeUserPreference(TimerPreferenceFragment.Key.ADV_DELAY_TYPE)
        loadIncrementUserPreference(TimerPreferenceFragment.Key.ADV_INCREMENT_SECONDS)
        loadNegativeTimeUserPreference(TimerPreferenceFragment.Key.ADV_NEGATIVE_TIME)
    }

    private fun loadAudibleNotificationUserPreference() {
        playBuzzerAtEnd = sharedPreferences.getBoolean(
                TimerPreferenceFragment.Key.PLAY_BELL.toString(), false)
        playSoundOnButtonTap = sharedPreferences.getBoolean(
                TimerPreferenceFragment.Key.PLAY_CLICK.toString(), false)
    }

    private fun loadNegativeTimeUserPreference(key: TimerPreferenceFragment.Key) {
        allowNegativeTime = sharedPreferences.getBoolean(key.toString(), false)
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