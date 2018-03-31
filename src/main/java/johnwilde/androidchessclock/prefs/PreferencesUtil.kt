package johnwilde.androidchessclock.prefs

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.content.edit
import com.f2prateek.rx.preferences2.RxSharedPreferences
import johnwilde.androidchessclock.R
import johnwilde.androidchessclock.Utils
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesUtil @Inject constructor(
        val context: Context,
        rxSharedPreferences: RxSharedPreferences) {

    val sharedPreferences : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    // Clock subscribes to this so it can update view state when it changes
    val timeGap = rxSharedPreferences
            .getBoolean(Key.TIME_GAP.toString())
            .asObservable()

    // General settings
    var showTimeGap: Boolean
        get() = sharedPreferences.getBoolean(Key.TIME_GAP.toString(), false)
        set(value) = sharedPreferences.edit {
            putBoolean(Key.TIME_GAP.toString(), value)
        }

    var allowNegativeTime: Boolean
        get() = sharedPreferences.getBoolean(Key.NEGATIVE_TIME.toString(), false)
        set(value) = sharedPreferences.edit {
            putBoolean(Key.NEGATIVE_TIME.toString(), value)
        }

    var playBuzzerAtEnd: Boolean
        get() = sharedPreferences .getBoolean(Key.PLAY_BELL.toString(), true)
        set(value) = sharedPreferences.edit {
            putBoolean(Key.PLAY_BELL.toString(), value)
        }

    var playSoundOnButtonTap: Boolean
        get() = sharedPreferences.getBoolean(Key.PLAY_CLICK.toString(), true)
        set(value) = sharedPreferences.edit {
            putBoolean(Key.PLAY_CLICK.toString(), value)
        }

    // Basic time settings
    var initialHours : Int
        get() = getTimerOptionsValue(Key.HOURS, "0")
        set(value) = sharedPreferences.edit {
            putString(Key.HOURS.toString(), value.toString())
        }

    var initialMinutes : Int
        get() = getTimerOptionsValue(Key.MINUTES, "15")
        set(value) = sharedPreferences.edit {
            putString(Key.MINUTES.toString(), value.toString())
        }

    var initialSeconds : Int
        get() = getTimerOptionsValue(Key.SECONDS, "0")
        set(value) = sharedPreferences.edit {
            putString(Key.SECONDS.toString(), value.toString())
        }

    var basicSettingsUseBonus: Boolean
        get() = sharedPreferences.getBoolean(Key.BASIC_USE_BONUS.toString(), false)
        set(value) = sharedPreferences.edit {
            putBoolean(Key.BASIC_USE_BONUS.toString(), value)
        }

    var basicIncrementSeconds: Int
        get() = getTimerOptionsValue(Key.INCREMENT_SECONDS, "0")
        set(value) = sharedPreferences.edit {
            putString(Key.INCREMENT_SECONDS.toString(), value.toString())
        }

    var basicDelayType: DelayType
        get() {
            val delayTypeString = sharedPreferences.getString(Key.DELAY_TYPE.toString(),
                    "FISCHER")
            return DelayType.valueOf(delayTypeString!!.toUpperCase())
        }
        set(value) = sharedPreferences.edit {
            putString(Key.DELAY_TYPE.toString(), value.toString())
        }

    // Hourglass settings
    var hourglassMinutes : Int
        get() = getTimerOptionsValue(Key.HOURGLASS_MINUTES, "5")
        set(value) = sharedPreferences.edit {
            putString(Key.HOURGLASS_MINUTES.toString(), value.toString())
        }

    var hourglassSeconds : Int
        get() = getTimerOptionsValue(Key.HOURGLASS_SECONDS, "0")
        set(value) = sharedPreferences.edit {
            putString(Key.HOURGLASS_SECONDS.toString(), value.toString())
        }


    // Tournament settings
    var phase1Minutes : Int
        get() = getTimerOptionsValue(Key.FIDE_MIN_PHASE1, "90")
        set(value) = sharedPreferences.edit {
            putString(Key.FIDE_MIN_PHASE1.toString(), value.toString())
        }

    var phase2Minutes : Int
        get() = getTimerOptionsValue(Key.FIDE_MIN_PHASE2, "30")
        set(value) = sharedPreferences.edit {
            putString(Key.FIDE_MIN_PHASE2.toString(), value.toString())
        }

    var phase1NumberOfMoves : Int
        get() = getTimerOptionsValue(Key.FIDE_MOVES_PHASE1, "40")
        set(value) = sharedPreferences.edit {
            putString(Key.FIDE_MOVES_PHASE1.toString(), value.toString())
        }

    var tournamentUseBonus: Boolean
        get() = sharedPreferences.getBoolean(Key.ADV_USE_BONUS.toString(), true)
        set(value) = sharedPreferences.edit {
            putBoolean(Key.ADV_USE_BONUS.toString(), value)
        }

    var tournamentIncrementSeconds: Int
        get() = getTimerOptionsValue(Key.ADV_INCREMENT_SECONDS, "30")
        set(value) = sharedPreferences.edit {
            putString(Key.ADV_INCREMENT_SECONDS.toString(), value.toString())
        }

    var tournamentDelayType: DelayType
        get() {
            val delayTypeString = sharedPreferences.getString(Key.ADV_DELAY_TYPE.toString(),
                    "FISCHER")
            return DelayType.valueOf(delayTypeString!!.toUpperCase())
        }
        set(value) = sharedPreferences.edit {
            putString(Key.ADV_DELAY_TYPE.toString(), value.toString())
        }

    var timeControlType: TimeControlType
        get() {
            var timeControl = TimeControlType.BASIC
            val type = sharedPreferences
                    .getString(PreferencesUtil.Key.TIMECONTROL_TYPE_2.toString(),
                            TimeControlType.BASIC.toString())
            try {
                timeControl = TimeControlType.valueOf(type)
            } catch (e: Exception) {
                sharedPreferences.edit {
                    putString(PreferencesUtil.Key.TIMECONTROL_TYPE_2.toString(),
                            TimeControlType.BASIC.toString())
                }
            }
            return timeControl
        }
        set(value) = sharedPreferences.edit {
            putString(PreferencesUtil.Key.TIMECONTROL_TYPE_2.toString(), value.toString())
        }

    enum class TimeControlType {
        BASIC, TOURNAMENT, HOURGLASS
    }
    enum class DelayType {
        FISCHER, BRONSTEIN
    }

    private var incrementSeconds: Int = 0
        get() {
            return when(timeControlType) {
                TimeControlType.HOURGLASS -> 0
                TimeControlType.TOURNAMENT -> if (delayEnabled) tournamentIncrementSeconds else 0
                TimeControlType.BASIC -> if (delayEnabled) basicIncrementSeconds else 0
            }
        }

    var delayType: DelayType = DelayType.FISCHER
        get() {
            return when(timeControlType) {
                TimeControlType.HOURGLASS -> DelayType.FISCHER
                TimeControlType.TOURNAMENT -> tournamentDelayType
                TimeControlType.BASIC -> basicDelayType
            }
        }

    var delayEnabled: Boolean = true
            get() {
               return when(timeControlType) {
                   TimeControlType.HOURGLASS -> false
                   TimeControlType.TOURNAMENT ->
                       tournamentUseBonus && tournamentIncrementSeconds > 0
                   TimeControlType.BASIC ->
                       basicSettingsUseBonus && basicIncrementSeconds > 0
               }
            }
    var initialDurationSeconds: Int = 0
        get() {
            return when(timeControlType) {
                TimeControlType.HOURGLASS -> hourglassMinutes * 60 + hourglassSeconds
                TimeControlType.TOURNAMENT ->
                    phase1Minutes * 60
                TimeControlType.BASIC ->
                    initialHours * 3600 + initialMinutes * 60 + initialSeconds
            }
        }

    fun formattedBasicTime() : String {
        val ms = (initialHours * 3600 + initialMinutes * 60 + initialSeconds) * 1000
        val timeString = Utils.formatClockTime(ms.toLong())
        return if (basicSettingsUseBonus && basicIncrementSeconds > 0) {
            val bonusStringSec = basicIncrementSeconds.toString()
            val bonusTypeString = basicDelayType.toString()[0]
            val bonusString = "+ $bonusStringSec $bonusTypeString"
            context.getString(R.string.basic_text_delay, timeString, bonusString)
        } else {
            timeString
        }
    }

    fun formattedHourglass(): String {
        val ms = (hourglassMinutes * 60 + hourglassSeconds) * 1000
        return Utils.formatClockTime(ms.toLong())
    }

    fun formattedTournament(): String {
        val ms1 = phase1Minutes * 60 * 1000
        val moves = phase1NumberOfMoves
        val ms2 = phase2Minutes * 60 * 1000
        val s1 = Utils.formatClockTime(ms1.toLong())
        val s2 = Utils.formatClockTime(ms2.toLong())
        return if (tournamentUseBonus && tournamentIncrementSeconds > 0) {
            val bonusStringSec = tournamentIncrementSeconds.toString()
            val bonusTypeString = tournamentDelayType.toString()[0]
            val bonusString = "+ $bonusStringSec $bonusTypeString"
            context.getString(R.string.tournament_text_delay,
                    s1, moves, s2, bonusString)
        } else {
            context.getString(R.string.tournament_text_no_delay, s1, moves, s2)
        }
    }


    private fun getTimerOptionsValue(key: Key, default: String = "0"): Int {
        try {
            val s: String = sharedPreferences.getString(key.toString(), default)
            return Integer.parseInt(s)

        } catch (ex: NumberFormatException) {
            Timber.d("Exception: %s", ex)
            return 0
        }
    }

    fun getBronsteinDelayMs(): Long {
        val value = if (delayEnabled && delayType == DelayType.BRONSTEIN) {
            (incrementSeconds * 1000).toLong()
        } else {
            0
        }
        return value
    }

    fun getFischerDelayMs(): Long {
        val value = if (delayEnabled && delayType == DelayType.FISCHER) {
            (incrementSeconds * 1000).toLong()
        } else {
            0
        }
        return value
    }

    enum class Key constructor(private val mValue: String) {
        MINUTES("initial_minutes_preference"),
        SECONDS("initial_seconds_preference"),
        INCREMENT_SECONDS("increment_preference"),
        DELAY_TYPE("delay_type_preference"),
        NEGATIVE_TIME("allow_negative_time_preference"),
        PLAY_CLICK("audible_notification_preference_click"),
        PLAY_BELL("audible_notification_preference_bell"),
        TIMECONTROL_TYPE("timecontrol_type_preference"),
        FIDE_MOVES_PHASE1("fide_n_moves"),
        FIDE_MIN_PHASE1("fide_minutes1"),
        FIDE_MIN_PHASE2("fide_minutes2"),
        ADV_INCREMENT_SECONDS("advanced_increment_preference"),
        ADV_DELAY_TYPE("advanced_delay_type_preference"),
        BASIC_SCREEN("basic_time_control_preference_screen"),
        ADVANCED_SCREEN("advanced_time_control_preference_screen"),
        TIME_GAP("show_time_gap"),
        HOURGLASS_SCREEN("hourglass_time_control_preference_screen"),
        HOURGLASS_MINUTES("hourglass_initial_minutes_preference"),
        HOURGLASS_SECONDS("hourglass_initial_seconds_preference"),
        BASIC_USE_BONUS("basic_time_control_use_bonus_preference"),
        HOURS("initial_hours_preference"),
        ADV_USE_BONUS("advanced_time_control_use_bonus_preference"),
        TIMECONTROL_TYPE_2("timecontrol_type__2_preference")
        ;

        override fun toString(): String {
            return mValue
        }
    }
}


