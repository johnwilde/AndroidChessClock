package johnwilde.androidchessclock.prefs

import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.preference.PreferenceScreen
import android.widget.BaseAdapter

import johnwilde.androidchessclock.R

/**  Activity that inflates the preferences from XML.
 *
 * It also updates the view with the current preference value
 * and reports back which prefs were changed.
 */
class TimerPreferenceFragment : PreferenceFragment() {

    private val timeControlType: TimeControl
        get() {
            var timeControlType = TimeControl.FIDE
            val type = PreferenceManager.getDefaultSharedPreferences(activity)
                    .getString(Key.TIMECONTROL_TYPE.toString(), TimeControl.FIDE.toString())
            try {
                timeControlType = TimeControl.valueOf(type)
            } catch (e: Exception) {
                PreferenceManager.getDefaultSharedPreferences(activity)
                        .edit()
                        .putString(Key.TIMECONTROL_TYPE.toString(), TimeControl.FIDE.toString())
                        .apply()
            }

            return timeControlType
        }

    // This enum stores the preference keys
    // that are defined in preferences.xml
    enum class Key private constructor(private val mValue: String) {
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
        SIMPLE_TIME_CONTROL_CHECKBOX("simple_time_control_checkbox"),
        TOURNAMENT_TIME_CONTROL_CHECKBOX("tournament_time_control_checkbox"),
        ADVANCED_SCREEN("advanced_time_control_preference_screen"),
        TIME_GAP("show_time_gap"),
        HOURGLASS_SCREEN("hourglass_time_control_preference_screen"),
        HOURGLASS_MINUTES("hourglass_initial_minutes_preference"),
        HOURGLASS_SECONDS("hourglass_initial_seconds_preference"),
        HOURGLASS_TIME_CONTROL_CHECKBOX("hourglass_time_control_checkbox");

        override fun toString(): String {
            return mValue
        }

        companion object {
            var checkBoxes = setOf(SIMPLE_TIME_CONTROL_CHECKBOX,
                    TOURNAMENT_TIME_CONTROL_CHECKBOX,
                    HOURGLASS_TIME_CONTROL_CHECKBOX)
            var subCheckBoxes = setOf(BASIC_SCREEN,
                    ADVANCED_SCREEN,
                    HOURGLASS_SCREEN)

            fun fromString(s: String): Key {

                for (k in Key.values()) {
                    if (k.toString().equals(s, ignoreCase = true))
                        return k
                }

                throw IllegalArgumentException()

            }
        }
    }

