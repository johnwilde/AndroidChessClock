package johnwilde.androidchessclock.main
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.transition.TransitionManager
import android.support.v4.widget.DrawerLayout
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.hannesdorfmann.mosby3.mvi.MviActivity
import com.jakewharton.rxbinding2.view.RxView
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import johnwilde.androidchessclock.BarChartActivity
import johnwilde.androidchessclock.R
import johnwilde.androidchessclock.clock.ClockFragment
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.logic.ClockManager
import johnwilde.androidchessclock.main.MainViewState.PlayPauseButton.State
import johnwilde.androidchessclock.prefs.PreferencesUtil
import johnwilde.androidchessclock.prefs.TimerPreferenceActivity
import johnwilde.androidchessclock.sound.SoundFragment
import kotlinx.android.synthetic.main.main_activity.*
import timber.log.Timber
import javax.inject.Inject

var REQUEST_CODE_PREFERENCES : Int = 1
var REQUEST_CODE_ADJUST_TIME : Int = 2
val RESET_DIALOG_SHOWING = "RESET_DIALOG_SHOWING"

interface HasSnackbar {
    var snackBar : Snackbar?
    fun showSnackbar(message : String) : Snackbar?
    fun hideSnackbar()
}

class MainActivity : MviActivity<MainView, MainViewPresenter>(), MainView,
        HasSupportFragmentInjector, HasSnackbar {

    @Inject lateinit var clockManager : ClockManager
    @Inject lateinit var preferenceUtil : PreferencesUtil
    @Inject lateinit var fragmentInjector: DispatchingAndroidInjector<android.support.v4.app.Fragment>

    var views : Array<View> = emptyArray()
    var dialog : AlertDialog? = null
    private lateinit var drawerListener : SimpleDrawerListener

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate")
        setContentView(R.layout.main_activity)

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

    override fun createPresenter(): MainViewPresenter {
        // This component controls the state of the play / pause button and renders the
        // spinner view (during Bronstein delay)
        return MainViewPresenter(clockManager)
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
        clockManager.pause()
        dialog = AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.reset)
                .setMessage(R.string.really_reset)
                .setPositiveButton(R.string.yes, {
                    _, _ ->
                    clockManager.reset()
                    Toast.makeText(applicationContext, R.string.new_game, Toast.LENGTH_SHORT).show()
                })
                .setNeutralButton(R.string.stats, { _, _ -> launchStatsActivity() })
                .setNegativeButton(R.string.no, null)
                .show()
    }

    override fun render(viewState: MainViewState) {
        renderButton(viewState.button)
        renderSpinner(viewState.spinner)
        renderPromptToMove(viewState.prompt)
    }

    private fun renderButton(button: MainViewState.PlayPauseButton) {
        // Show "PLAY" when checked, "PAUSE" when not checked
        play_pause_button.isChecked = button.buttonState == State.PLAY
        // When game is finished hide the button
        play_pause_button.visibility = if (button.visible) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

    private fun renderSpinner(spinnerViewState: MainViewState.Spinner) {
        spinner.msTotal =  preferenceUtil.getBronsteinDelayMs()
        spinner.msSoFar = spinnerViewState.msDelayToGo
        if (spinnerViewState.msDelayToGo > 0) {
            spinner.visibility = View.VISIBLE
            spinner.postInvalidate()
        } else {
            spinner.visibility = View.INVISIBLE
        }
    }

    private fun renderPromptToMove(viewState: MainViewState.Snackbar) {
        if (viewState.show) {
            val id = if (viewState.message == MainViewState.Snackbar.Message.WHITE_LOST) {
                R.string.white_lost
            } else {
                R.string.black_lost
            }
            val s = showSnackbar(resources.getString(id))
            s?.let {
                it.setAction(
                        R.string.stats,
                        { _ -> startActivity(BarChartActivity.createIntent(this)) }
                )
            }
        } else if (viewState.dismiss) {
            hideSnackbar()
        }
    }

    override var snackBar : Snackbar? = null
    var snackBarDismissed : PublishSubject<Any> = PublishSubject.create()
    override fun showSnackbar(message : String) : Snackbar? {
        return if (snackBar == null || snackBar?.isShownOrQueued == false) {
            val v = coordinatorLayout
            snackBar = Snackbar.make(v, message, Snackbar.LENGTH_INDEFINITE)

            snackBar?.addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    if (Snackbar.Callback.DISMISS_EVENT_SWIPE == event) {
                        snackBarDismissed.onNext(1)
                    }
                }
            })
            snackBar?.show()
            snackBar
        } else {
            null
        }
    }

    override fun hideSnackbar() {
        snackBar?.dismiss()
    }

    override fun snackBarDismissed(): Observable<Any> {
        return snackBarDismissed
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
                preferenceUtil.loadTimeControlPreferences()
                clockManager.reset()
            }
        }
    }

    class SimpleDrawerListener : DrawerLayout.SimpleDrawerListener() {
        var subject : PublishSubject<Any> = PublishSubject.create()
        override fun onDrawerOpened(drawerView: View) {
            subject.onNext(1)
        }
    }

    override fun supportFragmentInjector(): AndroidInjector<android.support.v4.app.Fragment> {
        return fragmentInjector
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationDrawer)) {
            drawerLayout.closeDrawer(navigationDrawer)
        } else {
            super.onBackPressed()
        }
    }

}