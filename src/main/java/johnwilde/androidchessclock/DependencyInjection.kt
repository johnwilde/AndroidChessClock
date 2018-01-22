package johnwilde.androidchessclock

import android.content.Context
import android.preference.PreferenceManager
import johnwilde.androidchessclock.logic.ClockManager
import johnwilde.androidchessclock.prefs.PreferencesUtil

class DependencyInjection (context: Context){
    val preferenceUtil = PreferencesUtil(PreferenceManager.getDefaultSharedPreferences(context))
    val clockManager = ClockManager(preferenceUtil)
}