    enum class TimeControl {
        FIDE, CUSTOM
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences)
        val rootPref = findPreference(Key.ADVANCED_SCREEN.toString()) as PreferenceScreen
        findPreference(Key.TIMECONTROL_TYPE.toString()).onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue as String
            rootPref.summary = newValue
            (preferenceScreen.rootAdapter as BaseAdapter).notifyDataSetChanged()
            true
        }
    }

    private fun setAdvancedTimeControlPreferences() {

        val fideKeys = arrayOf(Key.FIDE_MIN_PHASE1, Key.FIDE_MIN_PHASE2, Key.FIDE_MOVES_PHASE1, Key.ADV_DELAY_TYPE, Key.ADV_INCREMENT_SECONDS)

        when (timeControlType) {
            TimerPreferenceFragment.TimeControl.FIDE ->
                setPreferenceEnabledUsingKey(fideKeys, false)
            TimerPreferenceFragment.TimeControl.CUSTOM ->
                // allow user to set the values
                setPreferenceEnabledUsingKey(fideKeys, true)
        }

        setFideMovesPhase1SummaryText()
        setFirstScreenSummaryText()
    }

    private fun setFideMovesPhase1SummaryText() {
        val keyMin = Key.FIDE_MIN_PHASE1.toString()
        val keyMoves = Key.FIDE_MOVES_PHASE1.toString()

        val s1 = getString(R.string.fide_minutes1_title_part1) + " " +
                PreferenceManager.getDefaultSharedPreferences(activity).getString(keyMoves, "N") + " " +
                getString(R.string.fide_minutes1_title_part2)

        val e = findPreference(keyMin) as EditTextPreference
        e.title = s1
    }

    private fun setFirstScreenSummaryText() {
        val lp = findPreference(Key.TIMECONTROL_TYPE.toString()) as ListPreference
        val p = findPreference(Key.ADVANCED_SCREEN.toString())
        p.summary = lp.value
    }

    private fun setFidePreferences() {
        // These are the official FIDE tournament time control values
        val FIDE_PHASE1_MIN = 90
        val FIDE_PHASE2_MIN = 30
        val FIDE_PHASE1_MOVES = 40
        val FIDE_INCREMENT_SEC = 30
        val delayType = PreferencesUtil.DelayType.FISCHER.name

        setEditTextValue(Key.FIDE_MIN_PHASE1, "" + FIDE_PHASE1_MIN)
        setEditTextValue(Key.FIDE_MIN_PHASE2, "" + FIDE_PHASE2_MIN)
        setEditTextValue(Key.FIDE_MOVES_PHASE1, "" + FIDE_PHASE1_MOVES)
        setEditTextValue(Key.ADV_INCREMENT_SECONDS, "" + FIDE_INCREMENT_SEC)
        setListPreferenceValue(Key.ADV_DELAY_TYPE, delayType)
    }

    // Helper functions that checks value before making a change.
    // This prevents stack overflow on API 7 devices
    private fun setEditTextValue(key: Key, value: String) {
        val e = findPreference(key.toString()) as EditTextPreference
        if (e.text != value)
            e.text = value
    }

    private fun setListPreferenceValue(key: Key, value: String) {
        val lp = findPreference(key.toString()) as ListPreference
        if (lp.value != value)
            lp.value = value
    }

    private fun setCheckBoxValue(key: Key, checked: Boolean?) {
        val cb = findPreference(key.toString()) as CheckBoxPreference
        if (cb.isChecked != checked)
            cb.isChecked = checked!!
    }

    private fun setPreferenceEnabledUsingKey(keys: Array<Key>, value: Boolean): Boolean {
        var changedStatusOfAKey = false
        for (key in keys) {
            val p = findPreference(key.toString())
            val originalValue = p.isEnabled
            changedStatusOfAKey = changedStatusOfAKey || originalValue != value
            p.isEnabled = value

        }
        return changedStatusOfAKey
    }

    /*
	 * Make any necessary changes to preference values.
	 *
	 * Ensure a value was entered for all EditText fields.
	 * Set default FIDE preferences, if needed.
	 */
    fun doValidationAndInitialization() {
        for (k in Key.values()) {
            val p = findPreference(k.toString())
            if (p is EditTextPreference) {
                // handle case where user entered no text
                if (p.text.trim { it <= ' ' }.length == 0) {
                    p.text = "0"
                }
            }
        }

        if (timeControlType == TimeControl.FIDE) {
            setFidePreferences()
        }
    }

    private fun getCheckbox(key: Key): RadioCheckBoxPreference {
        return findPreference(key.toString()) as RadioCheckBoxPreference
    }

    private fun updateRadioCheckBox(key: Key) {
        if (!getCheckbox(key).isChecked) {
            return
        }
        val cbToDisable = mutableSetOf<Key>()
        val subToEnable = mutableSetOf<Key>()
        val subToDisable = mutableSetOf<Key>()

        when (key) {
            Key.SIMPLE_TIME_CONTROL_CHECKBOX -> {
                cbToDisable.addAll(Key.checkBoxes.minus(key))
                subToEnable.add(Key.BASIC_SCREEN)
                subToDisable.addAll(Key.subCheckBoxes.minus(Key.BASIC_SCREEN))
            }
            Key.TOURNAMENT_TIME_CONTROL_CHECKBOX -> {
                cbToDisable.addAll(Key.checkBoxes.minus(key))
                subToEnable.add(Key.ADVANCED_SCREEN)
                subToDisable.addAll(Key.subCheckBoxes.minus(Key.ADVANCED_SCREEN))
            }
            Key.HOURGLASS_TIME_CONTROL_CHECKBOX -> {
                cbToDisable.addAll(Key.checkBoxes.minus(key))
                subToEnable.add(Key.HOURGLASS_SCREEN)
                subToDisable.addAll(Key.subCheckBoxes.minus(Key.HOURGLASS_SCREEN))
            }
            else -> {}
        }
        for (k in cbToDisable) {
            getCheckbox(k).isChecked = false
        }
        for (k in subToDisable) {
            findPreference(k.toString()).isEnabled = false
        }
        for (k in subToEnable) {
            findPreference(k.toString()).isEnabled = true
        }
    }

    fun updateCheckbox(key: String?) {
        if (key == null) {
            for (k in Key.checkBoxes) {
                updateRadioCheckBox(k)
            }
        } else if (Key.checkBoxes.contains(Key.fromString(key))) {
           updateRadioCheckBox(Key.fromString(key))
        }
    }

    /*
     * Make any necessary changes to summary text
     * and enabled/disabled status of preference fields.
    */
    fun doSummaryTextUpdates() {
        // Populate the preference text descriptions with helpful text
        // that lets user know the current value of the various options.
        setTimeControlSummaryText()
        setAdvancedTimeControlPreferences()
    }

    private fun setTimeControlSummaryText() {
        // Setup the initial values
        for (k in Key.values()) {
            val p = findPreference(k.toString())
            if (p is EditTextPreference) {
                p.summary = p.text
            }
            if (k == Key.DELAY_TYPE || k == Key.ADV_DELAY_TYPE) {
                val listPref = p as ListPreference
                val s = getString(R.string.summary_delay_type_preference) + " " +
                        listPref.value
                listPref.summary = s
            }
            if (k == Key.TIMECONTROL_TYPE) {
                val listPref = p as ListPreference
                listPref.summary = listPref.value
            }
        }
    }
}
