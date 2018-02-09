package johnwilde.androidchessclock.prefs

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import johnwilde.androidchessclock.prefs.TimerPreferenceFragment.Key
import timber.log.Timber

class TimerPreferenceActivity : AppCompatActivity(),
        SharedPreferences.OnSharedPreferenceChangeListener {
    var fragment : TimerPreferenceFragment = TimerPreferenceFragment()

    companion object {
        @JvmField val LOAD_ALL = "johnwilde.androidchessclock.LoadAll"
        @JvmField val LOAD_UI = "johnwilde.androidchessclock.LoadUi"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Display the fragment as the main content.
        fragmentManager
                .beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit()
    }

    private fun unregister() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun register() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    override fun onResume() {
        super.onResume()
        updateAll(null)
    }

    override fun onPause() {
        super.onPause()
        unregister()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences,
                                           key: String) {
        Timber.d("%s changed", key)
        // These keys don't require a clock reset
        val uiKeys = listOf(
                Key.TIME_GAP,
                Key.PLAY_BELL,
                Key.PLAY_CLICK,
                Key.NEGATIVE_TIME,
                Key.ADV_NEGATIVE_TIME)

        if (!uiKeys.contains(Key.fromString(key))){
            setResult(RESULT_OK, intent.putExtra(LOAD_ALL, true));
        }
        updateAll(key);
    }

    private fun updateAll(key : String?) {
        unregister()
        fragment.updateCheckbox(key)
        fragment.doValidationAndInitialization()
        fragment.doSummaryTextUpdates()
        register()
    }
}
