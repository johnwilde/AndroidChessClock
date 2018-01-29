package johnwilde.androidchessclock.main
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.support.transition.TransitionManager
import android.support.v4.widget.DrawerLayout
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.hannesdorfmann.mosby3.mvi.MviActivity
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import johnwilde.androidchessclock.BarChartActivity
import johnwilde.androidchessclock.ChessApplication
import johnwilde.androidchessclock.DependencyInjection
import johnwilde.androidchessclock.R
import johnwilde.androidchessclock.clock.ClockFragment
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.prefs.TimerPreferenceActivity
import johnwilde.androidchessclock.sound.SoundFragment
import kotlinx.android.synthetic.main.main_activity.*
import timber.log.Timber

var REQUEST_CODE_PREFERENCES : Int = 1
var REQUEST_CODE_ADJUST_TIME : Int = 2
val RESET_DIALOG_SHOWING = "RESET_DIALOG_SHOWING"

class MainActivity : MviActivity<PlayPauseView, PlayPausePresenter>(), PlayPauseView {

    var views : Array<View> = emptyArray()
    lateinit var dependencyInjection : DependencyInjection
    var dialog : AlertDialog? = null
    lateinit var drawerListener : SimpleDrawerListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate")
        setContentView(R.layout.main_activity)

        // initialize the singleton "business logic"
        dependencyInjection = ChessApplication.getDependencyInjection(this)

        drawerListener = SimpleDrawerListener()
        drawerLayout.addDrawerListener(drawerListener)
        views = arrayOf(leftContainer, buttons, rightContainer)

        supportFragmentManager.findFragmentByTag("sound") ?:
            supportFragmentManager
                    .beginTransaction()
                    .add(SoundFragment(), "sound")
                    .commit()

        supportFragmentManager.findFragmentByTag("left") ?:
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.left, ClockFragment.newInstance(ClockView.Color.WHITE), "left")
                        .commit()

        supportFragmentManager.findFragmentByTag("right") ?:
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.right, ClockFragment.newInstance(ClockView.Color.BLACK), "right")
                        .commit()

        reset_button.setOnClickListener {
            showResetDialog()
        }
        swap_sides.setOnClickListener {
            swapSides()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState!!.getBoolean(RESET_DIALOG_SHOWING, false)) {
            showResetDialog()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState!!.putBoolean(RESET_DIALOG_SHOWING, dialog?.isShowing ?: false)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        // https://stackoverflow.com/questions/15244179/dismissing-dialog-on-activity-finish
        dialog?.dismiss()
        drawerLayout.removeDrawerListener(drawerListener)
    }

    override fun createPresenter(): PlayPausePresenter {
        // This component controls the state of the play / pause button and renders the
        // spinner view (during Bronstein delay)
        return PlayPausePresenter(dependencyInjection.clockManager)
    }

    override fun playPauseIntent(): Observable<Any> {
        return RxView.clicks(play_pause_button)
    }

    override fun drawerOpened(): Observable<Any> {
        return drawerListener.subject
    }

    private fun launchStatsActivity() {
        startActivity(BarChartActivity.createIntent(this))
    }

    private fun showResetDialog() {
        dependencyInjection.clockManager.pause()
        dialog = AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.reset)
                .setMessage(R.string.really_reset)
                .setPositiveButton(R.string.yes, {
                    _, _ ->
                    dependencyInjection.clockManager.reset()
                    Toast.makeText(applicationContext, R.string.new_game, Toast.LENGTH_SHORT).show()
                })
                .setNeutralButton(R.string.stats, { _, _ -> launchStatsActivity() })
                .setNegativeButton(R.string.no, null)
                .show()
    }

    override fun render(viewState: PlayPauseViewState) {
//        Timber.d("%s", viewState)
        when (viewState) {
            is PlayPauseState -> {
                // Show "PLAY" when checked, "PAUSE" when not checked
                play_pause_button.isChecked = viewState.showPlay
                if (viewState.showDialog) {
                    Toast.makeText(applicationContext, R.string.pause_dialog, Toast.LENGTH_SHORT).show()
                }
                // When game is finished hide the button
                play_pause_button.visibility = if (viewState.enabled) View.VISIBLE else View.INVISIBLE
            }
            is SpinnerViewState -> {
                renderSpinner(viewState)
            }
        }
    }

    private fun renderSpinner(spinnerViewState: SpinnerViewState) {
        spinner.msTotal = dependencyInjection.preferenceUtil.getBronsteinDelayMs()
        spinner.msSoFar = spinnerViewState.msDelayToGo
        if (spinnerViewState.msDelayToGo > 0) {
            spinner.visibility = View.VISIBLE
            spinner.postInvalidate()
        } else {
            spinner.visibility = View.INVISIBLE
        }
    }

    private fun swapSides() {
        TransitionManager.beginDelayedTransition(mainContainer)
        mainContainer.removeAllViews()
        views.reverse()
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            views.get(0).scaleY = -1f
            views.get(0).scaleX = -1f
            views.get(2).scaleY = 1f
            views.get(2).scaleX = 1f
        }
        for (view in views) {
            mainContainer.addView(view)
        }
    }

    fun onSettingsClick(item: MenuItem) {
        launchPreferencesActivity()
    }

    fun onAboutClick(item: MenuItem) {
        showAboutDialog()
    }

    private fun launchPreferencesActivity() {
        // launch an activity through this intent
        val launchPreferencesIntent = Intent().setClass(this,
                TimerPreferenceActivity::class.java)
        // Make it a subactivity so we know when it returns
        startActivityForResult(launchPreferencesIntent, REQUEST_CODE_PREFERENCES)
    }

    private fun showAboutDialog() {
        val builder = AlertDialog.Builder(this)
        val title = getString(R.string.app_name)
        val about = getString(R.string.about_dialog)
        val message = title + ", " + getString(R.string.version) + ": " + getPackageVersion() + "\n\n" + about
        builder.setMessage(message).setPositiveButton(getString(R.string.OK)) { dialog, _ -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }

    private fun getPackageVersion(): String {
        return try {
            val manager = packageManager.getPackageInfo(packageName, 0)
            manager.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            getString(R.string.unknown)
        }
    }

    // This method is called when the user preferences activity returns. That
    // activity set fields in the Intent data to indicate what preferences
    // have been changed. The method takes the action appropriate for what
    // has changed. In some cases the clocks are reset.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // check which activity has returned (for now we only have one, so
        // it isn't really necessary).
        if (requestCode == REQUEST_CODE_PREFERENCES) {
            if (data == null)
                return  // no preferences were changed

            // reset clocks using new settings
            if (data.getBooleanExtra(
                    TimerPreferenceActivity.LOAD_ALL, false)) {
                dependencyInjection.preferenceUtil.loadAllUserPreferences()
                dependencyInjection.clockManager.reset()
            } else {
                dependencyInjection.preferenceUtil.loadUiPreferences()
            }
        }
    }

    class SimpleDrawerListener : DrawerLayout.SimpleDrawerListener() {
        var subject : PublishSubject<Any> = PublishSubject.create()
        override fun onDrawerOpened(drawerView: View) {
            subject.onNext(1)
        }
    }
}