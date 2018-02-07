package johnwilde.androidchessclock

import android.content.Context
import johnwilde.androidchessclock.logic.ClockManager
import johnwilde.androidchessclock.logic.SystemTime
import johnwilde.androidchessclock.prefs.PreferencesUtil

class DependencyInjection (context: Context){
    val preferenceUtil = PreferencesUtil(context)
    val clockManager = ClockManager(preferenceUtil, SystemTime())
}