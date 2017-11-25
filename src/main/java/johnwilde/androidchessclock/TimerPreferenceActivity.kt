package johnwilde.androidchessclock

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceManager

import johnwilde.androidchessclock.TimerPreferenceFragment.Key;

class TimerPreferenceActivity : PreferenceActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    lateinit var fragment : TimerPreferenceFragment

    companion object {
        @JvmField
        val LOAD_ALL = "johnwilde.androidchessclock.LoadAll"
        @JvmField
        val LOAD_UI = "johnwilde.androidchessclock.LoadUi"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Display the fragment as the main content.
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, TimerPreferenceFragment())
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
        register()
    }

    override fun onPause() {
        super.onPause()
        unregister()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences,
                                           key: String) {
        val uiKeys = listOf(
                Key.SHOW_MOVE_COUNTER,
                Key.SWAP_SIDES,
                Key.SCREEN_DIM,
                Key.PLAY_BELL,
                Key.PLAY_CLICK);

        if (uiKeys.contains(Key.fromString(key))){
            setResult(RESULT_OK, intent.putExtra(LOAD_UI, true));
        }
        else{
            setResult(RESULT_OK, intent.putExtra(LOAD_ALL, true));
        }
        updateAll(key);
    }

    private fun updateAll(key : String?) {
        unregister()
        fragment.updateCheckbox(key)
        fragment.doValidationAndInitialization();
        fragment.doSummaryTextUpdates();
        register()
    }
}
